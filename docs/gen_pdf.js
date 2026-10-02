// SRS -> PDF generator (Chrome headless)
//
// Node script: reads docs/SRS.md, converts the contained markdown subset
// (headings, tables, lists, code fences, inline bold/code) into a print-ready
// HTML document, renders the Mermaid/ASCII blocks as HTML/CSS diagrams, then
// prints the HTML to PDF using the installed Chrome.
//
// Usage: node docs/gen_pdf.js

const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const ROOT = path.resolve(__dirname, '..');
const MD = fs.readFileSync(path.join(ROOT, 'docs', 'SRS.md'), 'utf8').replace(/\r\n/g, '\n');

// ---------------------------------------------------------------- inline md
const escHtml = s => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

function inline(s) {
    return s
        .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
        .replace(/`([^`]+)`/g, '<code>$1</code>');
}

// ------------------------------------------------------------- diagrams
const UC_TITLE = {
    'UC1': 'Register / Create account',
    'UC2': 'View loan products',
    'UC3': 'Apply for a loan',
    'UC4': 'Manage own applications<br/>(edit / submit / cancel)',
    'UC5': 'Record repayment',
    'UC6': 'Review application (start review / recommend)',
    'UC7': 'Approve / reject application',
    'UC8': 'Disburse loan &amp; generate EMI schedule',
    'UC9': 'Manage customers (status &amp; KYC)',
    'UC10': 'Manage employees (provision / update)',
    'UC11': 'View dashboard &amp; reports',
    'UC12': 'Manage loan products (create / update)'
};
const UC_ROW = {
    'CUSTOMER': ['UC1', 'UC2', 'UC3', 'UC4', 'UC5'],
    'LOAN_OFFICER': ['UC2', 'UC6', 'UC9', 'UC11'],
    'MANAGER': ['UC2', 'UC7', 'UC8', 'UC9', 'UC11'],
    'ADMIN': ['UC2', 'UC9', 'UC10', 'UC11', 'UC12']
};

function ucDiagram() {
    const actors = Object.keys(UC_ROW);
    const rows = actors.map(a => `
        <div class="uc-row">
            <div class="uc-actor">${a}</div>
            <div class="uc-pills">${UC_ROW[a].map(u => `<span class="uc-pill">${u} · ${UC_TITLE[u]}</span>`).join('')}</div>
        </div>`).join('');
    const side = actors.map(a => `<div class="uc-actornode">&#9670; ${a}</div>`).join('');
    return `
    <div class="uc-wrap">
        <div class="uc-side"><b>Actors</b>${side}</div>
        <div class="uc-sys">
            <div class="uc-systitle">Loan Management System</div>
            ${rows}
        </div>
    </div>`;
}

function archDiagram() {
    const tier = (name, desc, chips) => `
        <div class="arch-tier">
            <div class="arch-tiername">${name}</div>
            <div class="arch-tierdesc">${desc}</div>
            <div class="arch-chips">${chips}</div>
        </div>`;
    const arrow = () => `<div class="arch-arrow">&#8595;&nbsp;JSON / Bearer token</div>`;
    return `
    <div class="arch-wrap">
        <div class="arch-main">
            ${tier('Presentation Tier — :5500', 'Static SPA (no build step)', '<span class="chip">index.html</span><span class="chip">app.html</span><span class="chip">views.js</span><span class="chip">app.js (hash router)</span><span class="chip">api.js (fetch client)</span><span class="chip">style.css</span>')}
            ${arrow()}
            ${tier('Application Tier — :8080', 'Spring Boot 3.5 · REST API', '<span class="chip">Controllers + DTO records</span><span class="chip">Service layer (business rules)</span><span class="chip">Repositories (JPA / Specifications / JPQL)</span><span class="chip">Flyway V1..V12</span>')}
            ${arrow()}
            ${tier('Data Tier', 'MySQL 8.0 — apsfc_lms', '<span class="chip">Users / Customers / Employees</span><span class="chip">Products / Applications / Loans</span><span class="chip">EmiSchedule / Repayments</span><span class="chip">ApplicationHistory (audit)</span>')}
        </div>
        <div class="arch-sec">
            <div class="arch-sec-title">Security (cross-cutting)</div>
            <div class="arch-pill">JWT filter → SecurityContext</div>
            <div class="arch-pill">@PreAuthorize (role gate)</div>
            <div class="arch-pill">Service-level ownership checks</div>
            <div class="arch-pill">@RestControllerAdvice error envelope</div>
            <div class="arch-pill">BCrypt + .env secrets + CORS allowlist</div>
        </div>
    </div>`;
}

const SEQ = [
    ['Client (SPA)', 'Send HTTP + Authorization: Bearer &lt;jwt&gt;'],
    ['Security Filter Chain', 'Parse JWT → SecurityContextHolder (role)'],
    ['DispatcherServlet', 'Route to controller'],
    ['Controller', '@Valid (400 on field errors) → @PreAuthorize (403)'],
    ['Service', 'Business rules · precondition guards · @Transactional'],
    ['Repository', 'JPA / Specification / JPQL -> MySQL'],
    ['MySQL', 'Return rows (ACID commit)'],
    ['Service ', 'Map entities → response DTOs'],
    ['Controller ', 'ResponseEntity 200 / 201 / 4xx'],
    ['Client (SPA)', 'Render view / error toast']
];

function sequenceDiagram() {
    const steps = SEQ.map(([who, what], i) => `
        <div class="seq-row">
            <div class="seq-who">${who}</div>
            <div class="seq-arrow">${i < SEQ.length - 1 ? '&#8595;' : ''}</div>
            <div class="seq-what">${what}</div>
        </div>`).join('');
    return `<div class="seq-wrap">${steps}</div>
        <p class="fig-note"><strong>Guard order:</strong> @Valid → @PreAuthorize → precondition rules (400 <code>BUSINESS_VIOLATION</code>) → state machine (409 <code>INVALID_STATE_TRANSITION</code>).</p>`;
}

function mermaidToHtml(content) {
    if (content.includes('sequenceDiagram')) return sequenceDiagram();
    if (content.includes('APSFC') && content.includes('UC1')) return ucDiagram();
    if (content.includes('Presentation Tier')) return archDiagram();
    return `<pre class="code-diag">${escHtml(content)}</pre>`;
}

// ------------------------------------------------------------ block parser
const lines = MD.split('\n');
const out = [];
let i = 0;

function slug(s) {
    return s.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
}

function pushList(rows) { // rows: [{type:'ul'|'ol', text}]
    const tag = rows[0].type === 'ul' ? 'ul' : 'ol';
    out.push(`<${tag}>`);
    for (const r of rows) out.push(`<li>${inline(r.text)}</li>`);
    out.push(`</${tag}>`);
}

while (i < lines.length) {
    const line = lines[i];

    // fenced block
    if (/^```/.test(line)) {
        const lang = line.slice(3).trim();
        let buf = [];
        i++;
        while (i < lines.length && !/^```/.test(lines[i])) { buf.push(lines[i]); i++; }
        i++; // consume closing ```
        const content = buf.join('\n');
        if (lang === 'mermaid') out.push(mermaidToHtml(content));
        else out.push(`<pre class="code-blk"><span class="code-lang">${escHtml(lang)}</span>\n${escHtml(content)}</pre>`);
        continue;
    }

    // heading
    let m = line.match(/^(#{1,4})\s+(.*)$/);
    if (m) {
        const level = m[1].length;
        const text = m[2];
        const tag = `h${Math.min(level, 3)}`;
        const id = slug(text.replace(/\(.*\)/g, ''));
        const cls = level === 2 ? ' class="sec"' : '';
        out.push(`<${tag} id="${id}"${cls}>${escHtml(text)}</${tag}>`);
        i++;
        continue;
    }

    // horizontal rule
    if (/^\s*(--+|\*\*\*)\s*$/.test(line)) { out.push('<hr class="soft">'); i++; continue; }

    // table block
    if (/^\|/.test(line)) {
        let rows = [];
        while (i < lines.length && /^\|/.test(lines[i])) {
            rows.push(lines[i]);
            i++;
        }
        // drop separator rows like |:---|----|
        const data = rows.filter(r => !/^\|[\s:|-]+\|$/.test(r));
        const cells = data.map(r => r.trim().replace(/^\||\|$/g, '').split('|').map(c => c.trim()));
        let html = '<div class="tbl-wrap"><table>';
        cells.forEach((r, ri) => {
            const tag = ri === 0 ? 'th' : 'td';
            html += '<tr>' + r.map(c => `<${tag}>${inline(c)}</${tag}>`).join('') + '</tr>';
        });
        html += '</table></div>';
        out.push(html);
        continue;
    }

    // lists
    const ulm = line.match(/^[-*]\s+(.*)$/);
    const olm = line.match(/^(\d+)\.\s+(.*)$/);
    if (ulm || olm) {
        const type = ulm ? 'ul' : 'ol';
        const rows = [{ type, text: (ulm || olm)[1] }];
        i++;
        while (i < lines.length) {
            const l = lines[i];
            if (/^[-*]\s+/.test(l)) { rows.push({ type: 'ul', text: l.replace(/^[-*]\s+/, '') }); i++; continue; }
            const om = l.match(/^(\d+)\.\s+(.*)$/);
            if (om) { rows.push({ type: 'ol', text: om[2] }); i++; continue; }
            break;
        }
        pushList(rows);
        continue;
    }

    // blank
    if (!line.trim()) { i++; continue; }

    // paragraph (accumulate until blank/block start)
    let para = [line];
    i++;
    while (i < lines.length && lines[i].trim() && !/^```/.test(lines[i]) && !/^(#{1,4})\s/.test(lines[i]) && !/^\|/.test(lines[i]) && !/^[-*]\s+/.test(lines[i]) && !/^(\d+)\.\s/.test(lines[i])) {
        para.push(lines[i]); i++;
    }
    out.push(`<p>${para.map(inline).join('<br>')}</p>`);
}

const body = out.join('\n');

// --------------------------------------------------------------- document
const html = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>APSFC Loan Management System — SRS</title>
<style>
  @page { size: A4; margin: 14mm 13mm 16mm 13mm; }
  * { box-sizing: border-box; }
  body { font-family: "Segoe UI", Calibri, Roboto, sans-serif; color: #141c26; font-size: 10.5pt; line-height: 1.5; margin: 0; }
  h1 { font-size: 21pt; margin: 0 0 6px; }
  h2 { font-size: 15pt; color: #0f4c81; border-bottom: 2px solid #0f4c81; padding-bottom: 4px; margin: 0 0 10px; }
  h2.sec { page-break-before: always; }
  h3 { font-size: 12.5pt; color: #0f4c81; margin: 18px 0 6px; }
  p { margin: 6px 0; }
  code { background: #eef2f8; border-radius: 3px; padding: 1px 4px; font-size: 9pt; font-family: Consolas, monospace; }
  strong { color: #0a3559; }
  table { width: 100%; border-collapse: collapse; margin: 8px 0 14px; font-size: 8.6pt; }
  th { background: #0f4c81; color: #fff; text-align: left; padding: 4px 7px; border: 1px solid #0f4c81; }
  td { border: 1px solid #c9d4e0; padding: 4px 7px; vertical-align: top; }
  .tbl-wrap { page-break-inside: auto; }
  tr:nth-child(even) td { background: #f4f8fc; }
  hr.soft { border: 0; border-top: 1px dotted #b7c5d4; margin: 16px 0; }

  ul, ol { margin: 6px 0 12px; padding-left: 20px; }
  li { margin: 3px 0; }
  .code-blk { background: #0d1f33; color: #e6edf5; border-radius: 6px; padding: 10px 14px; font-family: Consolas, monospace; font-size: 8.2pt; white-space: pre-wrap; margin: 8px 0; }
  .code-lang { color: #7fd4e8; font-weight: bold; }
  .code-diag { background: #fff; border: 1px solid #c9d4e0; border-radius: 6px; padding: 12px; font-family: Consolas, monospace; font-size: 8pt; white-space: pre; overflow-x: hidden; }

  /* cover */
  .cover { text-align: center; padding-top: 130px; }
  .cover h1 { font-size: 30pt; color: #0f4c81; }
  .cover .sub { font-size: 14pt; color: #0a3559; margin-bottom: 40px; }
  .cover .logo { display: inline-block; background: #0f4c81; color: #fff; font-weight: 800; font-size: 22pt; letter-spacing: 2px; padding: 10px 26px; border-radius: 10px; margin-bottom: 26px; }
  .cover .meta { display: inline-block; text-align: left; border: 1px solid #c9d4e0; border-radius: 8px; padding: 16px 24px; font-size: 10.5pt; margin-top: 30px; }
  .cover .meta td { border: 0; padding: 2px 8px; font-size: 10pt; }
  .toc { page-break-before: always; }
  .toc a { text-decoration: none; color: #141c26; }

  /* diagrams */
  .uc-wrap { border: 1.5px dashed #0f4c81; border-radius: 10px; padding: 0; margin: 12px 0; overflow: hidden; }
  .uc-side { border-right: 1px dashed #9bb3c8; }
  .uc-sys { flex: 1; padding: 0 14px 10px; }
  .uc-systitle { text-align: center; background: #0f4c81; color: #fff; font-weight: 700; letter-spacing: .5px; padding: 6px; margin: 0 -14px 10px; }
  .uc-row { display: flex; align-items: stretch; border-bottom: 1px solid #e2e9f1; padding: 8px 0; }
  .uc-row:last-child { border-bottom: 0; }
  .uc-actor { width: 120px; font-weight: 800; color: #0a3559; font-size: 9pt; padding-top: 4px; }
  .uc-pills { flex: 1; display: flex; flex-wrap: wrap; gap: 4px; }
  .uc-pill { background: #e7eef7; border: 1px solid #b8cee4; color: #0a3559; border-radius: 999px; padding: 2px 9px; font-size: 8pt; }

  .arch-wrap { display: flex; gap: 14px; margin: 12px 0; }
  .arch-main { flex: 1.15; }
  .arch-tier { border: 1px solid #0f4c81; border-radius: 8px; padding: 8px 12px; margin-bottom: 2px; }
  .arch-tiername { font-weight: 800; color: #0f4c81; font-size: 10pt; }
  .arch-tierdesc { font-size: 8.6pt; color: #4a5a6b; }
  .arch-chips { margin-top: 5px; }
  .chip { display: inline-block; background: #e7eef7; border: 1px solid #b8cee4; border-radius: 5px; padding: 1px 7px; font-size: 7.8pt; margin: 1px 2px 1px 0; }
  .arch-arrow { text-align: center; color: #0f4c81; font-weight: 600; font-size: 8.6pt; padding: 1px 0; }
  .arch-sec { flex: 0.85; border: 1px dashed #5a7a99; border-radius: 8px; padding: 10px 12px; align-self: start; }
  .arch-sec-title { font-weight: 800; color: #0a3559; margin-bottom: 8px; }
  .arch-pill { background: #f2f7fc; border: 1px solid #c8d9e9; border-radius: 6px; padding: 5px 8px; font-size: 8.4pt; margin-bottom: 5px; }

  .seq-wrap { border: 1px solid #c9d4e0; border-radius: 8px; padding: 10px 14px; margin: 12px 0; }
  .seq-row { display: flex; align-items: center; gap: 10px; }
  .seq-who { width: 150px; font-weight: 700; color: #0a3559; background: #eef2f8; border-radius: 5px; padding: 3px 8px; font-size: 8.6pt; }
  .seq-arrow { width: 18px; text-align: center; color: #0f4c81; }
  .seq-what { flex: 1; font-size: 8.6pt; color: #2b3a49; }
  .fig-note { font-size: 8.4pt; color: #40536a; background: #f2f7fc; border-left: 3px solid #0f4c81; padding: 5px 9px; border-radius: 4px; }
</style>
</head>
<body>

<div class="cover">
  <div class="logo">APSFC</div>
  <h1>Loan Management System</h1>
  <div class="sub">Software Requirements Specification (SRS)</div>
  <div class="meta">
    <table>
      <tr><td><strong>Version</strong></td><td>1.0 — Approved</td></tr>
      <tr><td><strong>Prepared by</strong></td><td>Senior Software Engineer</td></tr>
      <tr><td><strong>Date</strong></td><td>18-Sep-2026</td></tr>
      <tr><td><strong>Includes</strong></td><td>Requirement analysis · Use cases · Architecture · RBAC</td></tr>
    </table>
  </div>
</div>

<noscript><p style="color:#c0392b">JavaScript required for diagram rendering.</p></noscript>

${body}

</body>
</html>
`;

fs.writeFileSync(path.join(ROOT, 'docs', 'SRS.html'), html);
console.log('HTML written: docs/SRS.html (' + html.length + ' bytes)');

// ---------------------------------------------------------------- print PDF
const CHROME_CANDIDATES = [
    'C:/Program Files/Google/Chrome/Application/chrome.exe',
    'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
    '/usr/bin/google-chrome',
    '/usr/bin/chromium',
    'C:/Program Files/Chromium/Application/chrome.exe'
];
const chrome = CHROME_CANDIDATES.find(c => fs.existsSync(c));
if (!chrome) { console.log('Chrome not found — PDF skipped (docs/SRS.html remains).'); process.exit(0); }

const url = 'file:///' + path.join(ROOT, 'docs', 'SRS.html').replace(/\\/g, '/');
const pdf = path.join(ROOT, 'docs', 'SRS.pdf');
try {
    execFileSync(chrome, [
        '--headless=new', '--disable-gpu', '--no-sandbox',
        '--no-pdf-header-footer',
        '--print-to-pdf=' + pdf,
        url
    ], { stdio: ['ignore', 'pipe', 'pipe'] });
    console.log('PDF written: docs/SRS.pdf');
} catch (e) {
    console.error('PDF print failed:', e.stderr ? e.stderr.toString() : e.message);
    process.exit(1);
}