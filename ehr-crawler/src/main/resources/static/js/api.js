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
        return response.json();
    },

    async getPatient(id) {
        const response = await fetch(`/api/patients/${id}`);
        return response.json();
    },

    async getMedications(id) {
        const response = await fetch(`/api/patients/${id}/medications`);
        return response.json();
    },

    async getLastSync() {
        const response = await fetch('/api/metadata/last-sync');
        return response.json();
    }
};