import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges, ViewChild } from '@angular/core';
import * as L from 'leaflet';

export interface PickerLocation {
  latitude: number;
  longitude: number;
}

@Component({
  selector: 'app-location-picker',
  templateUrl: './location-picker.component.html',
  styleUrls: ['./location-picker.component.scss']
})
export class LocationPickerComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('mapElement', { static: true }) mapElement!: ElementRef<HTMLDivElement>;

  @Input() latitude: number | null = null;
  @Input() longitude: number | null = null;

  @Output() locationSelected = new EventEmitter<PickerLocation>();

  selectedLocation: PickerLocation | null = null;

  private map: L.Map | null = null;
  private marker: L.Marker | null = null;
  private readonly defaultCenter: L.LatLngTuple = [30.0444, 31.2357];
  private readonly defaultZoom = 12;
  private readonly selectedZoom = 16;

  ngAfterViewInit(): void {
    this.initializeMap();
    this.syncFromInputs();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.map) {
      return;
    }

    if (changes['latitude'] || changes['longitude']) {
      this.syncFromInputs();
    }
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.map = null;
    this.marker = null;
  }

  private initializeMap(): void {
    const markerIcon = L.icon({
      iconUrl: 'assets/leaflet/marker-icon.png',
      shadowUrl: 'assets/leaflet/marker-shadow.png',
      iconRetinaUrl: 'assets/leaflet/marker-icon-2x.png',
      iconSize: [25, 41],
      iconAnchor: [12, 41],
      popupAnchor: [1, -34],
      shadowSize: [41, 41]
    });

    this.map = L.map(this.mapElement.nativeElement, {
      center: this.defaultCenter,
      zoom: this.defaultZoom,
      zoomControl: true
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);

    this.map.on('click', (event: L.LeafletMouseEvent) => {
      this.setMarker(event.latlng.lat, event.latlng.lng, true);
    });

    this.marker = L.marker(this.defaultCenter, { draggable: true, icon: markerIcon });
    this.marker.on('dragend', () => {
      const latLng = this.marker?.getLatLng();
      if (!latLng) {
        return;
      }
      this.setMarker(latLng.lat, latLng.lng, true, false);
    });
  }

  private syncFromInputs(): void {
    if (this.latitude == null || this.longitude == null) {
      return;
    }

    this.setMarker(this.latitude, this.longitude, false);
  }

  private setMarker(lat: number, lng: number, emit: boolean, moveMap = true): void {
    if (!this.map || !this.marker) {
      return;
    }

    const normalized = this.normalizeCoordinates(lat, lng);

    const rounded = {
      latitude: this.round(normalized.latitude),
      longitude: this.round(normalized.longitude)
    };

    this.selectedLocation = rounded;
    this.marker.setLatLng([rounded.latitude, rounded.longitude]);

    if (!this.map.hasLayer(this.marker)) {
      this.marker.addTo(this.map);
    }

    if (moveMap) {
      this.map.setView([rounded.latitude, rounded.longitude], this.selectedZoom);
    }

    if (emit) {
      this.locationSelected.emit(rounded);
    }
  }

  private round(value: number): number {
    return Number(value.toFixed(6));
  }

  private normalizeCoordinates(lat: number, lng: number): PickerLocation {
    const normalizedLatitude = Math.max(-90, Math.min(90, lat));
    const normalizedLongitude = ((((lng + 180) % 360) + 360) % 360) - 180;

    return {
      latitude: normalizedLatitude,
      longitude: normalizedLongitude
    };
  }
}
