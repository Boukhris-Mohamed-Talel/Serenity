const express = require('express');
const cors = require('cors');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

const claims = [];

app.post('/api/claims', (req, res) => {
  const { ref, patientName, description, amount, reimbursementAmount, insuranceCompany, insuranceGrade } = req.body;
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
    processedAt: null
  };

  claims.push(claim);
  console.log(`[RECEIVED] Claim ${ref} from ${claim.patientName} — ${amount} TND`);
  res.status(201).json(claim);
});

app.get('/api/claims', (_req, res) => {
  res.json(claims);
});

app.get('/api/claims/:ref/status', (req, res) => {
  const claim = claims.find(c => c.ref === req.params.ref);
  if (!claim) return res.status(404).json({ error: 'Claim not found' });

  res.json({
    ref: claim.ref,
    status: claim.status,
    reimbursementAmount: claim.reimbursementAmount
  });
});

app.patch('/api/claims/:ref/approve', (req, res) => {
  const claim = claims.find(c => c.ref === req.params.ref);
  if (!claim) return res.status(404).json({ error: 'Claim not found' });

  const montant = Number(req.body.reimbursementAmount ?? claim.reimbursementAmount ?? claim.amount);
  claim.status = 'APPROVED';
  claim.reimbursementAmount = montant;
  claim.processedAt = new Date().toISOString();

  console.log(`[APPROVED] Claim ${claim.ref} — reimbursing ${montant} TND`);
  res.json(claim);
});

app.patch('/api/claims/:ref/reject', (req, res) => {
  const claim = claims.find(c => c.ref === req.params.ref);
  if (!claim) return res.status(404).json({ error: 'Claim not found' });

  claim.status = 'REJECTED';
  claim.reimbursementAmount = null;
  claim.processedAt = new Date().toISOString();

  console.log(`[REJECTED] Claim ${claim.ref}`);
  res.json(claim);
});

app.get('/', (_req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(PORT, () => {
  console.log(`Insurance Portal running on http://localhost:${PORT}`);
});
