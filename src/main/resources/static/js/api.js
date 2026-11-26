const ApiClient = {
    async login(username, password) {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        return response.json();
    },

    async logout() {
        await fetch('/api/auth/logout', { method: 'POST' });
    },

    async getAllPatients() {
        const response = await fetch('/api/patients');
        if (!response.ok) return [];
        return response.json();
    },

    async getPatient(id) {
        const response = await fetch(`/api/patients/${id}`);
        if (!response.ok) throw new Error('Paziente non trovato');
        return response.json();
    },

    async getMedications(id) {
        const response = await fetch(`/api/patients/${id}/medications`);
        if (!response.ok) return [];
        return response.json();
    },

    async getAllergies(id) {
        const response = await fetch(`/api/patients/${id}/allergies`);
        if (!response.ok) return [];
        return response.json();
    },

    async getConditions(id) {
        const response = await fetch(`/api/patients/${id}/conditions`);
        if (!response.ok) return [];
        return response.json();
    },

    async getVitals(id) {
        const response = await fetch(`/api/patients/${id}/vitals`);
        if (!response.ok) return [];
        return response.json();
    },

    async getLabs(id) {
        const response = await fetch(`/api/patients/${id}/labs`);
        if (!response.ok) return [];
        return response.json();
    },

    async getLastSync() {
        try {
            const response = await fetch('/api/metadata/last-sync');
            if (!response.ok) return { lastSync: 'Non Disponibile' };
            return await response.json();
        } catch (e) {
            return { lastSync: 'Non Disponibile' };
        }
    },

	async getAuditLogs() {
	    const response = await fetch('/api/audit-logs');
	    if (!response.ok) return [];
	    return response.json();
	},
	
    async resetDatabase() {
        const response = await fetch('/api/auth/reset-db', { method: 'POST' });
        return response.json();
    }
};