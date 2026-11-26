function renderVitalsChart(canvasId, vitalsData) {
    const ctx = document.getElementById(canvasId);
    if (!ctx) return;

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: vitalsData.map(v => v.dateTime),
            datasets: [{
                label: 'Frequenza Cardiaca (BPM)',
                data: vitalsData.map(v => v.value),
                borderColor: '#d32f2f',
                backgroundColor: 'rgba(211, 47, 47, 0.1)',
                tension: 0.2,
                fill: true
            }]
        },
        options: {
            responsive: true,
            scales: {
                y: { beginAtZero: false }
            }
        }
    });
}