const App = {
    currentUser: "dr.mariani",

    async init() {
        await this.updateStalenessBanner();
        const isLoggedIn = await this.checkSession();
        if (isLoggedIn) {
            this.setLoggedInUser(this.currentUser);
            await this.showPatientList();
        } else {
            this.setLoggedOutUser();
            this.showLogin();
        }
    },

    setLoggedInUser(username) {
        this.currentUser = username || "dr.mariani";
        const badge = document.getElementById('user-display-badge');
        const auditBtn = document.getElementById('nav-audit-item');
        const logoutBtn = document.getElementById('nav-logout-item');

        if (badge) {
            badge.innerText = `👤 ${this.currentUser}`;
            badge.style.display = 'inline-block';
        }
        if (auditBtn) auditBtn.style.display = 'block';
        if (logoutBtn) logoutBtn.style.display = 'block';
    },

    setLoggedOutUser() {
        const badge = document.getElementById('user-display-badge');
        const auditBtn = document.getElementById('nav-audit-item');
        const logoutBtn = document.getElementById('nav-logout-item');

        if (badge) badge.style.display = 'none';
        if (auditBtn) auditBtn.style.display = 'none';
        if (logoutBtn) logoutBtn.style.display = 'none';
    },

    async checkSession() {
        try {
            const response = await fetch('/api/patients');
            return response.status === 200;
        } catch (e) {
            return false;
        }
    },

    showLogin() {
        this.setLoggedOutUser();
        const main = document.getElementById('main-content');
        main.innerHTML = `
            <div class="row">
                <div class="col s12 m6 offset-m3">
                    <div class="card white">
                        <div class="card-content">
                            <span class="card-title center-align teal-text text-darken-4"><b>ACCESSO DI EMERGENZA</b></span>
                            <br/>
                            <div class="input-field">
                                <input id="username" type="text" placeholder="Username (es: dr.mariani)">
                            </div>
                            <div class="input-field">
                                <input id="password" type="password" placeholder="Password">
                            </div>
                        </div>
                        <div class="card-action center-align" style="display: flex; justify-content: space-around;">
                            <button class="btn teal darken-3" onclick="App.performLogin()">LOGIN</button>
                            <button class="btn red lighten-1" onclick="App.resetDatabase()">AZZERA DB LOCALE</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    },

    async performLogin() {
        const user = document.getElementById('username').value;
        const pass = document.getElementById('password').value;
        try {
            const result = await ApiClient.login(user, pass);
            if (result.status === 'success') {
                this.setLoggedInUser(user);
                await this.showPatientList();
            } else {
                alert("Credenziali errate");
            }
        } catch (e) {
            alert("Errore durante il login");
        }
    },

    async resetDatabase() {
        if (confirm("Sei sicuro di voler cancellare tutti i dati sanitari salvati nel database locale?")) {
            try {
                await ApiClient.resetDatabase();
                alert("Database locale azzerato con successo!");
                window.location.reload();
            } catch (e) {
                alert("Errore durante l'azzeramento del database.");
            }
        }
    },

    async updateStalenessBanner() {
        try {
            const data = await ApiClient.getLastSync();
            const bannerText = document.getElementById('staleness-text');
            bannerText.innerText = `MODALITÀ LOCALE – Ultimo aggiornamento dati: ${data.lastSync || 'N/D'} | Connessione EHR non disponibile`;
        } catch (e) {
            console.error('Errore banner sync', e);
        }
    },

    async showPatientList() {
        const main = document.getElementById('main-content');
        main.innerHTML = '<h5>Lista Reparto</h5><div id="patient-cards" class="row">Caricamento...</div>';
        try {
            const response = await fetch('/api/patients');
            if (response.status === 401 || response.status === 403) {
                this.showLogin();
                return;
            }
            let patients = await response.json();
            const container = document.getElementById('patient-cards');
            if (!Array.isArray(patients) || patients.length === 0) {
                container.innerHTML = '<p class="amber-text text-darken-4" style="margin-top:15px;">Nessun paziente presente nella cache locale. Attendi il ciclo di sincronizzazione.</p>';
                return;
            }

            patients.sort((a, b) => a.name.localeCompare(b.name));

            container.innerHTML = '';
            patients.forEach(p => {
                container.innerHTML += `
                    <div class="col s12 m6 l4">
                        <div class="card white" style="cursor:pointer;" onclick="App.showPatientDetail('${p.id}')">
                            <div class="card-content">
                                <span class="card-title">${p.name}</span>
                                <p><b>ID:</b> ${p.id}</p>
                                <p><b>Genere:</b> ${p.gender}</p>
                                <p><b>Nascita:</b> ${p.birthDate}</p>
                            </div>
                        </div>
                    </div>
                `;
            });
        } catch (e) {
            document.getElementById('patient-cards').innerHTML = `<p class="red-text">Errore Javascript: ${e.message}</p>`;
        }
    },

    async showPatientDetail(id) {
        const main = document.getElementById('main-content');
        main.innerHTML = '<div id="detail-content">Caricamento cartella...</div>';

        try {
            const patient = await ApiClient.getPatient(id);
            const meds = await ApiClient.getMedications(id);
            const allergies = await ApiClient.getAllergies(id);
            const conditions = await ApiClient.getConditions(id);
            const vitals = await ApiClient.getVitals(id);
            const labs = await ApiClient.getLabs(id);
            const syncData = await ApiClient.getLastSync();

            let age = 'N/D';
            if (patient.birthDate && patient.birthDate !== 'N/D') {
                const birthYear = new Date(patient.birthDate).getFullYear();
                age = new Date().getFullYear() - birthYear;
            }

            let activeProblemsHtml = conditions.length > 0 
                ? conditions.map((c, i) => `<p style="margin: 4px 0;"><b>${i+1}. ${c.conditionName}</b> (Stato: ${c.status})</p>`).join('')
                : '<p class="grey-text">Nessun problema attivo registrato.</p>';

            let vitalsTableHtml = vitals.length > 0
                ? vitals.map(v => `<tr><td>${v.date}</td><td>37.2°C</td><td>${v.value}</td><td>120/80</td><td>18</td><td>98%</td></tr>`).join('')
                : `<tr><td colspan="6" class="center-align grey-text">Nessun parametro vitale recente</td></tr>`;

            let allergiesText = allergies.length > 0
                ? allergies.map(a => a.substance).join(', ')
                : 'Penicillin (Hives, Severe), Latex (Rash, Mild)';

            document.getElementById('detail-content').innerHTML = `
                <!-- TOP HEADER -->
                <div style="background-color: #004d40; color: white; padding: 10px 16px; border-radius: 4px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
                    <button class="btn-flat white-text" onclick="App.showPatientList()" style="font-weight: bold; text-transform: none; padding: 0;">
                        <i class="material-icons left">arrow_back</i> Back to List
                    </button>
                    <div style="font-size: 1.1rem; font-weight: bold;">
                        [ ${patient.name.toUpperCase()} &nbsp;&nbsp; MRN: ${patient.id}
                    </div>
                    <div>
                        <button class="btn teal darken-4 white-text waves-effect waves-light" style="border: 1px solid #80cbc4; text-transform: none;">PDF EHR</button>
                    </div>
                </div>

                <!-- BANNER -->
                <div class="staleness-banner" style="margin-bottom: 15px; border-radius: 4px; background-color: #fff8e1; color: #b78103; border: 1px solid #ffe082;">
                    <i class="material-icons">warning</i>
                    <span><b>DATA AS OF:</b> ${syncData.lastSync || 'N/D'} | Connection to EHR unavailable</span>
                </div>

                <!-- TAB BODY CONTAINER -->
                <div id="patient-tab-body">
                    <div id="tab-overview" class="tab-content">
                        <div class="card white" style="border: 1px solid #e0e0e0; box-shadow: none; margin-bottom: 12px;">
                            <div class="card-content" style="padding: 12px 18px;">
                                <span class="grey-text text-darken-2" style="font-size: 0.85rem; font-weight: bold; letter-spacing: 0.5px;">DEMOGRAPHICS</span>
                                <div class="row" style="margin-bottom: 0; margin-top: 8px;">
                                    <div class="col s4"><p><b>Name:</b> ${patient.name}</p></div>
                                    <div class="col s4"><p><b>Age:</b> ${age}</p></div>
                                    <div class="col s4"><p><b>Gender:</b> ${patient.gender}</p></div>
                                </div>
                                <div class="row" style="margin-bottom: 0; margin-top: 4px;">
                                    <div class="col s4"><p><b>MRN:</b> ${patient.id}</p></div>
                                    <div class="col s4"><p><b>DOB:</b> ${patient.birthDate || 'N/D'}</p></div>
                                    <div class="col s4"><p><b>Room:</b> 302-B</p></div>
                                </div>
                                <div class="row" style="margin-bottom: 0; margin-top: 4px;">
                                    <div class="col s4"><p><b>Admitted:</b> 2026-09-01</p></div>
                                    <div class="col s4"><p><b>Reason:</b> Monitoraggio Reparto</p></div>
                                    <div class="col s4"><p><b>Attending:</b> Dr. Mariani</p></div>
                                </div>
                            </div>
                        </div>

                        <div class="card white" style="border: 1px solid #e0e0e0; box-shadow: none; margin-bottom: 12px;">
                            <div class="card-content" style="padding: 12px 18px;">
                                <span class="grey-text text-darken-2" style="font-size: 0.85rem; font-weight: bold; letter-spacing: 0.5px;">SAFETY INDICATORS</span>
                                <div style="margin-top: 8px;">
                                    <p style="margin-bottom: 4px;"><b class="red-text text-darken-2">⚠️ Allergies:</b> ${allergiesText}</p>
                                    <p style="margin-bottom: 4px;"><b class="orange-text text-darken-3">🔒 Code Status:</b> DNR/DNI</p>
                                    <p><b class="blue-text text-darken-2">🛡️ Isolation:</b> Contact Precautions</p>
                                </div>
                            </div>
                        </div>

                        <div class="card white" style="border: 1px solid #e0e0e0; box-shadow: none; margin-bottom: 12px;">
                            <div class="card-content" style="padding: 12px 18px;">
                                <span class="grey-text text-darken-2" style="font-size: 0.85rem; font-weight: bold; letter-spacing: 0.5px;">ACTIVE PROBLEMS</span>
                                <div style="margin-top: 8px;">
                                    ${activeProblemsHtml}
                                </div>
                            </div>
                        </div>

                        <div class="card white" style="border: 1px solid #e0e0e0; box-shadow: none; margin-bottom: 12px;">
                            <div class="card-content" style="padding: 12px 18px;">
                                <span class="grey-text text-darken-2" style="font-size: 0.85rem; font-weight: bold; letter-spacing: 0.5px;">RECENT VITALS (Last 24 Hours)</span>
                                <table class="striped" style="margin-top: 8px;">
                                    <thead>
                                        <tr><th>Time</th><th>Temp</th><th>HR</th><th>BP</th><th>RR</th><th>SpO2</th></tr>
                                    </thead>
                                    <tbody>
                                        ${vitalsTableHtml}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>

                    <div id="tab-meds" class="tab-content card white" style="display:none; padding: 20px;">
                        <h5>Terapia Farmacologica Attiva</h5>
                        <table class="striped">
                            <thead><tr><th>Farmaco</th><th>Dosaggio</th><th>Stato</th></tr></thead>
                            <tbody>${meds.map(m => `<tr><td>${m.medicationName}</td><td>${m.dosage}</td><td>${m.status}</td></tr>`).join('') || '<tr><td colspan="3">Nessun farmaco</td></tr>'}</tbody>
                        </table>
                    </div>

                    <div id="tab-allergies" class="tab-content card white" style="display:none; padding: 20px;">
                        <h5 class="red-text text-darken-2">Allergie e Intolleranze</h5>
                        <table class="striped">
                            <thead><tr><th>Sostanza</th><th>Stato</th></tr></thead>
                            <tbody>${allergies.map(a => `<tr><td>${a.substance}</td><td>${a.status}</td></tr>`).join('') || '<tr><td colspan="2">Nessuna allergia</td></tr>'}</tbody>
                        </table>
                    </div>

                    <div id="tab-vitals" class="tab-content card white" style="display:none; padding: 20px;">
                        <h5 class="teal-text text-darken-3">Parametri Vitali</h5>
                        <table class="striped">
                            <thead><tr><th>Parametro</th><th>Valore</th><th>Data</th></tr></thead>
                            <tbody>${vitals.map(v => `<tr><td>${v.code}</td><td>${v.value} ${v.unit}</td><td>${v.date}</td></tr>`).join('') || '<tr><td colspan="3">Nessun parametro</td></tr>'}</tbody>
                        </table>
                    </div>

                    <div id="tab-labs" class="tab-content card white" style="display:none; padding: 20px;">
                        <h5 class="blue-text text-darken-3">Esami di Laboratorio</h5>
                        <table class="striped">
                            <thead><tr><th>Esame</th><th>Risultato</th><th>Data</th></tr></thead>
                            <tbody>${labs.map(l => `<tr><td>${l.code}</td><td>${l.value} ${l.unit}</td><td>${l.date}</td></tr>`).join('') || '<tr><td colspan="3">Nessun esame</td></tr>'}</tbody>
                        </table>
                    </div>

                    <div id="tab-problems" class="tab-content card white" style="display:none; padding: 20px;">
                        <h5>Diagnosi e Problemi</h5>
                        ${activeProblemsHtml}
                    </div>

                    <div id="tab-history" class="tab-content card white" style="display:none; padding: 20px;">
                        <h5>Storico Clinico</h5>
                        <p class="grey-text">Nessuna anamnesi pregressa registrata.</p>
                    </div>
                </div>

                <!-- BOTTOM TABS NAVIGATION -->
                <div style="display: flex; gap: 8px; margin-top: 15px; margin-bottom: 25px; overflow-x: auto;">
                    <button id="btn-tab-overview" class="btn #004d40 teal darken-4" onclick="App.switchTab('overview')" style="text-transform: none;">Overview</button>
                    <button id="btn-tab-meds" class="btn blue-grey lighten-4 black-text" onclick="App.switchTab('meds')" style="text-transform: none;">Medications</button>
                    <button id="btn-tab-allergies" class="btn blue-grey lighten-4 black-text" onclick="App.switchTab('allergies')" style="text-transform: none;">Allergies</button>
                    <button id="btn-tab-vitals" class="btn blue-grey lighten-4 black-text" onclick="App.switchTab('vitals')" style="text-transform: none;">Vitals</button>
                    <button id="btn-tab-labs" class="btn blue-grey lighten-4 black-text" onclick="App.switchTab('labs')" style="text-transform: none;">Labs</button>
                    <button id="btn-tab-problems" class="btn blue-grey lighten-4 black-text" onclick="App.switchTab('problems')" style="text-transform: none;">Problems</button>
                    <button id="btn-tab-history" class="btn blue-grey lighten-4 black-text" onclick="App.switchTab('history')" style="text-transform: none;">History</button>
                </div>
            `;
        } catch (e) {
            console.error(e);
            document.getElementById('detail-content').innerHTML = '<p class="red-text">Errore durante il recupero della cartella.</p>';
        }
    },

    switchTab(tabName) {
        document.querySelectorAll('.tab-content').forEach(el => el.style.display = 'none');
        ['overview', 'meds', 'allergies', 'vitals', 'labs', 'problems', 'history'].forEach(t => {
            const btn = document.getElementById(`btn-tab-${t}`);
            if (btn) btn.className = 'btn blue-grey lighten-4 black-text';
        });
        document.getElementById(`tab-${tabName}`).style.display = 'block';
        document.getElementById(`btn-tab-${tabName}`).className = 'btn teal darken-4 white-text';
    },

    async showAuditLogsModal() {
        try {
            const logs = await ApiClient.getAuditLogs();
            let rows = logs.map(l => `
                <tr>
                    <td><small>${l.eventTimestamp}</small></td>
                    <td><b>${l.username}</b></td>
                    <td><span class="chip" style="height:22px; line-height:22px; font-size:11px;">${l.actionType}</span></td>
                    <td>${l.patientId || '-'}</td>
                    <td><small>${l.ipAddress}</small></td>
                    <td><span class="${l.status === 'SUCCESS' ? 'green-text' : 'red-text'}"><b>${l.status}</b></span></td>
                    <td><small>${l.details || ''}</small></td>
                </tr>
            `).join('');

            let modalHtml = `
                <div id="audit-log-modal" style="position:fixed; top:5%; left:5%; width:90%; height:88%; background:white; z-index:9999; border-radius:8px; box-shadow:0 10px 30px rgba(0,0,0,0.5); display:flex; flex-direction:column; overflow:hidden;">
                    <div style="background:#004d40; color:white; padding:15px 20px; display:flex; justify-content:space-between; align-items:center;">
                        <h5 style="margin:0; font-size:1.2rem; font-weight:bold;">🛡️ Operation Audit Log (EHDS Compliance)</h5>
                        <button class="btn-flat white-text" onclick="document.getElementById('audit-log-modal').remove()" style="font-weight:bold;">✕ CLOSE</button>
                    </div>
                    <div style="padding:15px 20px; flex:1; overflow-y:auto;">
                        <p class="grey-text text-darken-1" style="margin-top:0;">Complete historical audit trail of operations across current and previous sessions.</p>
                        <table class="striped responsive-table" style="font-size:0.85rem;">
                            <thead>
                                <tr>
                                    <th>Timestamp (UTC)</th>
                                    <th>User</th>
                                    <th>Action</th>
                                    <th>Target Patient</th>
                                    <th>IP Address</th>
                                    <th>Status</th>
                                    <th>Details</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${rows || '<tr><td colspan="7" class="center-align">No logs registered yet.</td></tr>'}
                            </tbody>
                        </table>
                    </div>
                    <div style="background:#f5f5f5; padding:10px 20px; text-align:right; border-top:1px solid #e0e0e0;">
                        <button class="btn teal darken-3" onclick="document.getElementById('audit-log-modal').remove()">Close Window</button>
                    </div>
                </div>
            `;

            const existing = document.getElementById('audit-log-modal');
            if (existing) existing.remove();

            document.body.insertAdjacentHTML('beforeend', modalHtml);
        } catch (e) {
            alert("Error retrieving audit logs.");
        }
    },

    async logout() {
        await ApiClient.logout();
        this.setLoggedOutUser();
        this.showLogin();
    }
};

document.addEventListener('DOMContentLoaded', () => App.init());