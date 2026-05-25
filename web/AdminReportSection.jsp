<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    /* ── Role guard ── */
    Integer _role = (Integer) session.getAttribute("role");
    if (_role == null || _role != 1) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return;
    }
    String _userId = (String) session.getAttribute("USER_ID");
%>

<style>
    /* ── Card shell ── */
    .report-card {
        background: #ffffff;
        border-radius: 12px;
        box-shadow: 0 1px 3px rgba(0,0,0,.10), 0 4px 16px rgba(0,0,0,.06);
        width: 100%;
        margin-top: 2rem;
        overflow: hidden;
    }

    /* ── Card header ── */
    .card-header {
        background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 100%);
        padding: 1.4rem 1.8rem;
        display: flex;
        align-items: center;
        gap: 1rem;
    }
    .card-header svg { flex-shrink: 0; }
    .card-header h2 {
        font-size: 1.25rem;
        font-weight: 700;
        color: #fff;
        margin: 0;
    }
    .card-header p {
        font-size: .8rem;
        color: #bfdbfe;
        margin-top: .2rem;
        margin-bottom: 0;
    }

    /* ── Card body ── */
    .card-body { padding: 1.8rem; }

    /* ── Section label ── */
    .section-label {
        font-size: .7rem;
        font-weight: 700;
        letter-spacing: .08em;
        text-transform: uppercase;
        color: #64748b;
        margin-bottom: .75rem;
    }

    /* ── Report buttons ── */
    .report-group {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
        gap: .9rem;
        margin-bottom: 1.6rem;
    }

    .btn-report {
        display: flex;
        align-items: center;
        gap: .75rem;
        padding: .85rem 1rem;
        border-radius: 10px;
        border: 1.5px solid #e2e8f0;
        background: #f8fafc;
        color: #1e3a8a;
        font-size: .875rem;
        font-weight: 600;
        text-decoration: none;
        cursor: pointer;
        transition: all .18s ease;
        position: relative;
        overflow: hidden;
    }
    .btn-report:hover {
        border-color: #2563eb;
        background: #eff6ff;
        box-shadow: 0 2px 8px rgba(37,99,235,.18);
        transform: translateY(-1px);
    }
    .btn-report svg { flex-shrink: 0; color: #2563eb; }
    .btn-report .btn-label { display: flex; flex-direction: column; text-align: left; }
    .btn-report .btn-label span:first-child { font-size: .875rem; color: #1e293b; }
    .btn-report .btn-label small { font-size: .72rem; font-weight: 400; color: #64748b; margin-top: .15rem; }

    .btn-report.mine svg { color: #16a34a; }
    .btn-report.mine { color: #14532d; }
    .btn-report.mine:hover { border-color: #16a34a; background: #f0fdf4; box-shadow: 0 2px 8px rgba(22,163,74,.18); }

    /* ── Divider ── */
    .divider {
        border: none;
        border-top: 1.5px dashed #e2e8f0;
        margin: 1.4rem 0;
    }

    /* ── Time-bound form ── */
    .timebound-grid {
        display: grid;
        grid-template-columns: 1fr 1fr auto;
        gap: .75rem;
        align-items: end;
    }
    @media (max-width: 540px) {
        .timebound-grid { grid-template-columns: 1fr; }
    }

    .field { display: flex; flex-direction: column; gap: .35rem; }
    .field label { font-size: .78rem; font-weight: 600; color: #475569; margin: 0; text-align: left;}
    .field input[type="date"] {
        padding: .6rem .75rem;
        border-radius: 8px;
        border: 1.5px solid #e2e8f0;
        font-size: .875rem;
        color: #1e293b;
        background: #f8fafc;
        transition: border-color .15s;
        width: 100%;
    }
    .field input[type="date"]:focus {
        outline: none;
        border-color: #2563eb;
        background: #fff;
        box-shadow: 0 0 0 3px rgba(37,99,235,.12);
    }

    .btn-timebound {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: .5rem;
        padding: .65rem 1.2rem;
        background: #1e3a8a;
        color: #fff;
        border: none;
        border-radius: 8px;
        font-size: .875rem;
        font-weight: 600;
        cursor: pointer;
        transition: background .18s, transform .1s;
        text-decoration: none;
        white-space: nowrap;
        height: 38px;
    }
    .btn-timebound:hover { background: #1d4ed8; transform: translateY(-1px); }
    .btn-timebound:active { transform: translateY(0); }

    /* ── Error message ── */
    .msg-error {
        display: none;
        background: #fef2f2;
        border: 1px solid #fca5a5;
        color: #991b1b;
        border-radius: 8px;
        padding: .65rem 1rem;
        font-size: .82rem;
        margin-top: .75rem;
        text-align: left;
    }

    /* ── Spinner overlay ── */
    .spinner-wrap {
        display: none;
        position: fixed; inset: 0;
        background: rgba(15,23,42,.45);
        z-index: 9999;
        align-items: center;
        justify-content: center;
        flex-direction: column;
        gap: 1rem;
    }
    .spinner-wrap.active { display: flex; }
    .spinner {
        width: 48px; height: 48px;
        border: 5px solid rgba(255,255,255,.2);
        border-top-color: #fff;
        border-radius: 50%;
        animation: spin .8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .spinner-wrap p { color: #fff; font-size: .9rem; font-weight: 600; margin: 0; }
</style>

<div class="spinner-wrap" id="spinnerOverlay">
    <div class="spinner"></div>
    <p>Generating PDF, please wait…</p>
</div>

<div class="report-card">
    <div class="card-header">
        <svg width="28" height="28" fill="none" viewBox="0 0 24 24" stroke="white" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414A1 1 0 0119 9.414V19a2 2 0 01-2 2z"/>
        </svg>
        <div>
            <h2>Print System Reports</h2>
            <p>All reports are generated as landscape PDF files and downloaded directly to your device.</p>
        </div>
    </div>

    <div class="card-body">
        <p class="section-label">User Directories</p>
        <div class="report-group">
            <a href="#" class="btn-report" onclick="triggerDownload('<%= request.getContextPath() %>/ReportServlet?reportType=USERLIST'); return false;">
                <svg width="22" height="22" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M17 20h5v-2a4 4 0 00-5.916-3.519M9 20H4v-2a4 4 0 015.916-3.519M15 7a4 4 0 11-8 0 4 4 0 018 0zm6 3a3 3 0 11-6 0 3 3 0 016 0zM3 10a3 3 0 116 0 3 3 0 01-6 0z"/>
                </svg>
                <span class="btn-label">
                    <span>All System Users</span>
                    <small>Full user list and operational roles</small>
                </span>
            </a>
        </div>

        <hr class="divider">

        <p class="section-label">PostgreSQL Telemetry Logs</p>
        <div class="report-group">
            <a href="#" class="btn-report" onclick="triggerDownload('<%= request.getContextPath() %>/ReportServlet?reportType=LOGALL'); return false;">
                <svg width="22" height="22" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414A1 1 0 0119 9.414V19a2 2 0 01-2 2z"/>
                </svg>
                <span class="btn-label">
                    <span>All Activity Logs</span>
                    <small>Every historic record in schema</small>
                </span>
            </a>

            <a href="#" class="btn-report mine" onclick="triggerDownload('<%= request.getContextPath() %>/ReportServlet?reportType=LOGMINE'); return false;">
                <svg width="22" height="22" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
                </svg>
                <span class="btn-label">
                    <span>My Activity Logs</span>
                    <small>Only your specific admin actions</small>
                </span>
            </a>
        </div>

        <hr class="divider">

        <p class="section-label">Time-Bound Telemetry Query</p>
        <div class="timebound-grid">
            <div class="field">
                <label for="dateFrom">From Date</label>
                <input type="date" id="dateFrom" name="from" max="<%= new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()) %>">
            </div>
            <div class="field">
                <label for="dateTo">To Date</label>
                <input type="date" id="dateTo" name="to" max="<%= new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()) %>">
            </div>
            <button class="btn-timebound" onclick="downloadTimeBound()">
                <svg width="18" height="18" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
                </svg>
                Generate PDF
            </button>
        </div>

        <div class="msg-error" id="dateError">
            ⚠ Please select both a valid <strong>From</strong> and <strong>To</strong> date range where the initiation date does not exceed the termination date.
        </div>
    </div>
</div>

<script>
    function triggerDownload(url) {
        showSpinner();
        const a = document.createElement('a');
        a.href = url;
        a.style.display = 'none';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        
        // Hide spinner once browser takes over the payload handle stream
        setTimeout(hideSpinner, 3000);
    }

    function downloadTimeBound() {
        const from = document.getElementById('dateFrom').value;
        const to = document.getElementById('dateTo').value;
        const errEl = document.getElementById('dateError');

        if (!from || !to || from > to) {
            errEl.style.display = 'block';
            return;
        }
        errEl.style.display = 'none';

        const url = '<%= request.getContextPath() %>/ReportServlet'
                  + '?reportType=LOGTIMEBOUND'
                  + '&from=' + encodeURIComponent(from)
                  + '&to='   + encodeURIComponent(to);
        triggerDownload(url);
    }

    function showSpinner() {
        document.getElementById('spinnerOverlay').classList.add('active');
    }
    function hideSpinner() {
        document.getElementById('spinnerOverlay').classList.remove('active');
    }

    document.getElementById('dateFrom').addEventListener('change', () => {
        document.getElementById('dateError').style.display = 'none';
    });
    document.getElementById('dateTo').addEventListener('change', () => {
        document.getElementById('dateError').style.display = 'none';
    });
</script>