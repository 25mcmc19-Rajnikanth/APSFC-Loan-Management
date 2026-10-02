// APSFC Loan Management - API client
// Backend runs on :8080, frontend is served from :5500 (CORS-configured).

const API_BASE = (window.location.hostname === 'localhost')
    ? 'http://localhost:8080/api'
    : 'http://127.0.0.1:8080/api';

const ace = {
    get token() { return localStorage.getItem('apsfc_token') || ''; },
    set token(v) { localStorage.setItem('apsfc_token', v); },
    get user() { try { return JSON.parse(localStorage.getItem('apsfc_user')) || null; } catch { return null; } },
    set user(v) { localStorage.setItem('apsfc_user', JSON.stringify(v)); },

    clear() { localStorage.removeItem('apsfc_token'); localStorage.removeItem('apsfc_user'); },

    async request(path, { method = 'GET', body = null } = {}) {
        const headers = { 'Content-Type': 'application/json' };
        if (this.token) headers['Authorization'] = 'Bearer ' + this.token;

        const res = await fetch(API_BASE + path, {
            method,
            headers,
            body: body !== null ? JSON.stringify(body) : undefined
        });

        let data = null;
        const text = await res.text();
        if (text) { try { data = JSON.parse(text); } catch { data = text; } }

        if (!res.ok) {
            if (res.status === 401 && this.token && path.indexOf('/auth/login') === -1 && path.indexOf('/auth/register') === -1) {
                this.clear();
                if (!location.hash) location.href = 'index.html';
            }
            const msg = (data && data.message)
                ? (data.errorCode === 'VALIDATION_ERROR' && data.fieldErrors
                    ? Object.values(data.fieldErrors).join('; ')
                    : data.message)
                : 'Request failed (' + res.status + ')';
            const err = new Error(msg);
            err.status = res.status;
            err.code = data && data.errorCode;
            throw err;
        }
        return data;
    },

    get(path, params) {
        const qs = params ? '?' + new URLSearchParams(Object.entries(params).filter(([, v]) => v !== '' && v != null)).toString() : '';
        return this.request(path + qs);
    },
    post(path, body = {}) { return this.request(path, { method: 'POST', body }); },
    put(path, body = {}) { return this.request(path, { method: 'PUT', body }); },
    patch(path, body = {}) { return this.request(path, { method: 'PATCH', body }); }
};