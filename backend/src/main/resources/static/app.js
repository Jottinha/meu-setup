'use strict';

const countEl   = document.getElementById('count');
const btn       = document.getElementById('downloadBtn');
const btnLabel  = document.getElementById('btnLabel');
const clearBtn  = document.getElementById('clearBtn');

function getChecked() {
    return [...document.querySelectorAll('input[type="checkbox"]:checked')];
}

function updateUI() {
    const n = getChecked().length;
    countEl.textContent = n;
    btn.disabled = n === 0;
    btnLabel.textContent = n === 0
        ? 'Selecione ao menos 1 app'
        : `Baixar Instalador (${n} app${n > 1 ? 's' : ''})`;
}

document.addEventListener('change', (e) => {
    if (e.target.type === 'checkbox') updateUI();
});

clearBtn.addEventListener('click', () => {
    document.querySelectorAll('input[type="checkbox"]').forEach(cb => cb.checked = false);
    updateUI();
});

btn.addEventListener('click', async () => {
    const appIds = getChecked().map(cb => cb.value);
    if (appIds.length === 0) return;

    btn.disabled = true;
    btnLabel.textContent = 'Gerando instalador...';
    btn.insertAdjacentHTML('afterbegin', '<span class="spinner"></span>');

    try {
        const response = await fetch('/api/generate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(appIds),
        });

        if (!response.ok) {
            const msg = await response.text().catch(() => 'Erro desconhecido');
            throw new Error(msg);
        }

        const blob = await response.blob();
        const url  = URL.createObjectURL(blob);

        const anchor  = document.createElement('a');
        anchor.href   = url;
        anchor.download = 'Instalador.exe';
        anchor.click();
        URL.revokeObjectURL(url);

    } catch (err) {
        alert('Erro ao gerar o instalador:\n' + err.message);
        console.error(err);
    } finally {
        const spinner = btn.querySelector('.spinner');
        if (spinner) spinner.remove();
        updateUI();
    }
});
