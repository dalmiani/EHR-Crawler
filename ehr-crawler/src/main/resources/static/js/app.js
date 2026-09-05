const App = {
    currentPatientId: null,

    async init() {
        await this.updateStalenessBanner();
        await this.showPatientList();
    },

    async updateStalenessBanner() {
        try {
            const data = await ApiClient.getLastSync();
            const bannerText = document.getElementById('staleness-text');
            bannerText.innerText = `MODALITÀ LOCALE – Ultimo aggiornamento dati: ${data.lastSync} | Connessione EHR non disponibile`;
        } catch (e) {
            console.error('Errore durante il recupero dei dati di sincronizzazione', e);
        }
    },

    async showPatientList() {
        const main = document.getElementById('main-content');
        main.innerHTML = '<h5>Censimento Reparto</h5><div id="patient-cards" class="row">Caricamento...</div>';
        
        try {
            const patients = await ApiClient.getAllPatients();
            const container = document.getElementById('patient-cards');
            container.innerHTML = '';

            patients.forEach(p => {
                container.innerHTML += `
                    <div class="col s12 m6 l4">
                        <div class="card clickable" onclick="App.showPatientDetail('${p.id}')">
                            <div class="card-content">
                                <span class="card-title">${p.name}</span>
                                <p><strong>ID:</strong> ${p.id}</p>
                                <p><strong>Genere:</strong> ${p.gender}</p>
                                <div style="margin-top:10px;">
                                    <span class="safety-chip safety-chip--allergy">Penicillina</span>
                                    <span class="safety-chip safety-chip--code-status">DNR/DNI</span>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
            });
        } catch (e) {
            main.innerHTML = '<p class="red-text">Errore durante il caricamento dei pazienti dal repository locale.</p>';
        }
    },

    async showPatientDetail(id) {
        this.currentPatientId = id;
        const main = document.getElementById('main-content');
        main.innerHTML = '<a class="btn waves-effect waves-light flat" onclick="App.showPatientList()"><i class="material-icons left">arrow_back</i>Torna al Censimento</a><div id="detail-content">Caricamento...</div>';

        try {
            const patient = await ApiClient.getPatient(id);
            const meds = await ApiClient.getMedications(id);

            let medsRows = meds.map(m => `<tr><td>${m.medicationName}</td><td>${m.dosage}</td><td>${m.status}</td></tr>`).join('');

            document.getElementById('detail-content').innerHTML = `
                <h4>${patient.name}</h4>
                <p><strong>ID Paziente:</strong> ${patient.id} | <strong>Genere:</strong> ${patient.gender}</p>
                <hr/>
                <h5>Terapia Farmacologica Attiva</h5>
                <table class="striped">
                    <thead>
                        <tr><th>Farmaco</th><th>Dosaggio</th><th>Stato</th></tr>
                    </thead>
                    <tbody>${medsRows || '<tr><td colspan="3">Nessun farmaco registrato.</td></tr>'}</tbody>
                </table>
            `;
        } catch (e) {
            document.getElementById('detail-content').innerHTML = '<p class="red-text">Errore durante il recupero della cartella del paziente.</p>';
        }
    },

    async logout() {
        await ApiClient.logout();
        alert('Logout effettuato');
    }
};

document.addEventListener('DOMContentLoaded', () => App.init());