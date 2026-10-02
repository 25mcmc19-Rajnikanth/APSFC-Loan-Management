// SPA router + global actions for APSFC LoanOS

const NAV = {
    CUSTOMER: [
        ['#/home', 'Home'],
        ['#/apply', 'Apply for loan'],
        ['#/my-apps', 'My applications'],
        ['#/my-loans', 'My loans'],
        ['#/payments', 'Payments'],
        ['#/profile', 'Profile']
    ],
    LOAN_OFFICER: [
        ['#/home', 'Dashboard'],
        ['#/staff-apps?status=SUBMITTED', 'Review queue'],
        ['#/staff-apps?status=UNDER_REVIEW', 'Under review'],
        ['#/staff-loans', 'Loans'],
        ['#/customers', 'Customers'],
        ['#/reports', 'Reports'],
        ['#/profile', 'Profile']
    ],
    MANAGER: [
        ['#/home', 'Dashboard'],
        ['#/staff-apps?status=UNDER_REVIEW', 'Approval queue'],
        ['#/staff-apps?status=APPROVED', 'Disbursements'],
        ['#/staff-loans', 'Loans'],
        ['#/customers', 'Customers'],
        ['#/reports', 'Reports'],
        ['#/profile', 'Profile']
    ],
    ADMIN: [
        ['#/home', 'Dashboard'],
        ['#/staff-apps', 'Applications'],
        ['#/staff-loans', 'Loans'],
        ['#/customers', 'Customers'],
        ['#/employees', 'Employees'],
        ['#/reports', 'Reports'],
        ['#/profile', 'Profile']
    ]
};

const router = {
    async go(hash) {
        location.hash = hash || '#/home';
    },
    async route() {
        const u = await getUser();
        if (!u) return;
        buildNav(u);
        const raw = location.hash.replace(/^#\//, '') || 'home';
        const [path, qs = ''] = raw.split('?');
        const parts = path.split('/');
        const view = parts[0];

        let allowed = false;
        const customerOnly = ['home', 'apply', 'my-apps', 'my-loans', 'payments', 'profile', 'app-detail-edit'].includes(view);
        const staffOnly = ['staff-apps', 'staff-loans', 'customers', 'customer', 'employees', 'reports', 'employee-edit'].includes(view);
        if (view === 'app-detail' || view === 'loan-detail') allowed = true; // service enforces ownership
        if (customerOnly) allowed = u.role === 'CUSTOMER';
        if (staffOnly) allowed = ['LOAN_OFFICER', 'MANAGER', 'ADMIN'].includes(u.role);
        if (view === 'employees' || view === 'employee-edit') allowed = u.role === 'ADMIN' || u.role === 'MANAGER';

        if (!allowed) { toast('Not permitted for your role'); location.hash = '#/home'; return; }

        const host = document.getElementById('view');
        host.innerHTML = '<div class="loading">Loading…</div>';
        try {
            const fn = Views[view] || Views.home;
            host.innerHTML = await fn(view, u, parts[1]);
            [...host.querySelectorAll('script')].forEach(s => {
                const n = document.createElement('script');
                n.textContent = s.textContent;
                s.replaceWith(n);
            });
            window.scrollTo(0, 0);
        } catch (e) {
            host.innerHTML = `<div class="card"><h3>Something went wrong</h3><p class="muted">${escapeHtml(e.message)}</p></div>`;
        }
    }
};

function buildNav(u) {
    const nav = document.getElementById('nav');
    nav.innerHTML = NAV[u.role]?.map(([href, label]) => {
        const active = (location.hash.split('?')[0] === href.split('?')[0]) ? ' class="active"' : '';
        return `<a href="${href}"${active}>${label}</a>`;
    }).join('') || '';
    const chip = document.getElementById('user-chip');
    chip.innerHTML = `<span class="chip-name">${escapeHtml(u.fullName)}</span>
        <span class="chip-role">${escapeHtml(u.role)}</span>
        <button class="btn-mini" onclick="doLogout()">Logout</button>`;
}

window.addEventListener('hashchange', () => router.route());

async function boot() {
    const u = await getUser();
    if (!u) return;
    document.body.classList.add('g');
    router.route();
}

function doLogout() {
    ace.clear();
    location.href = 'index.html';
}

// ---------------- GLOBAL ACTION HANDLERS ----------------

async function submitApp(id) {
    if (await act(`/loan-applications/${id}/submit`, 'POST', {}, 'Submitted for review')) router.go('#/app-detail/' + id);
}
async function cancelApp(id) {
    if (confirm('Withdraw this application?')) {
        if (await act(`/loan-applications/${id}/cancel`, 'POST', {}, 'Withdrawn')) router.route();
    }
}
async function startReview(id) {
    if (await act(`/loan-applications/${id}/start-review`, 'POST', {}, 'Review started')) router.go('#/app-detail/' + id);
}

function recommend(id, rec) {
    const comment = prompt('Officer notes (shown to manager):') || '';
    if (comment === null) return;
    act(`/loan-applications/${id}/recommend`, 'POST', { recommendation: rec, comment }, 'Recommendation recorded')
        .then(ok => { if (ok) router.route(); });
}

function decideApp(id, kind) {
    const comment = prompt(kind === 'approve' ? 'Approval comment' : 'Rejection reason') || '';
    if (comment === null) return;
    act(`/loan-applications/${id}/${kind}`, 'POST', { comment }, kind === 'approve' ? 'Approved' : 'Rejected')
        .then(ok => { if (ok) router.go('#/app-detail/' + id); });
}

function disburseApp(id, principal) {
    modal('Disburse loan', `
        <form id="disburse-form">
          <label>Disbursed amount (₹)<input name="amount" type="number" step="0.01" value="${principal}"></label>
          <label>Disbursement date<input name="date" type="date"></label>
        </form>`,
        `<button class="btn btn-ghost" onclick="closeModal()">Cancel</button>
         <button class="btn btn-primary" onclick="doDisburse(${id})">Disburse</button>`);
}

async function doDisburse(id) {
    const f = new FormData(document.getElementById('disburse-form'));
    const body = {};
    if (f.get('amount')) body.disbursedAmount = +f.get('amount');
    if (f.get('date')) body.disbursementDate = f.get('date');
    if (await act('/loans/disburse/' + id, 'POST', body, 'Loan disbursed')) { closeModal(); router.go('#/staff-loans'); }
}

function editDraft(id) {
    router.go('#/app-detail-edit/' + id);
}

function payModal(loanId) {
    modal('Record payment', `
        <form id="pay-form">
          <label>Amount (₹)<input name="amount" type="number" step="0.01" required placeholder="e.g. 19785.60"></label>
          <div class="row2">
            <label>Mode<select name="mode"><option>UPI</option><option>NEFT</option><option>IMPS</option><option>RTGS</option><option>CASH</option><option>CHEQUE</option></select></label>
            <label>Txn reference<input name="ref" placeholder="optional"></label>
          </div>
          <label>Remarks<input name="remarks" placeholder="optional"></label>
          <p class="hint">Payments settle the next unpaid installment only.</p>
        </form>`,
        `<button class="btn btn-ghost" onclick="closeModal()">Cancel</button>
         <button class="btn btn-primary" onclick="doPay(${loanId})">Record</button>`);
}
async function doPay(loanId) {
    const f = new FormData(document.getElementById('pay-form'));
    const body = { amount: +f.get('amount'), paymentMode: f.get('mode') };
    if (f.get('ref')) body.transactionRef = f.get('ref');
    if (f.get('remarks')) body.remarks = f.get('remarks');
    if (await act(`/loans/${loanId}/repayments`, 'POST', body, 'Payment recorded')) { closeModal(); router.route(); }
}

async function verifyKyc(id) {
    if (await act(`/customers/${id}/kyc/verify`, 'POST', {}, 'KYC verified')) router.route();
}
async function toggleCustomer(id, current) {
    const to = current === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    if (await act(`/customers/${id}/status`, 'PATCH', { status: to }, 'Updated')) router.route();
}
async function toggleEmployee(id, current) {
    const to = current === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    if (await act(`/employees/${id}/status`, 'PATCH', { status: to }, 'Updated')) router.route();
}

function employeeCreate() {
    modal('Provision staff account', `
        <form id="emp-form">
          <div class="row2"><label>Username<input name="username" required minlength="3"></label>
          <label>Email<input name="email" type="email" required></label></div>
          <div class="row2"><label>Full name<input name="fullName" required></label>
          <label>Phone<input name="phone" required></label></div>
          <div class="row2"><label>Role<select name="role"><option>LOAN_OFFICER</option><option>MANAGER</option></select></label>
          <label>Employee code<input name="employeeCode" required></label></div>
          <div class="row2"><label>Designation<input name="designation"></label>
          <label>Department<input name="department"></label></div>
          <div class="row2"><label>Joining date<input name="joiningDate" type="date"></label>
          <label>Password<input name="password" type="password" required placeholder="8+ chars, mixed case + digit + symbol"></label></div>
        </form>`,
        `<button class="btn btn-ghost" onclick="closeModal()">Cancel</button>
         <button class="btn btn-primary" onclick="doEmployeeCreate()">Create</button>`);
}
async function doEmployeeCreate() {
    const f = new FormData(document.getElementById('emp-form'));
    const body = { username: f.get('username'), email: f.get('email'), password: f.get('password'),
        fullName: f.get('fullName'), phone: f.get('phone'), role: f.get('role'), employeeCode: f.get('employeeCode'),
        designation: f.get('designation') || undefined, department: f.get('department') || undefined,
        joiningDate: f.get('joiningDate') || undefined };
    if (await act('/employees', 'POST', body, 'Staff provisioned')) { closeModal(); router.go('#/employees'); }
}

function employeeEdit(id) {
    router.go('#/employee-edit/' + id);
}

router.go('#/home');
window.addEventListener('DOMContentLoaded', boot);
window.addEventListener('error', e => console.error(e.message));