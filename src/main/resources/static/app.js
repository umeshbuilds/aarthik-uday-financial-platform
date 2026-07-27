// Global Application State Context
let currentUser = null;
let activeClientId = null;
let liveSimInterval = null;
let adminClients = [];

// Chart references to prevent redraw/recreate bugs
let chartAdminAllocation = null;
let chartAdminPartners = null;
let chartClientBreakdown = null;

// Currency Formatter Utility
const formatCurrency = (val) => {
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 2
    }).format(val || 0);
};

// 1. DYNAMIC NAVIGATION VIEW ROUTER
const switchView = (viewId) => {
    // Hide all views
    document.querySelectorAll('.view-pane').forEach(el => el.classList.remove('active'));
    
    // Deactivate all sidebar nav items
    document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));

    // Show selected view
    const viewEl = document.getElementById(`view-${viewId}`);
    if (viewEl) {
        viewEl.classList.add('active');
    }

    // Set corresponding sidebar nav active
    if (viewId === 'admin-dashboard') {
        document.getElementById('nav-admin-dashboard').classList.add('active');
    } else if (viewId === 'admin-clients' || viewId === 'client-details') {
        document.getElementById('nav-admin-clients')?.classList.add('active');
    } else if (viewId === 'client-dashboard') {
        document.getElementById('nav-client-dashboard').classList.add('active');
    }
};

// 2. CHECK SESSION ON INITIAL APP BOOT
const checkSession = async () => {
    try {
        const response = await fetch('/api/auth/session');
        const data = await response.json();

        if (data.authenticated) {
            currentUser = data;
            
            // Set User info in badge
            document.getElementById('user-display-name').textContent = currentUser.fullName;
            document.getElementById('user-display-role').textContent = currentUser.role.replace('ROLE_', '');

            // Configure views and sidebar depending on the role
            document.getElementById('auth-overlay').style.display = 'none';
            document.getElementById('app-container').style.display = 'flex';

            if (currentUser.role === 'ROLE_ADMIN') {
                document.getElementById('nav-admin-dashboard').style.display = 'block';
                document.getElementById('nav-admin-clients').style.display = 'block';
                document.getElementById('nav-client-dashboard').style.display = 'none';
                switchView('admin-dashboard');
                loadAdminDashboard();
            } else if (currentUser.role === 'ROLE_CLIENT') {
                document.getElementById('nav-admin-dashboard').style.display = 'none';
                document.getElementById('nav-admin-clients').style.display = 'none';
                document.getElementById('nav-client-dashboard').style.display = 'block';
                switchView('client-dashboard');
                loadClientDashboard();
            }
        } else {
            // Force show Login Card
            currentUser = null;
            document.getElementById('auth-overlay').style.display = 'flex';
            document.getElementById('app-container').style.display = 'none';
            
            // Clean up intervals if any
            if (liveSimInterval) {
                clearInterval(liveSimInterval);
                liveSimInterval = null;
                resetLiveSimButton();
            }
        }
    } catch (err) {
        console.error('Session check error:', err);
    }
};

// 3. LOGIN FORM HANDLER
const handleLogin = async (event) => {
    event.preventDefault();
    const usernameEl = document.getElementById('login-username');
    const passwordEl = document.getElementById('login-password');
    const errorEl = document.getElementById('auth-error');

    errorEl.style.display = 'none';

    try {
        const params = new URLSearchParams();
        params.append('username', usernameEl.value);
        params.append('password', passwordEl.value);

        const response = await fetch('/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: params
        });

        const data = await response.json();

        if (response.ok && data.success) {
            usernameEl.value = '';
            passwordEl.value = '';
            checkSession();
        } else {
            errorEl.style.display = 'block';
            errorEl.textContent = data.message || 'Invalid username or password.';
        }
    } catch (err) {
        console.error('Login error:', err);
        errorEl.style.display = 'block';
        errorEl.textContent = 'Server connection failed. Try again.';
    }
};

// 4. LOGOUT HANDLER
const handleLogout = async (event) => {
    if (event) event.preventDefault();
    try {
        const response = await fetch('/logout', { method: 'POST' });
        if (response.ok) {
            checkSession();
        }
    } catch (err) {
        console.error('Logout error:', err);
    }
};

// 5. MOCK DATA UPDATES TICKER ENGINE
const startLiveTickerUpdates = () => {
    const tickerEl = document.getElementById('live-ticker');
    if (!tickerEl) return;
    
    // Simulate stock ticker values shifting slightly every 4 seconds in UI
    setInterval(() => {
        const items = tickerEl.querySelectorAll('.ticker-item');
        items.forEach(item => {
            const priceEl = item.querySelector('.ticker-price');
            const statusEl = item.nextSibling || item.querySelector('span:nth-child(2)');
            if (!statusEl) return;
            
            let valText = statusEl.textContent;
            let regex = /₹([\d.]+)/;
            let match = valText.match(regex);
            if (match) {
                let currentVal = parseFloat(match[1]);
                let fluctuation = (Math.random() * 0.01) - 0.0045; // -0.45% to +0.55%
                let newVal = currentVal * (1.0 + fluctuation);
                let sign = fluctuation >= 0 ? '+' : '';
                let pctText = sign + (fluctuation * 100).toFixed(2) + '%';
                
                statusEl.textContent = `₹${newVal.toFixed(2)} (${pctText})`;
                if (fluctuation >= 0) {
                    statusEl.className = 'ticker-up';
                } else {
                    statusEl.className = 'ticker-down';
                }
            }
        });
    }, 4000);
};

// 6. LIVE SYNC FEED CONTROL (ADMIN ONLY)
const toggleLiveSim = () => {
    const button = document.getElementById('btn-live-toggle');
    const textEl = document.getElementById('live-toggle-text');
    if (!button || !textEl) return;

    if (liveSimInterval) {
        // Stop simulation
        clearInterval(liveSimInterval);
        liveSimInterval = null;
        resetLiveSimButton();
    } else {
        // Start simulation executing every 3.5 seconds
        liveSimInterval = setInterval(async () => {
            try {
                const response = await fetch('/api/admin/investments/simulate-live', {
                    method: 'POST'
                });
                if (response.ok) {
                    // Soft reload stats in place
                    if (activeClientId) {
                        loadClientDetail(activeClientId);
                    } else {
                        loadAdminDashboard();
                    }
                }
            } catch (err) {
                console.error('Live Sync simulation error:', err);
            }
        }, 3500);

        button.style.borderColor = 'var(--accent-red)';
        button.style.color = 'var(--accent-red)';
        button.className = 'btn btn-secondary'; // Remove pulse animation while ticking
        textEl.textContent = 'Pause Live Market Feed';
    }
};

const resetLiveSimButton = () => {
    const button = document.getElementById('btn-live-toggle');
    const textEl = document.getElementById('live-toggle-text');
    if (button && textEl) {
        button.style.borderColor = 'var(--accent-emerald)';
        button.style.color = 'var(--accent-emerald)';
        button.className = 'btn btn-secondary btn-pulse';
        textEl.textContent = 'Start Live Market Feed';
    }
};

// 7. ADMIN OVERVIEW LOADER
const loadAdminDashboard = async () => {
    try {
        // Clear detail view client reference
        activeClientId = null;

        // Fetch Dashboard Stats API
        const statsRes = await fetch('/api/dashboard/stats');
        const stats = await statsRes.json();

        // Populate Cards
        document.getElementById('admin-stat-aum').textContent = formatCurrency(stats.totalAum);
        document.getElementById('admin-stat-invested').textContent = formatCurrency(stats.totalInvested);
        document.getElementById('admin-stat-profit').textContent = formatCurrency(stats.totalProfit);
        document.getElementById('admin-stat-clients').textContent = stats.totalClients;

        const yieldPct = stats.totalInvested > 0 
                ? (stats.totalProfit / stats.totalInvested) * 100 
                : 0.00;
        document.getElementById('admin-stat-yield-pct').querySelector('span').textContent = `${yieldPct.toFixed(2)}%`;

        // Render Charts
        renderAdminCharts(stats);

        // Fetch registered clients list
        const clientsRes = await fetch('/api/admin/clients');
        adminClients = await clientsRes.json();
        
        // Render Clients Table
        renderClientTable(adminClients);

    } catch (err) {
        console.error('Error loading Admin Overview:', err);
    }
};

// Render Admin Charts
const renderAdminCharts = (stats) => {
    const ctxAllocation = document.getElementById('chart-admin-allocation').getContext('2d');
    const ctxPartners = document.getElementById('chart-admin-partners').getContext('2d');

    // Chart 1: Product Allocation (SIP, Stocks, SWP, Insurance)
    if (chartAdminAllocation) {
        chartAdminAllocation.destroy();
    }
    
    chartAdminAllocation = new Chart(ctxAllocation, {
        type: 'bar',
        data: {
            labels: ['SIP Mut. Funds', 'SWP Pension', 'LIC Insurance', 'IIFL Equities'],
            datasets: [{
                label: 'Valuation in ₹',
                data: [stats.sipValue, stats.swpValue, stats.insuranceValue, stats.stocksValue],
                backgroundColor: [
                    'rgba(255, 149, 5, 0.65)',   // Orange
                    'rgba(59, 130, 246, 0.65)',  // Blue
                    'rgba(245, 158, 11, 0.65)',  // Amber
                    'rgba(16, 185, 129, 0.65)'   // Emerald
                ],
                borderColor: [
                    '#FF9505', '#3b82f6', '#f59e0b', '#10b981'
                ],
                borderWidth: 1.5,
                borderRadius: 8
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: { backgroundColor: '#13141f', titleColor: '#fff', bodyColor: '#ccc' }
            },
            scales: {
                y: {
                    grid: { color: 'rgba(255, 255, 255, 0.05)' },
                    ticks: { color: '#94a3b8' }
                },
                x: {
                    grid: { display: false },
                    ticks: { color: '#94a3b8' }
                }
            }
        }
    });

    // Chart 2: Broker Splits (NJ Wealth vs IIFL)
    if (chartAdminPartners) {
        chartAdminPartners.destroy();
    }

    chartAdminPartners = new Chart(ctxPartners, {
        type: 'doughnut',
        data: {
            labels: ['NJ Wealth (Mutual Funds)', 'IIFL (Capital Stocks)'],
            datasets: [{
                data: [stats.njWealthValue, stats.iiflValue],
                backgroundColor: [
                    'rgba(255, 149, 5, 0.75)',
                    'rgba(16, 185, 129, 0.75)'
                ],
                borderColor: ['rgba(255, 149, 5, 0.3)', 'rgba(16, 185, 129, 0.3)'],
                borderWidth: 1,
                hoverOffset: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: { color: '#f8fafc', font: { family: 'Inter', size: 11 } }
                },
                tooltip: { backgroundColor: '#13141f' }
            },
            cutout: '70%'
        }
    });
};

// Render Client list in Admin Table
const renderClientTable = (clients) => {
    const tbody = document.getElementById('admin-clients-table-body');
    tbody.innerHTML = '';

    if (clients.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-secondary);">No clients registered. Click 'Add New Client' to start!</td></tr>`;
        return;
    }

    clients.forEach(client => {
        const yieldPct = client.totalInvested > 0 
                ? (client.totalProfit / client.totalInvested) * 100 
                : 0.0;

        const row = document.createElement('tr');
        row.innerHTML = `
            <td>
                <div style="font-weight: 700; color: #fff;">${client.user.fullName}</div>
                <div style="font-size: 0.75rem; color: var(--text-secondary);">@${client.user.username}</div>
            </td>
            <td>
                <div>${client.user.email}</div>
                <div style="font-size: 0.8rem; color: var(--text-secondary);">${client.phoneNumber}</div>
            </td>
            <td><code class="badge badge-purple">${client.panNumber}</code></td>
            <td>${formatCurrency(client.totalInvested)}</td>
            <td><span style="color: var(--accent-purple); font-weight: 700;">${formatCurrency(client.currentValue)}</span></td>
            <td>
                <span class="badge ${yieldPct >= 0 ? 'badge-emerald' : 'badge-red'}" style="${yieldPct < 0 ? 'background: rgba(239, 68, 68, 0.15); color: #f87171; border: 1px solid rgba(239, 68, 68, 0.3);' : ''}">
                    ${yieldPct >= 0 ? '+' : ''}${yieldPct.toFixed(2)}%
                </span>
            </td>
            <td>
                <button class="btn btn-secondary" style="padding: 0.4rem 0.8rem; font-size: 0.75rem;" onclick="loadClientDetail(${client.id})">
                    <ion-icon name="open-outline"></ion-icon>
                    <span>View Sheet</span>
                </button>
            </td>
        `;
        tbody.appendChild(row);
    });
};

// Filter Clients in Table based on search inputs
const filterClientTable = () => {
    const query = document.getElementById('client-search').value.toLowerCase();
    const filtered = adminClients.filter(c => 
        c.user.fullName.toLowerCase().includes(query) || 
        c.panNumber.toLowerCase().includes(query) ||
        c.user.username.toLowerCase().includes(query)
    );
    renderClientTable(filtered);
};

// 8. CLIENT PERSONAL DASHBOARD LOADER
const loadClientDashboard = async () => {
    try {
        // Fetch personal aggregated statistics
        const statsRes = await fetch('/api/dashboard/stats');
        const stats = await statsRes.json();

        // Populate Cards
        document.getElementById('client-stat-value').textContent = formatCurrency(stats.currentValue);
        document.getElementById('client-stat-invested').textContent = formatCurrency(stats.totalInvested);
        document.getElementById('client-stat-profit').textContent = formatCurrency(stats.totalProfit);

        const yieldPct = stats.totalInvested > 0 
                ? (stats.totalProfit / stats.totalInvested) * 100 
                : 0.00;
        
        const badgeEl = document.getElementById('client-stat-yield-pct');
        badgeEl.querySelector('span').textContent = `${yieldPct >= 0 ? '+' : ''}${yieldPct.toFixed(2)}%`;
        if (yieldPct < 0) {
            badgeEl.style.color = 'var(--accent-red)';
            badgeEl.querySelector('ion-icon').setAttribute('name', 'trending-down-outline');
        } else {
            badgeEl.style.color = 'var(--accent-emerald)';
            badgeEl.querySelector('ion-icon').setAttribute('name', 'trending-up-outline');
        }

        document.getElementById('client-welcome-title').textContent = `Welcome back, ${stats.fullName}!`;

        // Render Client Personal Holdings
        const holdingsRes = await fetch('/api/investments');
        const holdings = await holdingsRes.json();
        
        const tbody = document.getElementById('client-holdings-table-body');
        tbody.innerHTML = '';

        if (holdings.length === 0) {
            tbody.innerHTML = `<tr><td colspan="9" style="text-align: center; color: var(--text-secondary);">You have no investments recorded yet. Let your advisor know!</td></tr>`;
        } else {
            holdings.forEach(inv => {
                const invYield = inv.investedAmount > 0 
                        ? (inv.profitAmount / inv.investedAmount) * 100 
                        : 0.0;
                
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td style="font-weight: 700; color: #fff;">${inv.schemeName}</td>
                    <td><span class="badge ${inv.partner === 'IIFL' ? 'badge-emerald' : 'badge-purple'}">${inv.partner.replace('_', ' ')}</span></td>
                    <td><span class="badge badge-blue">${inv.type}</span></td>
                    <td>${inv.units.toFixed(2)}</td>
                    <td>₹${inv.buyPrice.toFixed(2)}</td>
                    <td><span style="font-weight: 600;">₹${inv.currentPrice.toFixed(2)}</span></td>
                    <td>${formatCurrency(inv.investedAmount)}</td>
                    <td><span style="color: var(--accent-purple); font-weight:700;">${formatCurrency(inv.currentValue)}</span></td>
                    <td style="color: ${invYield >= 0 ? 'var(--accent-emerald)' : 'var(--accent-red)'}; font-weight:600;">
                        ${invYield >= 0 ? '+' : ''}${invYield.toFixed(2)}%
                    </td>
                `;
                tbody.appendChild(row);
            });
        }

        // Render Client Personal Allocation Pie Chart
        renderClientChart(stats);

        // Render Client Recent Transactions Audit list
        const txRes = await fetch('/api/transactions');
        const transactions = await txRes.json();
        
        const txList = document.getElementById('client-tx-list');
        txList.innerHTML = '';

        if (transactions.length === 0) {
            txList.innerHTML = `<li style="font-size: 0.8rem; color: var(--text-secondary); text-align: center;">No history logs recorded yet.</li>`;
        } else {
            transactions.slice(0, 5).forEach(tx => {
                const li = document.createElement('li');
                li.style.display = 'flex';
                li.style.justifyContent = 'space-between';
                li.style.fontSize = '0.8rem';
                li.style.borderBottom = '1px solid rgba(255,255,255,0.03)';
                li.style.paddingBottom = '0.2rem';
                
                const formattedDate = new Date(tx.transactionDate).toLocaleDateString('en-IN', {
                    day: 'numeric', month: 'short'
                });
                
                li.innerHTML = `
                    <div>
                        <span style="font-weight:700; color: ${tx.type === 'BUY' ? 'var(--accent-emerald)' : 'var(--accent-purple)'}">${tx.type}</span>
                        <span style="color: var(--text-secondary); font-size:0.7rem;">(${formattedDate})</span>
                    </div>
                    <span style="font-weight: 600; color: #fff;">${formatCurrency(tx.amount)}</span>
                `;
                txList.appendChild(li);
            });
        }

    } catch (err) {
        console.error('Error loading Client Dashboard:', err);
    }
};

// Render Client Allocation Breakdown Chart
const renderClientChart = (stats) => {
    const ctx = document.getElementById('chart-client-breakdown').getContext('2d');
    
    if (chartClientBreakdown) {
        chartClientBreakdown.destroy();
    }

    chartClientBreakdown = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['SIP', 'SWP', 'Insurance', 'Stocks'],
            datasets: [{
                data: [stats.sipValue, stats.swpValue, stats.insuranceValue, stats.stocksValue],
                backgroundColor: [
                    '#FF9505', '#3b82f6', '#f59e0b', '#10b981'
                ],
                borderColor: 'rgba(255, 255, 255, 0.05)',
                borderWidth: 1.5
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'right',
                    labels: { color: '#94a3b8', font: { size: 10 } }
                },
                tooltip: { backgroundColor: '#13141f' }
            },
            cutout: '60%'
        }
    });
};

// 9. CLIENT GRANULAR DETAIL LOADER FOR ADMIN
const loadClientDetail = async (clientId) => {
    try {
        activeClientId = clientId;
        switchView('client-details');

        // Fetch client master information
        const clientRes = await fetch(`/api/admin/clients/${clientId}`);
        const client = await clientRes.json();

        // Set name and header information
        document.getElementById('detail-client-name').textContent = client.user.fullName;
        document.getElementById('detail-phone').textContent = client.phoneNumber;
        document.getElementById('detail-email').textContent = client.user.email;
        document.getElementById('detail-pan').textContent = client.panNumber;
        document.getElementById('detail-address').textContent = client.address || 'N/A';

        // Set portfolio value metrics cards
        document.getElementById('detail-total-value').textContent = formatCurrency(client.currentValue);
        document.getElementById('detail-total-invested').textContent = formatCurrency(client.totalInvested);
        document.getElementById('detail-total-profit').textContent = formatCurrency(client.totalProfit);

        if (client.totalProfit < 0) {
            document.getElementById('detail-total-profit').style.color = 'var(--accent-red)';
        } else {
            document.getElementById('detail-total-profit').style.color = 'var(--accent-emerald)';
        }

        // Set Client ID on add investment modal hidden input
        document.getElementById('inv-client-id').value = clientId;

        // Fetch holdings
        const holdingsRes = await fetch(`/api/admin/clients/${clientId}/investments`);
        const holdings = await holdingsRes.json();

        const tbody = document.getElementById('detail-holdings-table-body');
        tbody.innerHTML = '';

        if (holdings.length === 0) {
            tbody.innerHTML = `<tr><td colspan="9" style="text-align: center; color: var(--text-secondary);">No investments recorded for this client yet. Click 'Record Purchase' to start!</td></tr>`;
        } else {
            holdings.forEach(inv => {
                const invYield = inv.investedAmount > 0 
                        ? (inv.profitAmount / inv.investedAmount) * 100 
                        : 0.0;
                
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td style="font-weight: 700; color: #fff;">${inv.schemeName}</td>
                    <td><span class="badge ${inv.partner === 'IIFL' ? 'badge-emerald' : 'badge-purple'}">${inv.partner.replace('_', ' ')}</span></td>
                    <td><span class="badge badge-blue">${inv.type}</span></td>
                    <td>${inv.units.toFixed(2)}</td>
                    <td>₹${inv.buyPrice.toFixed(2)}</td>
                    <td><span style="font-weight:600;">₹${inv.currentPrice.toFixed(2)}</span></td>
                    <td>${formatCurrency(inv.investedAmount)}</td>
                    <td><span style="color: var(--accent-purple); font-weight:700;">${formatCurrency(inv.currentValue)}</span></td>
                    <td style="color: ${invYield >= 0 ? 'var(--accent-emerald)' : 'var(--accent-red)'}; font-weight:600;">
                        ${invYield >= 0 ? '+' : ''}${invYield.toFixed(2)}%
                    </td>
                `;
                tbody.appendChild(row);
            });
        }

        // Fetch transactions history log
        const txRes = await fetch(`/api/admin/clients/${clientId}/transactions`);
        const transactions = await txRes.json();

        const txTbody = document.getElementById('detail-transactions-table-body');
        txTbody.innerHTML = '';

        if (transactions.length === 0) {
            txTbody.innerHTML = `<tr><td colspan="3" style="text-align: center; color: var(--text-secondary);">No logs recorded.</td></tr>`;
        } else {
            transactions.forEach(tx => {
                const formattedDate = new Date(tx.transactionDate).toLocaleDateString('en-IN', {
                    day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit'
                });
                
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td style="font-size:0.75rem; color: var(--text-secondary);">${formattedDate}</td>
                    <td><span class="badge" style="background: ${tx.type === 'BUY' ? 'rgba(16,185,129,0.1)' : 'rgba(139,92,246,0.1)'}; color: ${tx.type === 'BUY' ? 'var(--accent-emerald)' : 'var(--accent-purple)'}">${tx.type}</span></td>
                    <td style="font-weight:600;">${formatCurrency(tx.amount)}</td>
                `;
                txTbody.appendChild(row);
            });
        }

    } catch (err) {
        console.error('Error loading Client detail profile sheet:', err);
    }
};

// 9b. ADMIN CLIENTS DEDICATED VIEW LOADER
const loadAdminClientsView = async () => {
    try {
        const clientsRes = await fetch('/api/admin/clients');
        adminClients = await clientsRes.json();
        renderClientsViewTable(adminClients);
    } catch (err) {
        console.error('Error loading Admin Clients view:', err);
    }
};

const renderClientsViewTable = (clients) => {
    const tbody = document.getElementById('clients-view-table-body');
    if (!tbody) return;
    tbody.innerHTML = '';

    if (clients.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-secondary);">No clients found.</td></tr>`;
        return;
    }

    clients.forEach(client => {
        const yieldPct = client.totalInvested > 0 
                ? (client.totalProfit / client.totalInvested) * 100 
                : 0.0;

        const row = document.createElement('tr');
        row.innerHTML = `
            <td>
                <div style="font-weight: 700; color: #fff;">${client.user.fullName}</div>
                <div style="font-size: 0.75rem; color: var(--text-secondary);">@${client.user.username}</div>
            </td>
            <td>
                <div>${client.user.email}</div>
                <div style="font-size: 0.8rem; color: var(--text-secondary);">${client.phoneNumber}</div>
            </td>
            <td><code class="badge badge-purple">${client.panNumber}</code></td>
            <td>${formatCurrency(client.totalInvested)}</td>
            <td><span style="color: var(--accent-purple); font-weight: 700;">${formatCurrency(client.currentValue)}</span></td>
            <td>
                <span class="badge ${yieldPct >= 0 ? 'badge-emerald' : 'badge-red'}" style="${yieldPct < 0 ? 'background: rgba(239, 68, 68, 0.15); color: #f87171; border: 1px solid rgba(239, 68, 68, 0.3);' : ''}">
                    ${yieldPct >= 0 ? '+' : ''}${yieldPct.toFixed(2)}%
                </span>
            </td>
            <td>
                <button class="btn btn-secondary" style="padding: 0.4rem 0.8rem; font-size: 0.75rem;" onclick="loadClientDetail(${client.id})">
                    <ion-icon name="open-outline"></ion-icon>
                    <span>View Sheet</span>
                </button>
            </td>
        `;
        tbody.appendChild(row);
    });
};

const filterClientsViewTable = () => {
    const query = document.getElementById('clients-view-search').value.toLowerCase();
    const filtered = adminClients.filter(c => 
        c.user.fullName.toLowerCase().includes(query) || 
        c.panNumber.toLowerCase().includes(query) ||
        c.user.username.toLowerCase().includes(query)
    );
    renderClientsViewTable(filtered);
};

// 10. FORM SUBMISSIONS: CREATE NEW CLIENT ACCOUNT
const submitNewClient = async (event) => {
    event.preventDefault();
    const username = document.getElementById('client-username').value;
    const password = document.getElementById('client-password').value;
    const fullName = document.getElementById('client-fullName').value;
    const email = document.getElementById('client-email').value;
    const phoneNumber = document.getElementById('client-phoneNumber').value;
    const panNumber = document.getElementById('client-panNumber').value;
    const address = document.getElementById('client-address').value;
    const errorEl = document.getElementById('create-client-error');

    errorEl.style.display = 'none';

    try {
        const response = await fetch('/api/admin/clients', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password, fullName, email, phoneNumber, panNumber, address })
        });

        if (response.ok) {
            // Success
            document.getElementById('form-create-client').reset();
            closeModal('modal-create-client');
            loadAdminDashboard();
        } else {
            const data = await response.json();
            errorEl.style.display = 'block';
            errorEl.textContent = data.message || 'Error occurred during registration.';
        }
    } catch (err) {
        console.error('Error registering client:', err);
        errorEl.style.display = 'block';
        errorEl.textContent = 'Server connection failed.';
    }
};

// 11. FORM SUBMISSIONS: RECORD PURCHASE
const submitNewInvestment = async (event) => {
    event.preventDefault();
    const clientId = document.getElementById('inv-client-id').value;
    const partner = document.getElementById('inv-partner').value;
    const type = document.getElementById('inv-type').value;
    const schemeName = document.getElementById('inv-schemeName').value;
    const units = document.getElementById('inv-units').value;
    const buyPrice = document.getElementById('inv-buyPrice').value;

    try {
        const response = await fetch('/api/admin/investments', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ clientId, partner, type, schemeName, units, buyPrice })
        });

        if (response.ok) {
            document.getElementById('form-add-investment').reset();
            closeModal('modal-add-investment');
            loadClientDetail(clientId);
        } else {
            alert('Failed to log investment. Check the details and try again.');
        }
    } catch (err) {
        console.error('Error recording investment:', err);
    }
};

// 12. UTILITY: ADAPT ASSET SELECTIONS BY BROKER PARTNER
const adaptAssetTypes = () => {
    const partner = document.getElementById('inv-partner').value;
    const typeSelect = document.getElementById('inv-type');
    
    typeSelect.innerHTML = '';

    if (partner === 'NJ_WEALTH') {
        typeSelect.innerHTML = `
            <option value="SIP">Systematic Plan (SIP)</option>
            <option value="SWP">Withdrawal Plan (SWP)</option>
            <option value="INSURANCE">Insurance Plan</option>
        `;
    } else if (partner === 'IIFL') {
        typeSelect.innerHTML = `
            <option value="STOCKS">Equity Shares (Stocks)</option>
        `;
    }
};

// 13. MODALS POPUPS INTERACTION
const openModal = (modalId) => {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('active');
        if (modalId === 'modal-add-investment') {
            adaptAssetTypes(); // Initialize options correctly
        }
    }
};

const closeModal = (modalId) => {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('active');
    }
};

// 14. DUAL TABLE TAB & FILTERING (ADMIN PANEL)
let activeAdminTab = 'clients';
let adminAssets = [];

const toggleAdminTab = (tabId) => {
    activeAdminTab = tabId;
    
    const btnClients = document.getElementById('tab-btn-clients');
    const btnAssets = document.getElementById('tab-btn-assets');
    const contentClients = document.getElementById('tab-content-clients');
    const contentAssets = document.getElementById('tab-content-assets');

    if (tabId === 'clients') {
        btnClients.classList.add('active');
        btnClients.style.color = '#fff';
        btnAssets.classList.remove('active');
        btnAssets.style.color = 'var(--text-secondary)';
        contentClients.style.display = 'block';
        contentAssets.style.display = 'none';
    } else {
        btnAssets.classList.add('active');
        btnAssets.style.color = '#fff';
        btnClients.classList.remove('active');
        btnClients.style.color = 'var(--text-secondary)';
        contentClients.style.display = 'none';
        contentAssets.style.display = 'block';
        loadMasterHoldings();
    }
};

const loadMasterHoldings = async () => {
    try {
        const response = await fetch('/api/investments');
        adminAssets = await response.json();
        renderMasterHoldingsTable(adminAssets);
    } catch (err) {
        console.error('Error loading master holdings:', err);
    }
};

const renderMasterHoldingsTable = (assets) => {
    const tbody = document.getElementById('admin-assets-table-body');
    tbody.innerHTML = '';

    if (assets.length === 0) {
        tbody.innerHTML = `<tr><td colspan="10" style="text-align: center; color: var(--text-secondary);">No client holdings logged. Click 'Import Sheets/Excel' or record purchases to populate!</td></tr>`;
        return;
    }

    assets.forEach(inv => {
        const invYield = inv.investedAmount > 0 
                ? (inv.profitAmount / inv.investedAmount) * 100 
                : 0.0;

        const row = document.createElement('tr');
        row.innerHTML = `
            <td>
                <div style="font-weight: 700; color: #fff;">${inv.client.user.fullName}</div>
                <div style="font-size: 0.75rem; color: var(--text-secondary);">PAN: ${inv.client.panNumber}</div>
            </td>
            <td style="font-weight: 600; color: #fff;">${inv.schemeName}</td>
            <td><span class="badge ${inv.partner === 'IIFL' ? 'badge-emerald' : 'badge-purple'}">${inv.partner.replace('_', ' ')}</span></td>
            <td><span class="badge badge-blue">${inv.type}</span></td>
            <td>${inv.units.toFixed(2)}</td>
            <td>₹${inv.buyPrice.toFixed(2)}</td>
            <td><span style="font-weight:600;">₹${inv.currentPrice.toFixed(2)}</span></td>
            <td>${formatCurrency(inv.investedAmount)}</td>
            <td><span style="color: var(--accent-purple); font-weight:700;">${formatCurrency(inv.currentValue)}</span></td>
            <td style="color: ${invYield >= 0 ? 'var(--accent-emerald)' : 'var(--accent-red)'}; font-weight:600;">
                ${invYield >= 0 ? '+' : ''}${invYield.toFixed(2)}%
            </td>
        `;
        tbody.appendChild(row);
    });
};

const filterAdminTables = () => {
    const query = document.getElementById('client-search').value.toLowerCase();
    
    if (activeAdminTab === 'clients') {
        const filtered = adminClients.filter(c => 
            c.user.fullName.toLowerCase().includes(query) || 
            c.panNumber.toLowerCase().includes(query) ||
            c.user.username.toLowerCase().includes(query)
        );
        renderClientTable(filtered);
    } else {
        const filtered = adminAssets.filter(inv => 
            inv.client.user.fullName.toLowerCase().includes(query) || 
            inv.client.panNumber.toLowerCase().includes(query) ||
            inv.schemeName.toLowerCase().includes(query) ||
            inv.partner.toLowerCase().includes(query) ||
            inv.type.toLowerCase().includes(query)
        );
        renderMasterHoldingsTable(filtered);
    }
};

// 15. SPREADSHEETS IMPORT HANDLERS (EXCEL / GOOGLE SHEETS)
let activeImportType = 'excel';

const switchImportForm = (type) => {
    activeImportType = type;
    const btnExcel = document.getElementById('btn-import-type-excel');
    const btnGoogle = document.getElementById('btn-import-type-google');
    const formExcel = document.getElementById('form-import-excel');
    const formGoogle = document.getElementById('form-import-google');
    const messageEl = document.getElementById('import-sheet-message');

    messageEl.style.display = 'none';

    if (type === 'excel') {
        btnExcel.style.opacity = '1';
        btnGoogle.style.opacity = '0.6';
        formExcel.style.display = 'block';
        formGoogle.style.display = 'none';
    } else {
        btnGoogle.style.opacity = '1';
        btnExcel.style.opacity = '0.6';
        formExcel.style.display = 'none';
        formGoogle.style.display = 'block';
    }
};

const updateExcelFileName = (input) => {
    const fileNameEl = document.getElementById('excel-file-name');
    if (input.files && input.files.length > 0) {
        fileNameEl.textContent = `Selected: ${input.files[0].name}`;
    } else {
        fileNameEl.textContent = '';
    }
};

const showImportMessage = (success, text) => {
    const el = document.getElementById('import-sheet-message');
    el.style.display = 'block';
    el.textContent = text;
    if (success) {
        el.style.background = 'rgba(16, 185, 129, 0.15)';
        el.style.color = '#34d399';
        el.style.border = '1px solid rgba(16, 185, 129, 0.3)';
    } else {
        el.style.background = 'rgba(239, 68, 68, 0.15)';
        el.style.color = '#f87171';
        el.style.border = '1px solid rgba(239, 68, 68, 0.3)';
    }
};

const submitExcelImport = async (event) => {
    event.preventDefault();
    const fileInput = document.getElementById('import-excel-file');
    const messageEl = document.getElementById('import-sheet-message');
    
    if (fileInput.files.length === 0) {
        return;
    }

    const formData = new FormData();
    formData.append('file', fileInput.files[0]);

    try {
        showImportMessage(true, 'Importing Excel file... Please wait.');
        const response = await fetch('/api/admin/import/excel', {
            method: 'POST',
            body: formData
        });
        const data = await response.json();

        if (response.ok && data.success) {
            showImportMessage(true, data.message || 'Excel file imported successfully!');
            fileInput.value = '';
            document.getElementById('excel-file-name').textContent = '';
            setTimeout(() => {
                closeModal('modal-import-sheet');
                if (activeAdminTab === 'clients') {
                    loadAdminDashboard();
                } else {
                    loadMasterHoldings();
                }
            }, 1500);
        } else {
            showImportMessage(false, data.message || 'Failed to import Excel file.');
        }
    } catch (err) {
        console.error('Excel import error:', err);
        showImportMessage(false, 'Network connection failed.');
    }
};

const submitGoogleImport = async (event) => {
    event.preventDefault();
    const urlInput = document.getElementById('import-google-url');

    try {
        showImportMessage(true, 'Synchronizing Google Sheet... Please wait.');
        const response = await fetch('/api/admin/import/google-sheets', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ url: urlInput.value })
        });
        const data = await response.json();

        if (response.ok && data.success) {
            showImportMessage(true, data.message || 'Google Sheet synced successfully!');
            urlInput.value = '';
            setTimeout(() => {
                closeModal('modal-import-sheet');
                if (activeAdminTab === 'clients') {
                    loadAdminDashboard();
                } else {
                    loadMasterHoldings();
                }
            }, 1500);
        } else {
            showImportMessage(false, data.message || 'Failed to sync Google Sheet.');
        }
    } catch (err) {
        console.error('Google Sheets import error:', err);
        showImportMessage(false, 'Network connection failed.');
    }
};

// 16. INITIALIZE APP ROOT BINDINGS
document.addEventListener('DOMContentLoaded', () => {
    // Check dynamic authentication session immediately
    checkSession();
    
    // Start ticker simulation
    startLiveTickerUpdates();
});
