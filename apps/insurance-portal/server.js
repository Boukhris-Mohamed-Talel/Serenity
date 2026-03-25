const express = require('express');
const cors = require('cors');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 3000;
const DATA_DIR = path.join(__dirname, 'data');
const CLAIMS_FILE = path.join(DATA_DIR, 'claims.json');

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

function loadClaimsFromDisk() {
  try {
    if (!fs.existsSync(DATA_DIR)) {
      fs.mkdirSync(DATA_DIR, { recursive: true });
    }
    if (!fs.existsSync(CLAIMS_FILE)) {
      fs.writeFileSync(CLAIMS_FILE, '[]', 'utf8');
    }
    const raw = fs.readFileSync(CLAIMS_FILE, 'utf8');
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch (err) {
    console.error('Failed to load persisted claims:', err.message);
    return [];
  }
}

function saveClaimsToDisk() {
  try {
    fs.writeFileSync(CLAIMS_FILE, JSON.stringify(claims, null, 2), 'utf8');
  } catch (err) {
    console.error('Failed to persist claims:', err.message);
  }
}

const claims = loadClaimsFromDisk();

app.post('/api/claims', (req, res) => {
  const { ref, patientName, description, amount, reimbursementAmount, insuranceCompany, insuranceGrade, attachmentUrls } = req.body;
  if (!ref || !description || !amount) {
    return res.status(400).json({ error: 'ref, description, and amount are required' });
  }

  const numericAmount = Number(amount);
  const numericReimbursement = reimbursementAmount != null ? Number(reimbursementAmount) : numericAmount;

  const claim = {
    ref,
    patientName: patientName || 'Unknown',
    description,
    amount: numericAmount,
    insuranceCompany: insuranceCompany || 'N/A',
    insuranceGrade: insuranceGrade || 0,
    status: 'PENDING',
    // Default to proposed reimbursement from healthcare app (grade-based)
    reimbursementAmount: Number.isFinite(numericReimbursement) ? numericReimbursement : numericAmount,
    receivedAt: new Date().toISOString(),
    processedAt: null,
    rejectReason: null,
    adjustmentReason: null,
    attachmentUrls: Array.isArray(attachmentUrls) ? attachmentUrls.filter(url => typeof url === 'string' && url.trim() !== '') : []
  };

  claims.push(claim);
  saveClaimsToDisk();
  console.log(`[RECEIVED] Claim ${ref} from ${claim.patientName} — ${amount} TND`);
  res.status(201).json(claim);
});

app.get('/api/claims', (_req, res) => {
  res.json(claims);
});

app.get('/api/claims/:ref/status', (req, res) => {
  const claim = claims.find(c => c.ref === req.params.ref);
  if (!claim) return res.status(404).json({ error: 'Claim not found' });

  const reason = claim.status === 'REJECTED'
    ? claim.rejectReason
    : (claim.adjustmentReason || null);

  res.json({
    ref: claim.ref,
    status: claim.status,
    reimbursementAmount: claim.reimbursementAmount,
    reason
  });
});

app.get('/api/claims/:ref', (req, res) => {
  const claim = claims.find(c => c.ref === req.params.ref);
  if (!claim) return res.status(404).json({ error: 'Claim not found' });
  res.json(claim);
});

app.patch('/api/claims/:ref/approve', (req, res) => {
  const claim = claims.find(c => c.ref === req.params.ref);
  if (!claim) return res.status(404).json({ error: 'Claim not found' });

  const montant = Number(req.body.reimbursementAmount ?? claim.reimbursementAmount ?? claim.amount);
  if (!Number.isFinite(montant) || montant <= 0) {
    return res.status(400).json({ error: 'Valid reimbursement amount is required' });
  }
  const adjustmentReason = typeof req.body.adjustmentReason === 'string'
    ? req.body.adjustmentReason.trim()
    : '';

  claim.status = 'APPROVED';
  claim.reimbursementAmount = montant;
  claim.processedAt = new Date().toISOString();
  claim.rejectReason = null;
  claim.adjustmentReason = adjustmentReason || null;
  saveClaimsToDisk();

  console.log(`[APPROVED] Claim ${claim.ref} — reimbursing ${montant} TND`);
  res.json(claim);
});

app.patch('/api/claims/:ref/reject', (req, res) => {
  const claim = claims.find(c => c.ref === req.params.ref);
  if (!claim) return res.status(404).json({ error: 'Claim not found' });
  const rejectReason = typeof req.body.rejectReason === 'string' ? req.body.rejectReason.trim() : '';
  if (!rejectReason) {
    return res.status(400).json({ error: 'Rejection reason is required' });
  }

  claim.status = 'REJECTED';
  claim.reimbursementAmount = null;
  claim.processedAt = new Date().toISOString();
  claim.rejectReason = rejectReason;
  claim.adjustmentReason = null;
  saveClaimsToDisk();

  console.log(`[REJECTED] Claim ${claim.ref} — reason: ${rejectReason}`);
  res.json(claim);
});

app.get('/', (_req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(PORT, () => {
  console.log(`Loaded ${claims.length} persisted claim(s) from disk`);
  console.log(`Insurance Portal running on http://localhost:${PORT}`);
});
