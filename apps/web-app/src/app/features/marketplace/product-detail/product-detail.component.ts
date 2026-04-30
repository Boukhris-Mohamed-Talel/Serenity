import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import { AuthService } from '../../../core/services/auth.service';
import { MarketplaceShellService } from '../../../core/services/marketplace-shell.service';
import { MarketplaceUxService } from '../../../core/services/marketplace-ux.service';
import { MarketplaceProduct } from '../../../shared/models/marketplace.model';

type ArticleExperienceType = 'PODCAST' | 'BOOK' | 'EXERCISE' | 'VIDEO';

@Component({
  selector: 'app-product-detail',
  templateUrl: './product-detail.component.html',
  styleUrls: ['./product-detail.component.scss']
})
export class ProductDetailComponent implements OnInit, OnDestroy {
  @ViewChild('previewAudio') previewAudio?: ElementRef<HTMLAudioElement>;
  @ViewChild('previewVideo') previewVideo?: ElementRef<HTMLVideoElement>;

  private static readonly AUDIO_PREVIEW_LIMIT_SECONDS = 90;
  private static readonly VIDEO_PREVIEW_LIMIT_SECONDS = 30;

  product: MarketplaceProduct | null = null;
  loading = false;
  quantity = 1;
  inWishlist = false;
  userId: number | null = null;
  /** Full digital access granted after staff confirms the order (server: PAID). */
  hasServerAccess = false;
  accessRequestSent = false;
  showPaywall = false;
  paywallReason = 'Preview finished. Request full access to continue.';
  unlocking = false;
  unlockError = '';
  mediaError = '';

  recentOthers: MarketplaceProduct[] = [];
  resumeDismissed = false;
  playbackRate = 1;
  sleepTimerMin: 0 | 5 | 10 | 15 | 30 = 0;
  /** Browser `window.setTimeout` returns a numeric handle (not `NodeJS.Timeout`). */
  private sleepTimerId: number | null = null;
  private lastProgressFlush = 0;

  private productId = 0;

  bookPreviewPages: string[] = [];
  currentBookPage = 0;
  exerciseSteps: string[] = [];
  unlockedExerciseSteps = 2;
  completedSteps: boolean[] = [];

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly marketplaceService: MarketplaceService,
    private readonly authService: AuthService,
    private readonly ux: MarketplaceUxService,
    private readonly shell: MarketplaceShellService
  ) {}

  ngOnInit(): void {
    this.userId = this.authService.getUserId();
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/marketplace']);
      return;
    }
    this.productId = id;

    this.loading = true;
    this.marketplaceService.getProductById(id).subscribe({
      next: product => {
        this.product = product;
        this.bootstrapPreviewContent(product);
        this.hydratePreviewFromStorage();
        this.clampQuantityToStock();
        this.loading = false;
        this.checkWishlistStatus(id);
        this.refreshAccess();
        this.ux.pushRecentlyViewed(id);
        this.loadRecentOthers();
        window.setTimeout(() => this.applyPlaybackRate(), 0);
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  private bootstrapPreviewContent(product: MarketplaceProduct): void {
    this.bookPreviewPages = this.buildBookPreviewPages(product);
    this.exerciseSteps = this.buildExerciseSteps(product);
    this.completedSteps = this.exerciseSteps.map(() => false);
    this.currentBookPage = 0;
  }

  ngOnDestroy(): void {
    this.clearSleepTimer();
  }

  get showResumeBanner(): boolean {
    if (!this.product || !this.usesPreviewFlow || this.isUnlocked || this.resumeDismissed) {
      return false;
    }
    const st = this.ux.getPreviewProgress(this.productId);
    if (!st) {
      return false;
    }
    return (
      (st.bookPage ?? 0) > 0 ||
      (st.audioTime ?? 0) > 2 ||
      (st.videoTime ?? 0) > 2
    );
  }

  dismissResume(): void {
    this.resumeDismissed = true;
  }

  continuePreview(): void {
    this.resumeDismissed = true;
    this.openPreview();
  }

  applyPlaybackRate(): void {
    const audio = this.previewAudio?.nativeElement;
    if (audio) {
      audio.playbackRate = this.playbackRate;
    }
  }

  onSleepTimerChange(): void {
    this.clearSleepTimer();
  }

  private clearSleepTimer(): void {
    if (this.sleepTimerId !== null) {
      window.clearTimeout(this.sleepTimerId);
      this.sleepTimerId = null;
    }
  }

  private scheduleSleepTimerOnPlay(): void {
    this.clearSleepTimer();
    if (!this.sleepTimerMin) {
      return;
    }
    const ms = this.sleepTimerMin * 60 * 1000;
    this.sleepTimerId = window.setTimeout(() => {
      this.pauseAudio();
      const a = this.previewAudio?.nativeElement;
      a?.pause();
    }, ms);
  }

  private flushPreviewProgress(): void {
    if (!this.productId || !this.usesPreviewFlow || this.isUnlocked) {
      return;
    }
    const now = Date.now();
    if (now - this.lastProgressFlush < 1200) {
      return;
    }
    this.lastProgressFlush = now;
    const patch: { bookPage?: number; audioTime?: number; videoTime?: number } = {};
    if (this.experienceType === 'BOOK') {
      patch.bookPage = this.currentBookPage;
    }
    const audio = this.previewAudio?.nativeElement;
    if (audio && this.experienceType === 'PODCAST') {
      patch.audioTime = audio.currentTime;
    }
    const video = this.previewVideo?.nativeElement;
    if (video && this.experienceType === 'VIDEO') {
      patch.videoTime = video.currentTime;
    }
    this.ux.setPreviewProgress(this.productId, patch);
  }

  private hydratePreviewFromStorage(): void {
    const st = this.ux.getPreviewProgress(this.productId);
    if (!st) {
      return;
    }
    if (this.experienceType === 'BOOK' && typeof st.bookPage === 'number') {
      const max = Math.max(0, this.bookPreviewPages.length - 1);
      this.currentBookPage = Math.min(Math.max(0, st.bookPage), max);
    }
    window.setTimeout(() => {
      if (this.isUnlocked) {
        return;
      }
      const audio = this.previewAudio?.nativeElement;
      if (audio && this.experienceType === 'PODCAST' && typeof st.audioTime === 'number') {
        audio.currentTime = Math.min(st.audioTime, ProductDetailComponent.AUDIO_PREVIEW_LIMIT_SECONDS - 0.25);
      }
      const video = this.previewVideo?.nativeElement;
      if (video && this.experienceType === 'VIDEO' && typeof st.videoTime === 'number') {
        video.currentTime = Math.min(st.videoTime, ProductDetailComponent.VIDEO_PREVIEW_LIMIT_SECONDS - 0.25);
      }
    }, 0);
  }

  private loadRecentOthers(): void {
    const ids = this.ux.getRecentlyViewedIds().filter(x => x !== this.productId).slice(0, 8);
    if (ids.length === 0) {
      this.recentOthers = [];
      return;
    }
    forkJoin(ids.map(pid => this.marketplaceService.getProductById(pid))).subscribe({
      next: rows => {
        const order = new Map(ids.map((pid, i) => [pid, i]));
        this.recentOthers = [...rows].sort((a, b) => (order.get(a.id) ?? 0) - (order.get(b.id) ?? 0));
      },
      error: () => {
        this.recentOthers = [];
      }
    });
  }

  openAccessExplainer(): void {
    this.shell.openAccessExplainer();
  }

  toggleCompare(): void {
    if (!this.product) {
      return;
    }
    this.ux.toggleCompare(this.product.id);
  }

  isInCompare(): boolean {
    return this.product ? this.ux.isInCompare(this.product.id) : false;
  }

  checkWishlistStatus(productId: number): void {
    if (!this.userId) return;
    
    this.marketplaceService.isProductInWishlist(productId).subscribe({
      next: (inWishlist) => {
        this.inWishlist = inWishlist;
      }
    });
  }

  get isUnlocked(): boolean {
    return this.hasServerAccess;
  }

  private refreshAccess(): void {
    if (!this.productId || !this.authService.isLoggedIn()) {
      this.hasServerAccess = false;
      return;
    }
    this.marketplaceService.hasPaidAccessForProduct(this.productId).subscribe({
      next: ok => {
        this.hasServerAccess = ok;
      },
      error: () => {
        this.hasServerAccess = false;
      }
    });
  }

  get isDigitalArticle(): boolean {
    return this.product?.type === 'DIGITAL';
  }

  get usesPreviewFlow(): boolean {
    return this.isDigitalArticle && Boolean(this.product?.previewable);
  }

  get experienceType(): ArticleExperienceType {
    if (this.product?.previewType === 'VIDEO') {
      return 'VIDEO';
    }
    if (this.product?.previewType === 'AUDIO') {
      return 'PODCAST';
    }
    if (this.product?.previewType === 'BOOK') {
      return 'BOOK';
    }

    const source = `${this.product?.name ?? ''} ${this.product?.description ?? ''}`.toLowerCase();
    if (source.includes('exercise') || source.includes('routine') || source.includes('breathing')) {
      return 'EXERCISE';
    }
    return 'BOOK';
  }

  get previewProgressPercent(): number {
    if (this.experienceType === 'VIDEO') {
      const video = this.previewVideo?.nativeElement;
      if (!video) {
        return 0;
      }
      return Math.min((video.currentTime / ProductDetailComponent.VIDEO_PREVIEW_LIMIT_SECONDS) * 100, 100);
    }

    const audio = this.previewAudio?.nativeElement;
    if (!audio) {
      return 0;
    }
    return Math.min((audio.currentTime / ProductDetailComponent.AUDIO_PREVIEW_LIMIT_SECONDS) * 100, 100);
  }

  get currentBookPreviewText(): string {
    if (this.bookPreviewPages.length === 0) {
      return 'Preview is being prepared. Please check back in a moment.';
    }
    return this.bookPreviewPages[this.currentBookPage] ?? this.bookPreviewPages[0];
  }

  get hasNextBookPage(): boolean {
    return this.currentBookPage < this.bookPreviewPages.length - 1;
  }

  get previewLimitSeconds(): number {
    return this.experienceType === 'VIDEO'
      ? ProductDetailComponent.VIDEO_PREVIEW_LIMIT_SECONDS
      : ProductDetailComponent.AUDIO_PREVIEW_LIMIT_SECONDS;
  }

  openPreview(): void {
    if (!this.product) {
      return;
    }
    if (!this.usesPreviewFlow || this.isUnlocked) {
      return;
    }
    this.mediaError = '';

    if (this.experienceType === 'PODCAST') {
      const audio = this.previewAudio?.nativeElement;
      if (audio && audio.currentTime >= ProductDetailComponent.AUDIO_PREVIEW_LIMIT_SECONDS) {
        audio.currentTime = 0;
      }
      audio?.play();
      return;
    }

    if (this.experienceType === 'VIDEO') {
      const video = this.previewVideo?.nativeElement;
      if (video && video.currentTime >= ProductDetailComponent.VIDEO_PREVIEW_LIMIT_SECONDS) {
        video.currentTime = 0;
      }
      video?.play();
    }
  }

  onAudioPlay(): void {
    if (!this.usesPreviewFlow) {
      return;
    }
    this.applyPlaybackRate();
    this.scheduleSleepTimerOnPlay();
  }

  onAudioTimeUpdate(): void {
    if (!this.usesPreviewFlow || this.isUnlocked) {
      return;
    }

    const audio = this.previewAudio?.nativeElement;
    if (!audio) {
      return;
    }

    if (audio.currentTime >= ProductDetailComponent.AUDIO_PREVIEW_LIMIT_SECONDS) {
      audio.currentTime = ProductDetailComponent.AUDIO_PREVIEW_LIMIT_SECONDS;
      this.pauseAudio();
      this.openPaywall('Your preview ended. Request full access for the complete session.');
    }
    this.flushPreviewProgress();
  }

  onAudioSeeking(): void {
    if (!this.usesPreviewFlow || this.isUnlocked) {
      return;
    }
    const audio = this.previewAudio?.nativeElement;
    if (!audio) {
      return;
    }
    if (audio.currentTime > ProductDetailComponent.AUDIO_PREVIEW_LIMIT_SECONDS) {
      audio.currentTime = ProductDetailComponent.AUDIO_PREVIEW_LIMIT_SECONDS;
      this.pauseAudio();
      this.openPaywall('Preview limit reached. Request full access to keep listening.');
    }
  }

  onAudioEnded(): void {
    if (!this.usesPreviewFlow || this.isUnlocked) {
      return;
    }
    this.openPaywall('Preview completed. Request full access to continue listening.');
  }

  onAudioError(): void {
    this.mediaError = 'Unable to load this audio preview right now. Please try again in a moment.';
  }

  onVideoTimeUpdate(): void {
    if (!this.usesPreviewFlow || this.isUnlocked) {
      return;
    }

    const video = this.previewVideo?.nativeElement;
    if (!video) {
      return;
    }

    if (video.currentTime >= ProductDetailComponent.VIDEO_PREVIEW_LIMIT_SECONDS) {
      video.currentTime = ProductDetailComponent.VIDEO_PREVIEW_LIMIT_SECONDS;
      video.pause();
      this.openPaywall('Your video preview ended. Request full access to keep watching.');
    }
    this.flushPreviewProgress();
  }

  onVideoSeeking(): void {
    if (!this.usesPreviewFlow || this.isUnlocked) {
      return;
    }

    const video = this.previewVideo?.nativeElement;
    if (!video) {
      return;
    }

    if (video.currentTime > ProductDetailComponent.VIDEO_PREVIEW_LIMIT_SECONDS) {
      video.currentTime = ProductDetailComponent.VIDEO_PREVIEW_LIMIT_SECONDS;
      video.pause();
      this.openPaywall('Preview limit reached. Request full access to keep watching.');
    }
  }

  onVideoEnded(): void {
    if (!this.usesPreviewFlow || this.isUnlocked) {
      return;
    }
    this.openPaywall('Video preview completed. Request full access to keep watching.');
  }

  onVideoError(): void {
    this.mediaError = 'Unable to load this video preview right now. Please try again in a moment.';
  }

  nextBookPage(): void {
    if (!this.usesPreviewFlow) {
      return;
    }

    if (this.isUnlocked) {
      if (this.hasNextBookPage) {
        this.currentBookPage += 1;
      }
      return;
    }

    if (this.hasNextBookPage) {
      this.currentBookPage += 1;
      this.flushPreviewProgress();
      return;
    }

    this.openPaywall('Preview pages completed. Request full access to keep reading.');
  }

  previousBookPage(): void {
    if (this.currentBookPage > 0) {
      this.currentBookPage -= 1;
      this.flushPreviewProgress();
    }
  }

  toggleExerciseStep(index: number): void {
    if (!this.usesPreviewFlow) {
      return;
    }

    if (this.isUnlocked || index < this.unlockedExerciseSteps) {
      this.completedSteps[index] = !this.completedSteps[index];
      return;
    }

    this.openPaywall('You completed the free exercise preview. Request full access to continue.');
  }

  requestUnlock(): void {
    if (!this.product) {
      return;
    }
    if (!this.usesPreviewFlow) {
      return;
    }
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/auth/login']);
      return;
    }

    this.unlocking = true;
    this.unlockError = '';
    this.marketplaceService.requestDigitalAccess(this.product).subscribe({
      next: () => {
        this.unlocking = false;
        this.showPaywall = false;
        this.accessRequestSent = true;
        this.refreshAccess();
      },
      error: () => {
        this.unlocking = false;
        this.unlockError = 'Unable to submit your request right now. Please try again in a moment.';
      }
    });
  }

  closePaywall(): void {
    this.showPaywall = false;
    this.unlockError = '';
    this.resetPreviewState();
  }

  private openPaywall(reason: string): void {
    this.paywallReason = reason;
    this.showPaywall = true;
  }

  restartPreview(): void {
    this.resetPreviewState();
    this.openPreview();
  }

  private resetPreviewState(): void {
    this.pauseAudio();
    const audio = this.previewAudio?.nativeElement;
    if (audio) {
      audio.currentTime = 0;
    }

    const video = this.previewVideo?.nativeElement;
    if (video) {
      video.pause();
      video.currentTime = 0;
    }

    this.currentBookPage = 0;
    this.completedSteps = this.exerciseSteps.map(() => false);
    this.mediaError = '';
  }

  private pauseAudio(): void {
    const audio = this.previewAudio?.nativeElement;
    if (audio) {
      audio.pause();
    }
    this.clearSleepTimer();
  }

  onAudioPause(): void {
    this.clearSleepTimer();
  }

  get physicalInStock(): boolean {
    return !!this.product && this.product.type === 'PHYSICAL' && (this.product.stockQuantity ?? 0) > 0;
  }

  get maxBuyQuantity(): number {
    if (!this.product || this.product.type !== 'PHYSICAL') {
      return 999;
    }
    return Math.max(1, this.product.stockQuantity ?? 0);
  }

  clampQuantityToStock(): void {
    if (!this.product || this.product.type !== 'PHYSICAL') {
      if (this.quantity < 1) {
        this.quantity = 1;
      }
      return;
    }
    const max = this.maxBuyQuantity;
    if (this.quantity > max) {
      this.quantity = max;
    }
    if (this.quantity < 1) {
      this.quantity = Math.min(1, max);
    }
  }

  addToCart(): void {
    if (!this.product || !this.marketplaceService.isCartEligible(this.product)) {
      return;
    }
    this.clampQuantityToStock();
    if (!this.physicalInStock) {
      return;
    }
    if (!this.marketplaceService.addToCart(this.product, this.quantity)) {
      return;
    }
    this.shell.openMiniCart();
  }

  getPodcastPreviewUrl(productId: number): string {
    if (this.product?.previewUrl) {
      return this.product.previewUrl;
    }

    const options = [
      'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',
      'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3',
      'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3'
    ];
    return options[productId % options.length];
  }

  getVideoPreviewUrl(productId: number): string {
    if (this.product?.previewUrl) {
      return this.product.previewUrl;
    }

    const options = [
      'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
      'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'
    ];
    return options[productId % options.length];
  }

  private buildBookPreviewPages(product: MarketplaceProduct): string[] {
    const base = product.description?.trim() || 'This article shares practical techniques to support emotional balance.';
    return [
      `${base} This opening preview helps you understand the tone and approach before requesting full access.`,
      'In the full version, you will receive guided structure, reflective prompts, and calm step-by-step progression.'
    ];
  }

  private buildExerciseSteps(product: MarketplaceProduct): string[] {
    return [
      'Pause for 30 seconds and notice your breathing without judgment.',
      `Write one short intention inspired by \"${product.name}\".`,
      'Complete a full 5-minute guided routine (locked in preview mode).',
      'Record how your mood shifted after the full routine (locked in preview mode).'
    ];
  }

  toggleWishlist(): void {
    if (!this.product || !this.userId) {
      this.router.navigate(['/auth/login']);
      return;
    }

    if (this.inWishlist) {
      this.marketplaceService.removeFromWishlist(this.product.id).subscribe({
        next: () => {
          this.inWishlist = false;
        }
      });
    } else {
      this.marketplaceService.addToWishlist(this.product.id).subscribe({
        next: () => {
          this.inWishlist = true;
        }
      });
    }
  }
}
