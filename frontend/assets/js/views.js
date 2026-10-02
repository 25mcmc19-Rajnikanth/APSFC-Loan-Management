// Route views. Each view(route, role, ...parts) returns an HTML promise resolved by the router.

const Views = {};

// ---------------- HOME / DASHBOARD ----------------

Views.home = async () => {
    const u = await getUser();
    const isCustomer = u.role === 'CUSTOMER';
    if (!isCustomer) return Views.staffHome(u);
    return Views.customerHome(u);
};

Views.customerHome = async (u) => {
    const [apps, loans, pays] = await Promise.all([
        ace.get('/loan-applications/mine', { size: 100 }),
        ace.get('/loans/mine', { size: 100 }),
        ace.get('/repayments/mine', { size: 1 })
    ]);
    const active = loans.content.filter(l => l.status === 'ACTIVE');
    const due = active.reduce((s, l) => s + Number(l.outstandingBalance || 0), 0);

    const recent = [...apps.content].sort((a, b) => b.id - a.id).slice(0, 5);
    return `
    <div class="page-head"><div>
      <h1>Welcome back, ${escapeHtml(u.fullName)}</h1>
      <p class="muted">Your personal loan workspace</p>
    </div>
    <button class="btn btn-primary" onclick="router.go('#/apply')">+ Apply for a loan</button></div>
    <div class="stats">
      ${statCard('My Applications', apps.totalElements)}
      ${statCard('Active Loans', active.length)}
      ${statCard('Outstanding', money(due))}
    </div>
    <div class="card"><div class="card-head"><h3>Recent applications</h3></div>
      <div class="tbl">
        <div class="th row"><b>Number</b><b>Product</b><b>Amount</b><b>EMI</b><b>Status</b></div>
        ${recent.length ? recent.map(a => `<a class="row click" href="#/app-detail/${a.id}">
          <span>${a.applicationNumber}</span><span>${escapeHtml(a.productName)}</span>
          <span>${money(a.loanAmount)}</span><span>${money(a.monthlyEmi)}</span><span>${badge(a.status)}</span>
        </a>`).join('') : '<div class="row muted">No applications yet</div>'}
      </div>
    </div>`;
};

Views.staffHome = async (u) => {
    const s = await ace.get('/dashboard/summary');
    return `
    <div class="page-head"><div>
      <h1>Portfolio dashboard</h1><p class="muted">${escapeHtml(u.fullName)} · ${escapeHtml(u.role)}</p>
    </div></div>
    <div class="stats">
      ${statCard('Customers', s.totalCustomers)}
      ${statCard('Total Loans', s.totalLoans, s.activeLoans + ' active · ' + s.closedLoans + ' closed')}
      ${statCard('Outstanding', money(s.totalOutstanding))}
      ${statCard('Disbursed (all time)', money(s.totalDisbursed), 'This month: ' + money(s.disbursedThisMonth))}
      ${statCard('Collections', money(s.totalCollections), 'This month: ' + money(s.collectionsThisMonth))}
      ${statCard('EMI health', s.upcomingEmiCount + ' upcoming', s.overdueEmiCount + ' overdue')}
    </div>
    <div class="grid2">
      <div class="card"><div class="card-head"><h3>Application funnel</h3></div>
        <div class="rows">
        ${Object.entries(s.applicationsByStatus || {}).map(([k, v]) =>
          `<div class="gap-row"><span>${badge(k)}</span><b>${v}</b></div>`).join('')}
        </div>
      </div>
      <div class="card"><div class="card-head"><h3>Quick actions</h3></div>
        <div class="rows">
          ${u.role === 'LOAN_OFFICER' ? `
            <a class="btn btn-outline" href="#/staff-apps?status=SUBMITTED">Review submitted applications</a>
            <a class="btn btn-outline" href="#/staff-apps?status=UNDER_REVIEW">Under review queue</a>` : ''}
          ${u.role === 'MANAGER' ? `
            <a class="btn btn-outline" href="#/staff-apps?status=UNDER_REVIEW">Approve / reject queue</a>
            <a class="btn btn-outline" href="#/staff-apps?status=APPROVED">Ready to disburse</a>` : ''}
          <a class="btn btn-outline" href="#/staff-loans">All loans</a>
          <a class="btn btn-outline" href="#/reports">Reports</a>
        </div>
      </div>
    </div>`;
};

// ---------------- CUSTOMER: APPLY ----------------

Views.apply = async () => {
    const p = await ace.get('/loan-products', { status: 'ACTIVE', size: 100 });
    return `
    <div class="page-head"><div><h1>Apply for a loan</h1><p class="muted">Start a draft — it becomes reviewable only after you submit it.</p></div></div>
    <div class="card narrow">
      <form id="apply-form">
        <label>Loan product
          <select name="productId" required onchange="window._productSel()">
            <option value="">Select a product…</option>
            ${p.content.map(pr => `<option value="${pr.id}" data-min="${pr.minAmount}" data-max="${pr.maxAmount}"
                data-rate="${pr.baseInterestRate}" data-tmin="${pr.minTenureMonths}" data-tmax="${pr.maxTenureMonths}">
                ${escapeHtml(pr.name)} — ${money(pr.minAmount)} … ${money(pr.maxAmount)} @ ${pct(pr.baseInterestRate)}</option>`).join('')}
          </select>
        </label>
        <div class="row2">
          <label>Amount (₹)
            <input name="loanAmount" type="number" min="0" step="0.01" required placeholder="e.g. 300000">
          </label>
          <label>Tenure (months)
            <input name="tenureMonths" type="number" min="1" required placeholder="e.g. 36">
          </label>
        </div>
        <label>Purpose <textarea name="purpose" rows="3" placeholder="What is this loan for?"></textarea></label>
        <div class="hint" id="apply-info"></div>
        <button class="btn btn-primary" type="submit">Create draft</button>
      </form>
    </div>
    <script>
      window._productSel = () => {
        const sel = document.querySelector('#apply-form select[name=productId]');
        const o = sel.selectedOptions[0];
        const info = document.getElementById('apply-info');
        if (!o || o.value === '') { info.textContent = ''; return; }
        info.textContent = 'Eligible range: ' + money(o.dataset.min) + ' – ' + money(o.dataset.max)
          + ' · tenure ' + o.dataset.tmin + '–' + o.dataset.tmax + ' months · interest ' + o.dataset.rate + '% p.a.';
      };
      document.getElementById('apply-form').addEventListener('submit', async e => {
        e.preventDefault();
        const f = new FormData(e.target);
        const body = { productId: +f.get('productId'), loanAmount: +f.get('loanAmount'),
          tenureMonths: +f.get('tenureMonths'), purpose: f.get('purpose') || undefined };
        const btn = e.target.querySelector('button'); btn.disabled = true;
        try {
          const app = await ace.post('/loan-applications', body);
          toast('Draft created: ' + app.applicationNumber, 'ok');
          router.go('#/app-detail/' + app.id);
        } catch (err) { toast(err.message); btn.disabled = false; }
      });
    <\/script>`;
};

// ---------------- APPLICATIONS ----------------

Views['my-apps'] = async () => {
    const q = await ace.get('/loan-applications/mine', { size: 100 });
    const rows = [...q.content].sort((a, b) => b.id - a.id);
    return `
    <div class="page-head"><div><h1>My applications</h1><p class="muted">${rows.length} total</p></div>
      <button class="btn btn-primary" onclick="router.go('#/apply')">+ New application</button></div>
    <div class="card"><div class="tbl">
      <div class="th row"><b>Number</b><b>Product</b><b>Amount</b><b>EMI</b><b>Status</b><b>Submitted</b></div>
      ${rows.length ? rows.map(a => `<a class="row click" href="#/app-detail/${a.id}">
        <span>${a.applicationNumber}</span><span>${escapeHtml(a.productName)}</span>
        <span>${money(a.loanAmount)}</span><span>${money(a.monthlyEmi)}</span>
        <span>${badge(a.status)}</span><span>${datetime(a.submittedAt)}</span></a>`).join('')
        : '<div class="row muted">No applications. Start one above.</div>'}
    </div></div>`;
};

async function appActionsBar(app, role) {
    const items = [];
    if (app.status === 'DRAFT') {
        items.push(`<button class="btn btn-outline" onclick="editDraft(${app.id})">Edit draft</button>`);
        items.push(`<button class="btn btn-primary" onclick="submitApp(${app.id})">Submit</button>`);
        items.push(`<button class="btn btn-ghost" onclick="cancelApp(${app.id})">Cancel</button>`);
    } else if (app.status === 'SUBMITTED') {
        items.push(`<button class="btn btn-ghost" onclick="cancelApp(${app.id})">Withdraw</button>`);
        if (role === 'LOAN_OFFICER') items.push(`<button class="btn btn-primary" onclick="startReview(${app.id})">Start review</button>`);
    } else if (app.status === 'UNDER_REVIEW') {
        if (role === 'LOAN_OFFICER') {
            items.push(`<button class="btn btn-outline" onclick="recommend(${app.id},'APPROVE')">Recommend approve</button>`);
            items.push(`<button class="btn btn-ghost" onclick="recommend(${app.id},'REJECT')">Recommend reject</button>`);
        }
        if (role === 'MANAGER') {
            items.push(`<button class="btn btn-primary" onclick="decideApp(${app.id},'approve')">Approve</button>`);
            items.push(`<button class="btn btn-danger-ghost" onclick="decideApp(${app.id},'reject')">Reject</button>`);
        }
    } else if (app.status === 'APPROVED') {
        if (role === 'MANAGER') items.push(`<button class="btn btn-primary" onclick="disburseApp(${app.id}, ${app.loanAmount})">Disburse loan</button>`);
    }
    return items.join(' ');
}

Views['app-detail'] = async (_, __, id) => {
    const u = await getUser();
    const app = await ace.get('/loan-applications/' + id);
    const hist = await ace.get('/loan-applications/' + id + '/history');
    const actions = await appActionsBar(app, u.role);
    return `
    <div class="page-head"><div>
      <a class="muted noline" href="#/my-apps">&larr; Applications</a>
      <h1>${app.applicationNumber}</h1><p class="muted">${escapeHtml(app.productName)} · ${badge(app.status)}</p>
    </div><div>${actions}</div></div>
    <div class="grid2">
      <div class="card">
        <h3>Loan detail</h3>
        <table class="kv">
          <tr><td>Customer</td><td>${escapeHtml(app.customerName)}</td></tr>
          <tr><td>Product</td><td>${escapeHtml(app.productName)}</td></tr>
          <tr><td>Amount</td><td><b>${money(app.loanAmount)}</b></td></tr>
          <tr><td>Tenure</td><td>${app.tenureMonths} months</td></tr>
          <tr><td>Interest rate</td><td>${pct(app.interestRate)}</td></tr>
          <tr><td>Monthly EMI</td><td><b>${money(app.monthlyEmi)}</b></td></tr>
          <tr><td>Purpose</td><td>${escapeHtml(app.purpose || '—')}</td></tr>
          <tr><td>Created</td><td>${datetime(app.createdAt)}</td></tr>
        </table>
      </div>
      <div class="card">
        <h3>Review trail</h3>
        ${reviewMeta('Submitted by', app.submittedByName, app.submittedAt)}
        ${reviewMeta('Reviewed by', app.reviewedByName, app.reviewedAt)}
        ${app.reviewComment ? `<div class="note">Officer note: ${escapeHtml(app.reviewComment)}</div>` : ''}
        ${reviewMeta('Decision by', app.approvedByName, app.decidedAt)}
        ${app.decisionComment ? `<div class="note note-warn">Decision: ${escapeHtml(app.decisionComment)}</div>` : ''}
        <h4 style="margin-top:18px">History</h4>
        <div class="timeline">
          ${hist.map(h => `<div class="tl">
            <div class="tl-dot"></div>
            <div><b>${escapeHtml(h.action)}</b> → ${escapeHtml(h.toStatus)}<br>
              <span class="muted">${escapeHtml(h.performedByName)} · ${datetime(h.performedAt)}</span>
              ${h.comment ? `<div class="note">${escapeHtml(h.comment)}</div>` : ''}</div>
          </div>`).join('')}
        </div>
      </div>
    </div>`;
};

function reviewMeta(label, name, at) {
    return name ? `<div class="gap-row"><span class="muted">${label}</span><span>
      ${escapeHtml(name)}${at ? ' · ' + datetime(at) : ''}</span></div>` : '';
}

// ---------------- STAFF APPLICATIONS QUEUE ----------------

Views['staff-apps'] = async () => {
    const u = await getUser();
    const params = new URLSearchParams(location.hash.split('?')[1]);
    let status = params.get('status') || '';
    let productId = (params.get('productId') || '');
    let page = +(params.get('page') || 0);

    const q = await ace.get('/loan-applications', {
        status: status || undefined, productId: productId || undefined,
        page: 0, size: 50
    });
    const rows = q.content;
    const statuses = ['', 'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'DISBURSED', 'REJECTED', 'CANCELLED', 'DRAFT'];
    return `
    <div class="page-head"><div><h1>Applications</h1><p class="muted">${q.totalElements} matching</p></div></div>
    <div class="card">
      <div class="filters">
        <select onchange="router.go('#/staff-apps?status='+encodeURIComponent(this.value))">
          ${statuses.map(s => `<option value="${s}" ${s === status ? 'selected' : ''}>${s || 'All statuses'}</option>`).join('')}
        </select>
      </div>
      <div class="tbl">
        <div class="th row"><b>Number</b><b>Customer</b><b>Product</b><b>Amount</b><b>Status</b><b>Updated</b></div>
        ${rows.length ? rows.map(a => `<a class="row click" href="#/app-detail/${a.id}">
          <span>${a.applicationNumber}</span><span>${escapeHtml(a.customerName)}</span>
          <span>${escapeHtml(a.productName)}</span><span>${money(a.loanAmount)}</span>
          <span>${badge(a.status)}</span><span>${datetime(a.createdAt)}</span></a>`).join('')
          : '<div class="row muted">Nothing here</div>'}
      </div>
    </div>`;
};

// ---------------- LOANS ----------------

Views['my-loans'] = async () => {
    const q = await ace.get('/loans/mine', { size: 100 });
    const rows = [...q.content].sort((a, b) => b.id - a.id);
    return `
    <div class="page-head"><div><h1>My loans</h1><p class="muted">${rows.length} loans</p></div></div>
    <div class="card"><div class="tbl">
      <div class="th row"><b>Loan number</b><b>Product</b><b>Principal</b><b>EMI</b><b>Outstanding</b><b>Status</b></div>
      ${rows.length ? rows.map(l => `<a class="row click ${l.status === 'CLOSED' ? 'dim' : ''}" href="#/loan-detail/${l.id}">
        <span>${l.loanNumber}</span><span>${escapeHtml(l.productName)}</span><span>${money(l.principalAmount)}</span>
        <span>${money(l.monthlyEmi)}</span><span>${money(l.outstandingBalance)}</span><span>${badge(l.status)}</span></a>`).join('')
        : '<div class="row muted">No loans yet — your approved application must be disbursed by the branch.</div>'}
    </div></div>`;
};

Views['staff-loans'] = async () => {
    const q = await ace.get('/loans', { size: 100 });
    return `
    <div class="page-head"><div><h1>Loans</h1><p class="muted">${q.totalElements} disbursed</p></div></div>
    <div class="card"><div class="tbl">
      <div class="th row"><b>Loan number</b><b>Customer</b><b>Product</b><b>Disbursed</b><b>EMI</b><b>Outstanding</b><b>Status</b></div>
      ${q.content.map(l => `<a class="row click" href="#/loan-detail/${l.id}">
        <span>${l.loanNumber}</span><span>${escapeHtml(l.customerName)}</span><span>${escapeHtml(l.productName)}</span>
        <span>${money(l.disbursedAmount)}</span><span>${money(l.monthlyEmi)}</span>
        <span>${money(l.outstandingBalance)}</span><span>${badge(l.status)}</span></a>`).join('')}
    </div></div>`;
};

Views['loan-detail'] = async (_, __, id) => {
    const u = await getUser();
    const [loan, emis, pays] = await Promise.all([
        ace.get('/loans/' + id),
        ace.get('/loans/' + id + '/emi'),
        ace.get('/loans/' + id + '/repayments', { size: 50 })
    ]);
    const canPay = (u.role === 'CUSTOMER' || ['LOAN_OFFICER', 'MANAGER', 'ADMIN'].includes(u.role)) && loan.status === 'ACTIVE';
    return `
    <div class="page-head"><div>
      <a class="muted noline" href="${u.role === 'CUSTOMER' ? '#/my-loans' : '#/staff-loans'}">&larr; Loans</a>
      <h1>${loan.loanNumber}</h1><p class="muted">${escapeHtml(loan.customerName)} · ${badge(loan.status)} · disbursed ${date(loan.disbursementDate)} by ${escapeHtml(loan.disbursedByName || '—')}</p>
    </div>${canPay ? `<button class="btn btn-primary" onclick="payModal(${loan.id})">Record payment</button>` : ''}</div>
    <div class="stats">
      ${statCard('Principal', money(loan.principalAmount))}
      ${statCard('EMI / month', money(loan.monthlyEmi))}
      ${statCard('Total payable', money(loan.totalAmount), 'Interest ' + money(loan.totalInterest))}
      ${statCard('Paid', money(loan.amountPaid))}
      ${statCard('Outstanding', money(loan.outstandingBalance), loan.status === 'CLOSED' ? 'Closed ' + date(loan.closedAt) : '')}
    </div>
    <div class="grid2">
      <div class="card">
        <h3>EMI schedule (${emis.length})</h3>
        <div class="tbl">
          <div class="th row"><b>#</b><b>Due</b><b>Principal</b><b>Interest</b><b>Total</b><b>Paid</b><b>Status</b></div>
          ${emis.map(e => `<div class="row">
            <span>${e.emiNumber}</span><span>${date(e.dueDate)}</span>
            <span>${money(e.principalComponent)}</span><span>${money(e.interestComponent)}</span>
            <span>${money(e.totalAmount)}</span><span>${money(e.cumulativePaid)}</span>
            <span>${badge(e.paymentStatus)}</span></div>`).join('')}
        </div>
      </div>
      <div class="card">
        <h3>Payment history (${pays.totalElements})</h3>
        <div class="tbl">
          <div class="th row"><b>EMI</b><b>Amount</b><b>Mode</b><b>Date</b><b>By</b></div>
          ${pays.content.length ? pays.content.map(p => `<div class="row">
            <span>#${p.emiNumber ?? '—'}</span><span>${money(p.amount)}</span><span>${escapeHtml(p.paymentMode)}</span>
            <span>${date(p.paymentDate)}</span><span>${escapeHtml(p.recordedByName || '—')}</span></div>`).join('')
            : '<div class="row muted">No payments yet</div>'}
        </div>
      </div>
    </div>`;
};

// ---------------- CUSTOMERS (staff) ----------------

Views['customers'] = async () => {
    const params = new URLSearchParams(location.hash.split('?')[1]);
    const q = await ace.get('/customers', { search: params.get('search') || undefined, size: 100 });
    return `
    <div class="page-head"><div><h1>Customers</h1><p class="muted">${q.totalElements} registered</p></div></div>
    <div class="card">
      <div class="filters">
        <input placeholder="Search name / user / PAN / phone…" value="${escapeHtml(params.get('search') || '')}"
          onkeydown="if(event.key==='Enter')router.go('#/customers?search='+encodeURIComponent(this.value))">
        <button class="btn btn-outline" onclick="router.go('#/customers?search='+encodeURIComponent(document.querySelector('#customers input').value))">Search</button>
      </div>
      <div class="tbl">
        <div class="th row"><b>Name</b><b>Username</b><b>PAN</b><b>Aadhaar</b><b>Loans</b><b>Outstanding</b><b>KYC</b><b>Status</b></div>
        ${q.content.map(c => `<a class="row click" href="#/customer/${c.id}">
          <span><b>${escapeHtml(c.fullName)}</b></span><span>@${escapeHtml(c.username)}</span>
          <span>${escapeHtml(c.panNumber)}</span><span>${escapeHtml(c.aadhaarNumberMasked)}</span>
          <span>${c.activeLoanCount}</span><span>${money(c.outstandingBalance)}</span>
          <span>${c.kycVerified ? badge('PAID') : badge('PENDING')}</span><span>${badge(c.status)}</span></a>`).join('')}
      </div>
    </div>`;
};

Views['customer'] = async (_, __, id) => {
    const u = await getUser();
    const c = await ace.get('/customers/' + id);
    const canAdmin = ['MANAGER', 'ADMIN'].includes(u.role);
    const kycBadge = c.kycVerified ? 'PAID' : 'DRAFT';
    return `
    <div class="page-head"><div>
      <a class="muted noline" href="#/customers">&larr; Customers</a>
      <h1>${escapeHtml(c.fullName)}</h1><p class="muted">@${escapeHtml(c.username)} · ${badge(c.status)}</p>
    </div>
    <div>${canAdmin && !c.kycVerified ? `<button class="btn btn-primary" onclick="verifyKyc(${c.id})">Verify KYC</button>` : ''}
      ${canAdmin ? `<button class="btn btn-outline" onclick="toggleCustomer(${c.id}, '${c.status}')">${c.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}</button>` : ''}</div></div>
    <div class="grid2">
      <div class="card"><h3>Profile</h3>
        <table class="kv">
          <tr><td>Email</td><td>${escapeHtml(c.email)}</td></tr>
          <tr><td>Phone</td><td>${escapeHtml(c.phone || '—')}</td></tr>
          <tr><td>PAN</td><td>${escapeHtml(c.panNumber)}</td></tr>
          <tr><td>Aadhaar</td><td>${escapeHtml(c.aadhaarNumberMasked)}</td></tr>
          <tr><td>Date of birth</td><td>${date(c.dateOfBirth)}</td></tr>
          <tr><td>Occupation</td><td>${escapeHtml(c.employmentType || '—')}</td></tr>
          <tr><td>Location</td><td>${escapeHtml([c.city, c.state].filter(Boolean).join(', ') || '—')} ${escapeHtml(c.pincode || '')}</td></tr>
          <tr><td>KYC</td><td>${badge(kycBadge)}</td></tr>
        </table>
      </div>
      <div class="card"><h3>Loan position</h3>
        <div class="rows">
          <div class="gap-row"><span class="muted">Active loans</span><b>${c.activeLoanCount}</b></div>
          <div class="gap-row"><span class="muted">Outstanding</span><b>${money(c.outstandingBalance)}</b></div>
          <div class="gap-row"><span class="muted">Registered</span><b>${date(c.createdAt)}</b></div>
        </div>
      </div>
    </div>`;
};

// ---------------- EMPLOYEES (admin) ----------------

Views['employees'] = async () => {
    const u = await getUser();
    const q = await ace.get('/employees', { size: 100 });
    const canAdmin = u.role === 'ADMIN';
    return `
    <div class="page-head"><div><h1>Employees</h1><p class="muted">${q.totalElements} staff</p></div>
      ${canAdmin ? `<button class="btn btn-primary" onclick="employeeCreate()">+ Provision staff</button>` : ''}</div>
    <div class="card"><div class="tbl">
      <div class="th row"><b>Code</b><b>Name</b><b>Role</b><b>Designation</b><b>Department</b><b>Status</b>${canAdmin ? '<b>Actions</b>' : ''}</div>
      ${q.content.map(e => `<div class="row">
        <span>${escapeHtml(e.employeeCode)}</span>
        <span><b>${escapeHtml(e.fullName)}</b><br><span class="muted">@${escapeHtml(e.username)}</span></span>
        <span>${escapeHtml(e.role)}</span><span>${escapeHtml(e.designation || '—')}</span>
        <span>${escapeHtml(e.department || '—')}</span><span>${badge(e.status)}</span>
        ${canAdmin ? `<span>
            <button class="btn-mini" onclick="employeeEdit(${e.id})">Edit</button>
            <button class="btn-mini" onclick="toggleEmployee(${e.id}, '${e.status}')">${e.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}</button>
          </span>` : ''}
      </div>`).join('')}
    </div></div>`;
};

// ---------------- REPORTS (staff) ----------------

Views['reports'] = async () => {
    const [s, port, upcoming, overdue, cols] = await Promise.all([
        ace.get('/dashboard/summary'),
        ace.get('/reports/portfolio-by-product'),
        ace.get('/reports/upcoming-emis', { days: 30 }),
        ace.get('/reports/overdue-emis'),
        ace.get('/reports/collections')
    ]);
    return `
    <div class="page-head"><div><h1>Reports</h1><p class="muted">Portfolio analytics</p></div></div>
    <div class="grid2">
      <div class="card"><h3>Portfolio by product (active)</h3>
        <div class="tbl"><div class="th row"><b>Product</b><b>Loans</b><b>Disbursed</b><b>Outstanding</b></div>
        ${port.length ? port.map(r => `<div class="row"><span>${escapeHtml(r.productName)}</span><span>${r.loanCount}</span>
          <span>${money(r.disbursed)}</span><span>${money(r.outstanding)}</span></div>`).join('')
          : '<div class="row muted">No active loans</div>'}</div>
      </div>
      <div class="card"><h3>Collections by mode (last 30 days)</h3>
        <div class="tbl"><div class="th row"><b>Mode</b><b>Payments</b><b>Amount</b></div>
        ${cols.length ? cols.map(c => `<div class="row"><span>${escapeHtml(c.paymentMode)}</span><span>${c.paymentCount}</span>
          <span>${money(c.totalCollected)}</span></div>`).join('') : '<div class="row muted">No collections</div>'}</div>
      </div>
    </div>
    <div class="grid2">
      <div class="card"><h3>Upcoming EMIs (30 days) — ${upcoming.length}</h3>
        <div class="tbl"><div class="th row"><b>Due</b><b>Loan</b><b>Customer</b><b>EMI #</b><b>Remaining</b></div>
        ${upcoming.length ? upcoming.slice(0, 15).map(e => `<div class="row"><span>${date(e.dueDate)}</span><span>${e.loanNumber}</span>
          <span>${escapeHtml(e.customerName)}</span><span>${e.emiNumber}</span><span>${money(e.remainingDue)}</span></div>`).join('')
          : '<div class="row muted">Nothing due</div>'}</div>
      </div>
      <div class="card"><h3>Overdue EMIs — ${overdue.length}</h3>
        <div class="tbl"><div class="th row"><b>Due</b><b>Loan</b><b>Customer</b><b>EMI #</b><b>Remaining</b></div>
        ${overdue.length ? overdue.slice(0, 15).map(e => `<div class="row"><span>${date(e.dueDate)}</span><span>${e.loanNumber}</span>
          <span>${escapeHtml(e.customerName)}</span><span>${e.emiNumber}</span><span>${money(e.remainingDue)}</span></div>`).join('')
          : '<div class="row muted">All caught up</div>'}</div>
      </div>
    </div>`;
};

// ---------------- PROFILE ----------------

Views['profile'] = async () => {
    const u = await getUser();
    return `
    <div class="page-head"><div><h1>Profile</h1><p class="muted">How the API sees you</p></div></div>
    <div class="card narrow">
      <table class="kv">
        <tr><td>Name</td><td>${escapeHtml(u.fullName)}</td></tr>
        <tr><td>Username</td><td>@${escapeHtml(u.username)}</td></tr>
        <tr><td>Email</td><td>${escapeHtml(u.email)}</td></tr>
        <tr><td>Phone</td><td>${escapeHtml(u.phone || '—')}</td></tr>
        <tr><td>Role</td><td>${badge(u.role)}</td></tr>
        <tr><td>Account status</td><td>${badge(u.status)}</td></tr>
      </table>
    </div>`;
};

// ---------------- PAYMENTS (customer) ----------------

Views['payments'] = async () => {
    const q = await ace.get('/repayments/mine', { size: 100 });
    const rows = [...q.content].sort((a, b) => b.id - a.id);
    const total = rows.reduce((s, r) => s + Number(r.amount), 0);
    return `
    <div class="page-head"><div><h1>Payments</h1><p class="muted">${rows.length} records · ${money(total)} paid</p></div></div>
    <div class="card"><div class="tbl">
      <div class="th row"><b>Date</b><b>Loan</b><b>EMI #</b><b>Amount</b><b>Mode</b><b>Reference</b><b>Recorded by</b></div>
      ${rows.length ? rows.map(p => `<div class="row">
        <span>${date(p.paymentDate)}</span><span>${escapeHtml(p.loanNumber)}</span>
        <span>${p.emiNumber ? '#' + p.emiNumber : '—'}</span><span><b>${money(p.amount)}</b></span>
        <span>${escapeHtml(p.paymentMode)}</span><span>${escapeHtml(p.transactionRef || '—')}</span>
        <span>${escapeHtml(p.recordedByName || '—')}</span></div>`).join('')
        : '<div class="row muted">No payments yet</div>'}
    </div></div>`;
};

// ---------------- EDIT DRAFT (customer) ----------------

Views['app-detail-edit'] = async (_, __, id) => {
    const app = await ace.get('/loan-applications/' + id);
    if (app.status !== 'DRAFT') {
        toast('Only drafts can be edited');
        router.go('#/app-detail/' + id);
        return '';
    }
    const p = await ace.get('/loan-products', { status: 'ACTIVE', size: 100 });
    return `
    <div class="page-head"><div>
      <a class="muted noline" href="#/app-detail/${id}">&larr; ${app.applicationNumber}</a>
      <h1>Edit draft</h1></div></div>
    <div class="card narrow">
      <form id="edit-form">
        <label>Loan product
          <select name="productId">
            ${p.content.map(pr => `<option value="${pr.id}" ${pr.id === app.productId ? 'selected' : ''}>${escapeHtml(pr.name)}</option>`).join('')}
          </select>
        </label>
        <div class="row2">
          <label>Amount (₹)<input name="loanAmount" type="number" step="0.01" required value="${app.loanAmount}"></label>
          <label>Tenure (months)<input name="tenureMonths" type="number" min="1" required value="${app.tenureMonths}"></label>
        </div>
        <label>Purpose <textarea name="purpose" rows="3">${escapeHtml(app.purpose || '')}</textarea></label>
        <button class="btn btn-primary" type="submit">Save changes</button>
        <button class="btn btn-ghost" type="button" onclick="router.go('#/app-detail/${id}')">Back</button>
      </form>
    </div>
    <script>
      document.getElementById('edit-form').addEventListener('submit', async e => {
        e.preventDefault();
        const f = new FormData(e.target);
        const body = { productId: +f.get('productId'), loanAmount: +f.get('loanAmount'),
          tenureMonths: +f.get('tenureMonths'), purpose: f.get('purpose') || undefined };
        try {
          const a = await ace.put('/loan-applications/${id}', body);
          toast('Draft updated', 'ok');
          router.go('#/app-detail/${id}');
        } catch (err) { toast(err.message); }
      });
    <\/script>`;
};

// ---------------- EMPLOYEE EDIT (admin) ----------------

Views['employee-edit'] = async (_, __, id) => {
    const e = await ace.get('/employees/' + id);
    return `
    <div class="page-head"><div>
      <a class="muted noline" href="#/employees">&larr; Employees</a>
      <h1>${escapeHtml(e.fullName)} · ${escapeHtml(e.employeeCode)}</h1></div></div>
    <div class="card narrow">
      <form id="emp-edit-form">
        <label>Designation<input name="designation" value="${escapeHtml(e.designation || '')}"></label>
        <label>Department<input name="department" value="${escapeHtml(e.department || '')}"></label>
        <div class="row2">
          <label>Joining date<input name="joiningDate" type="date" value="${e.joiningDate || ''}"></label>
          <label>Phone<input name="phone" value="${escapeHtml(e.phone || '')}"></label>
        </div>
        <button class="btn btn-primary" type="submit">Save</button>
      </form>
    </div>
    <script>
      document.getElementById('emp-edit-form').addEventListener('submit', async e => {
        e.preventDefault();
        const f = new FormData(e.target);
        const body = {};
        if (f.get('designation')) body.designation = f.get('designation');
        if (f.get('department')) body.department = f.get('department');
        if (f.get('joiningDate')) body.joiningDate = f.get('joiningDate');
        if (f.get('phone')) body.phone = f.get('phone');
        try {
          await ace.put('/employees/${id}', body);
          toast('Employee updated', 'ok');
          router.go('#/employees');
        } catch (err) { toast(err.message); }
      });
    <\/script>`;
};