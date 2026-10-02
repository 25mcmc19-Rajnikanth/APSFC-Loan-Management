const fs = require('fs');
const path = require('path');
const dir = 'F:/APSFC Loan Management/frontend/assets/js';
// Usage: node tests/_render_test.js   (backend must be running on :8080)

const src = `
// ---- browser stubs ----
const storage = {};
const localStorage = {
  getItem: k => (k in storage ? storage[k] : null),
  setItem: (k, v) => { storage[k] = String(v); },
  removeItem: k => { delete storage[k]; }
};
const window = { location: { hostname: 'localhost', hash: '' } };
const location = { hash: '', href: '' };
const document = { querySelector: () => null, getElementById: () => ({ addEventListener: () => {}, value: '', appendChild: () => {}, innerHTML: '' }),
  createElement: () => ({ className: '', textContent: '', classList: ({ add: () => {} }), appendChild: () => {}, remove: () => {} }), body: { appendChild: () => {} } };
function toast() {}
function modal() {}
function closeModal() {}
const router = { go: () => {} };

${fs.readFileSync(path.join(dir, 'api.js'), 'utf8')}
${fs.readFileSync(path.join(dir, 'common.js'), 'utf8')}
${fs.readFileSync(path.join(dir, 'views.js'), 'utf8')}

async function login(identifier, password) {
  const r = await ace.post('/auth/login', { identifier, password });
  return r.token;
}

(async () => {
  const custT = await login('amit23', 'Passw0rd!');
  const offT = await login('officer', 'Officer@123');
  const mgrT = await login('manager', 'Manager@123');
  const admT = await login('admin@apsfc.local', 'Admin@123');

  ace.token = custT;
  const apps = (await ace.get('/loan-applications/mine?size=100')).content;
  const lid = apps.find(a => a.status === 'APPROVED' || a.status === 'DISBURSED') || apps[0];
  const draftApp = apps.find(a => a.status === 'DRAFT');
  const loans = (await ace.get('/loans/mine?size=100')).content;
  const loan = loans[0];
  ace.token = admT;
  const empL = (await ace.get('/employees?size=100')).content;

  const runs = [
    ['home', custT, 'CUSTOMER', () => Views['home']()],
    ['apply', custT, 'CUSTOMER', () => Views['apply']()],
    ['my-apps', custT, 'CUSTOMER', () => Views['my-apps']()],
    ['app-detail', custT, 'CUSTOMER', () => Views['app-detail'](null, null, lid.id)],
    ...(draftApp ? [['app-detail-edit', custT, 'CUSTOMER', () => Views['app-detail-edit'](null, null, draftApp.id)]] : []),
    ['my-loans', custT, 'CUSTOMER', () => Views['my-loans']()],
    ['loan-detail', custT, 'CUSTOMER', () => Views['loan-detail'](null, null, loan.id)],
    ['payments', custT, 'CUSTOMER', () => Views['payments']()],
    ['profile', custT, 'CUSTOMER', () => Views['profile']()],
    ['staff-apps', offT, 'OFFICER', () => Views['staff-apps']()],
    ['staff-loans', offT, 'OFFICER', () => Views['staff-loans']()],
    ['customers', offT, 'OFFICER', () => Views['customers']()],
    ['customer', mgrT, 'MANAGER', () => Views['customer'](null, null, 1)],
    ['reports', mgrT, 'MANAGER', () => Views['reports']()],
    ['employees', admT, 'ADMIN', () => Views['employees']()],
    ['employee-edit', admT, 'ADMIN', () => Views['employee-edit'](null, null, empL[0].id)]
  ];

  let failures = 0;
  for (const [name, tok, role, fn] of runs) {
    ace.token = tok;
    ace.user = null;
    storage['apsfc_user'] = null;
    try {
      const html = await fn();
      const noScript = html.replace(/<script[\\s\\S]*?<\\/script>/g, '');
      const bad = [];
      if (!/<h1>/.test(html)) bad.push('no <h1>');
      const mUn = noScript.match(/undefined|NaN|\\[object Object\\]|__ace/);
      if (mUn) bad.push('unrendered "' + mUn[0] + '" at ' + mUn.index + ': …' + html.slice(Math.max(0, mUn.index - 50), mUn.index + 30).replace(/\\n/g, ' ') + '…');
      const strays = html.match(/\\$\\{/g);
      if (strays && strays.length > 0) bad.push('stray $'+'{ ');
      if (bad.length) { failures++; console.log('FAIL ' + role + ' ' + name + ': ' + bad.join(' | ')); }
      else console.log('OK   ' + role + ' ' + name + ' (' + html.length + ' chars)');
    } catch (e) {
      failures++;
      console.log('FAIL ' + role + ' ' + name + ' threw [' + (e.status || e.code || '') + '] ' + e.message);
    }
  }
  console.log(failures === 0 ? 'ALL VIEWS RENDER CLEAN' : failures + ' FAILURES');
})().catch(e => { console.error('HARNESS ERROR: ' + e.message); process.exit(1); });
`;

(0, eval)(src);