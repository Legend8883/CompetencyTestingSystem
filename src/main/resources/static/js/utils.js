class Utils {
    static formatDate(dateString) {
        if (!dateString) return 'Нет даты';
        try {
            const date = new Date(dateString);
            return date.toLocaleDateString('ru-RU', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch (e) {
            return dateString;
        }
    }

    static formatTime(minutes) {
        if (!minutes) return '0 мин';
        if (minutes < 60) {
            return `${minutes} мин`;
        }
        const hours = Math.floor(minutes / 60);
        const mins = minutes % 60;
        return mins > 0 ? `${hours} ч ${mins} мин` : `${hours} ч`;
    }

    static createModal(title, content, onClose = null) {
        const modal = document.createElement('div');
        modal.className = 'modal';
        modal.innerHTML = `
            <div class="modal-content">
                <div class="modal-header">
                    <h3 style="margin: 0;">${title}</h3>
                    <button class="modal-close" style="background: none; border: none; font-size: 1.5rem; cursor: pointer; color: var(--gray-text);">&times;</button>
                </div>
                <div class="modal-body">${content}</div>
            </div>
        `;

        document.body.appendChild(modal);

        const closeBtn = modal.querySelector('.modal-close');
        const closeModal = () => {
            modal.classList.remove('active');
            setTimeout(() => {
                if (document.body.contains(modal)) {
                    document.body.removeChild(modal);
                }
                if (onClose) onClose();
            }, 300);
        };

        closeBtn.onclick = closeModal;
        modal.onclick = (e) => {
            if (e.target === modal) closeModal();
        };

        setTimeout(() => modal.classList.add('active'), 10);
        return modal;
    }

    static confirm(message, onConfirm, onCancel = null) {
        const modal = this.createModal('Подтверждение', `
            <p>${message}</p>
            <div style="display: flex; gap: 1rem; margin-top: 1.5rem;">
                <button id="confirmCancel" class="btn btn-outline" style="flex: 1;">Отмена</button>
                <button id="confirmOk" class="btn btn-primary" style="flex: 1;">ОК</button>
            </div>
        `);

        modal.querySelector('#confirmOk').onclick = () => {
            closeModal();
            if (onConfirm) onConfirm();
        };

        modal.querySelector('#confirmCancel').onclick = () => {
            closeModal();
            if (onCancel) onCancel();
        };
        
        const closeModal = () => modal.querySelector('.modal-close').click();
    }

    static showLoading(element) {
        if (element) {
            element.innerHTML = '<div style="text-align: center; padding: 2rem;">Загрузка...</div>';
        }
    }

    static showError(element, message) {
        if (element) {
            element.innerHTML = `<div class="error-message">${message}</div>`;
        }
    }

    static showSuccess(element, message) {
        if (element) {
            element.innerHTML = `<div class="success-message">${message}</div>`;
        }
    }
}

window.Utils = Utils;