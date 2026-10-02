// Shared UI helpers: formatting, DOM, state badges, toasts, modals, tables.

const money = v => '\u20B9 ' + Number(v || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const date = d => d ? String(d).slice(0, 10) : '—';
const datetime = d => d ? String(d).slice(0, 16).replace('T', ' ') : '—';
const pct = v => Number(v || 0).toFixed(2) + '%';

const STATUS_BADGE = {
    DRAFT: 'neutral', SUBMITTED: 'info', UNDER_REVIEW: 'info', APPROVED: 'ok',
    REJECTED: 'bad', CANCELLED: 'bad', DISBURSED: 'accent',
    ACTIVE: 'ok', CLOSED: 'neutral',
    PENDING: 'neutral', PARTIALLY_PAID: 'warn', PAID: 'ok',
    ACTIVE_: 'ok'
};

function badge(text) {
    const cls = STATUS_BADGE[text] || 'neutral';
    return `<span class="badge badge-${cls}">${escapeHtml(text)}</span>`;
}

function escapeHtml(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, c => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    })[c]);
}

function toast(msg, type = 'error') {
    const box = document.getElementById('toasts') || (() => {
        const d = document.createElement('div');
        d.id = 'toasts';
        document.body.appendChild(d);
        return d;
    })();
    const t = document.createElement('div');
    t.className = 'toast toast-' + type;
    t.textContent = msg;
    box.appendChild(t);
    setTimeout(() => t.classList.add('out'), 4200);
    setTimeout(() => t.remove(), 4600);
}

function modal(title, bodyHtml, actionsHtml = '') {
    const host = document.getElementById('modal-host');
    host.innerHTML = `
        <div class="modal-backdrop" onclick="if(event.target===this)closeModal()">
            <div class="modal">
                <div class="modal-head"><h3>${title}</h3><button class="plain" onclick="closeModal()">&times;</button></div>
                <div class="modal-body">${bodyHtml}</div>
                ${actionsHtml ? `<div class="modal-foot">${actionsHtml}</div>` : ''}
            </div>
        </div>`;
}

function closeModal() { document.getElementById('modal-host').innerHTML = ''; }

async function act(url, method, data, okMsg) {
    try {
        if (method === 'POST') await ace.post(url, data);
        else if (method === 'PATCH') await ace.patch(url, data);
        else if (method === 'PUT') await ace.put(url, data);
        toast(okMsg || 'Done', 'ok');
        return true;
    } catch (e) {
        toast(e.message);
        return false;
    }
}

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

function fmtMoney(v) { return money(v); }

function statCard(label, value, sub = '', tone = '') {
    return `<div class="stat ${tone}"><div class="stat-value">${value}</div><div class="stat-label">${label}</div>${sub ? `<div class="stat-sub">${sub}</div>` : ''}</div>`;
}

async function getUser() {
    if (ace.user && ace.user.id) return ace.user;
    try {
        const me = await ace.get('/auth/me');
        ace.user = me;
        return me;
    } catch (e) {
        ace.clear();
        location.href = 'index.html';
        return null;
    }
}