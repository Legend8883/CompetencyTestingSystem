// 📦 УТИЛИТЫ ДЛЯ ФРОНТЕНДА

// ====================
// 📅 РАБОТА С ДАТАМИ
// ====================

/**
 * Форматирование даты
 * @param {string|Date} date - Дата для форматирования
 * @param {boolean} includeTime - Включать ли время
 * @returns {string} Отформатированная дата
 */
function formatDate(date, includeTime = true) {
    if (!date) return '';
    
    const d = new Date(date);
    
    if (isNaN(d.getTime())) {
        return 'Некорректная дата';
    }
    
    const options = {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
    };
    
    if (includeTime) {
        options.hour = '2-digit';
        options.minute = '2-digit';
    }
    
    return d.toLocaleDateString('ru-RU', options);
}

/**
 * Форматирование времени из секунд
 * @param {number} seconds - Время в секундах
 * @returns {string} Отформатированное время
 */
function formatTime(seconds) {
    if (!seconds || seconds < 0) return '00:00';
    
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    
    if (hours > 0) {
        return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
    return `${minutes}:${secs.toString().padStart(2, '0')}`;
}

/**
 * Относительное время (например, "2 часа назад")
 * @param {string} dateString - Дата в строковом формате
 * @returns {string} Относительное время
 */
function timeAgo(dateString) {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now - date;
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHour = Math.floor(diffMin / 60);
    const diffDay = Math.floor(diffHour / 24);
    
    if (diffDay > 0) return `${diffDay} дн. назад`;
    if (diffHour > 0) return `${diffHour} ч. назад`;
    if (diffMin > 0) return `${diffMin} мин. назад`;
    return 'только что';
}

// ====================
// 🔧 ВАЛИДАЦИЯ
// ====================

/**
 * Валидация email
 * @param {string} email - Email для проверки
 * @returns {boolean} Валиден ли email
 */
function isValidEmail(email) {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
}

/**
 * Валидация пароля
 * @param {string} password - Пароль для проверки
 * @returns {boolean} Валиден ли пароль (минимум 6 символов)
 */
function isValidPassword(password) {
    return password && password.length >= 6;
}

/**
 * Проверка совпадения паролей
 * @param {string} password - Пароль
 * @param {string} confirmPassword - Подтверждение пароля
 * @returns {boolean} Совпадают ли пароли
 */
function passwordsMatch(password, confirmPassword) {
    return password === confirmPassword;
}

// ====================
// 💾 LOCALSTORAGE
// ====================

/**
 * Безопасное сохранение в localStorage
 * @param {string} key - Ключ
 * @param {any} data - Данные для сохранения
 * @returns {boolean} Успешно ли сохранено
 */
function saveToStorage(key, data) {
    try {
        const json = JSON.stringify(data);
        localStorage.setItem(key, json);
        return true;
    } catch (error) {
        console.error('Ошибка сохранения в localStorage:', error);
        return false;
    }
}

/**
 * Безопасная загрузка из localStorage
 * @param {string} key - Ключ
 * @returns {any|null} Загруженные данные или null
 */
function loadFromStorage(key) {
    try {
        const json = localStorage.getItem(key);
        return json ? JSON.parse(json) : null;
    } catch (error) {
        console.error('Ошибка загрузки из localStorage:', key, error);
        return null;
    }
}

/**
 * Безопасное удаление из localStorage
 * @param {string} key - Ключ
 */
function removeFromStorage(key) {
    try {
        localStorage.removeItem(key);
    } catch (error) {
        console.error('Ошибка удаления из localStorage:', error);
    }
}

/**
 * Проверка доступности localStorage
 * @returns {boolean} Доступен ли localStorage
 */
function isLocalStorageAvailable() {
    try {
        const test = '__storage_test__';
        localStorage.setItem(test, test);
        localStorage.removeItem(test);
        return true;
    } catch (e) {
        return false;
    }
}

// ====================
// 📊 ФОРМАТИРОВАНИЕ
// ====================

/**
 * Форматирование числа с разделителями
 * @param {number} num - Число
 * @returns {string} Отформатированное число
 */
function formatNumber(num) {
    if (typeof num !== 'number') return '0';
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
}

/**
 * Форматирование процентов
 * @param {number} value - Значение
 * @param {number} total - Общее значение
 * @returns {string} Проценты с символом %
 */
function formatPercent(value, total = 100) {
    const percent = total > 0 ? Math.round((value / total) * 100) : 0;
    return `${percent}%`;
}

/**
 * Обрезание текста с добавлением многоточия
 * @param {string} text - Текст
 * @param {number} maxLength - Максимальная длина
 * @returns {string} Обрезанный текст
 */
function truncateText(text, maxLength = 100) {
    if (!text) return '';
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
}

// ====================
// 🎨 РАБОТА С ЦВЕТАМИ
// ====================

/**
 * Генерация случайного цвета
 * @returns {string} HEX цвет
 */
function getRandomColor() {
    const colors = [
        '#3b82f6', '#10b981', '#f59e0b', '#ef4444',
        '#8b5cf6', '#ec4899', '#14b8a6', '#f97316'
    ];
    return colors[Math.floor(Math.random() * colors.length)];
}

/**
 * Получение цвета по проценту
 * @param {number} percent - Процент
 * @returns {string} CSS класс цвета
 */
function getScoreColorClass(percent) {
    if (percent >= 90) return 'score-excellent';
    if (percent >= 75) return 'score-good';
    if (percent >= 60) return 'score-average';
    return 'score-poor';
}

/**
 * Получение текста статуса по проценту
 * @param {number} percent - Процент
 * @returns {string} Текст статуса
 */
function getScoreStatusText(percent) {
    if (percent >= 90) return 'Отлично';
    if (percent >= 75) return 'Хорошо';
    if (percent >= 60) return 'Удовлетворительно';
    return 'Неудовлетворительно';
}

// ====================
// 🛠️ ДОПОЛНИТЕЛЬНЫЕ УТИЛИТЫ
// ====================

/**
 * Дебаунс функция
 * @param {Function} func - Функция
 * @param {number} wait - Время ожидания в мс
 * @returns {Function} Дебаунсированная функция
 */
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

/**
 * Троттлинг функция
 * @param {Function} func - Функция
 * @param {number} limit - Лимит времени в мс
 * @returns {Function} Троттлированная функция
 */
function throttle(func, limit) {
    let inThrottle;
    return function() {
        const args = arguments;
        const context = this;
        if (!inThrottle) {
            func.apply(context, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
}

/**
 * Копирование текста в буфер обмена
 * @param {string} text - Текст для копирования
 * @returns {Promise<boolean>} Успешно ли скопировано
 */
async function copyToClipboard(text) {
    try {
        await navigator.clipboard.writeText(text);
        return true;
    } catch (err) {
        console.error('Ошибка копирования в буфер обмена:', err);
        // Fallback для старых браузеров
        try {
            const textArea = document.createElement('textarea');
            textArea.value = text;
            document.body.appendChild(textArea);
            textArea.select();
            document.execCommand('copy');
            document.body.removeChild(textArea);
            return true;
        } catch (fallbackErr) {
            console.error('Fallback также не сработал:', fallbackErr);
            return false;
        }
    }
}

/**
 * Скачивание файла
 * @param {string} filename - Имя файла
 * @param {string} content - Содержимое файла
 * @param {string} type - MIME тип
 */
function downloadFile(filename, content, type = 'text/plain') {
    const blob = new Blob([content], { type });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}

/**
 * Генерация уникального ID
 * @returns {string} Уникальный ID
 */
function generateId() {
    return Date.now().toString(36) + Math.random().toString(36).substr(2);
}

// ====================
// 📱 ОПРЕДЕЛЕНИЕ УСТРОЙСТВ
// ====================

/**
 * Проверка мобильного устройства
 * @returns {boolean} Мобильное ли устройство
 */
function isMobileDevice() {
    return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
}

/**
 * Проверка планшета
 * @returns {boolean} Планшет ли это
 */
function isTabletDevice() {
    return /iPad|Android(?!.*Mobile)|Tablet|Silk/i.test(navigator.userAgent);
}

/**
 * Проверка десктопа
 * @returns {boolean} Десктоп ли это
 */
function isDesktopDevice() {
    return !isMobileDevice() && !isTabletDevice();
}

// ====================
// 🔗 РАБОТА С URL
// ====================

/**
 * Получение параметров из URL
 * @returns {Object} Параметры URL
 */
function getUrlParams() {
    const params = {};
    const queryString = window.location.search.substring(1);
    const pairs = queryString.split('&');
    
    for (let pair of pairs) {
        const [key, value] = pair.split('=');
        if (key) {
            params[decodeURIComponent(key)] = decodeURIComponent(value || '');
        }
    }
    
    return params;
}

/**
 * Установка параметров URL
 * @param {Object} params - Параметры для установки
 */
function setUrlParams(params) {
    const url = new URL(window.location);
    Object.entries(params).forEach(([key, value]) => {
        if (value === null || value === undefined) {
            url.searchParams.delete(key);
        } else {
            url.searchParams.set(key, value);
        }
    });
    window.history.pushState({}, '', url);
}

/**
 * Обновление параметра URL
 * @param {string} key - Ключ параметра
 * @param {string} value - Значение параметра
 */
function updateUrlParam(key, value) {
    const params = getUrlParams();
    params[key] = value;
    setUrlParams(params);
}

// ====================
// 🎯 ОБРАБОТКА ФОРМ
// ====================

/**
 * Показать ошибку в форме
 * @param {string} elementId - ID элемента ошибки
 * @param {string} message - Сообщение об ошибке
 */
function showError(elementId, message) {
    const element = document.getElementById(elementId);
    if (element) {
        element.textContent = message;
        element.classList.add('show');
        const input = document.getElementById(elementId.replace('Error', ''));
        if (input) input.classList.add('error');
    }
}

/**
 * Скрыть ошибку в форме
 * @param {string} elementId - ID элемента ошибки
 */
function hideError(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
        element.classList.remove('show');
        const input = document.getElementById(elementId.replace('Error', ''));
        if (input) input.classList.remove('error');
    }
}

/**
 * Очистить все ошибки формы
 * @param {string} formId - ID формы
 */
function clearFormErrors(formId) {
    const form = document.getElementById(formId);
    if (form) {
        const errors = form.querySelectorAll('.error-message');
        errors.forEach(error => {
            error.classList.remove('show');
            error.textContent = '';
        });
        
        const inputs = form.querySelectorAll('.error');
        inputs.forEach(input => input.classList.remove('error'));
    }
}

/**
 * Валидация формы
 * @param {HTMLFormElement} form - Форма для валидации
 * @returns {boolean} Валидна ли форма
 */
function validateForm(form) {
    let isValid = true;
    const requiredInputs = form.querySelectorAll('[required]');
    
    requiredInputs.forEach(input => {
        if (!input.value.trim()) {
            isValid = false;
            input.classList.add('error');
            
            const errorId = input.id + 'Error';
            const errorElement = document.getElementById(errorId);
            if (errorElement) {
                errorElement.textContent = 'Это поле обязательно для заполнения';
                errorElement.classList.add('show');
            }
        }
    });
    
    return isValid;
}

// ====================
// 🎪 АНИМАЦИИ И ЭФФЕКТЫ
// ====================

/**
 * Плавная прокрутка к элементу
 * @param {string} elementId - ID элемента
 * @param {number} offset - Смещение
 */
function smoothScrollTo(elementId, offset = 0) {
    const element = document.getElementById(elementId);
    if (element) {
        const elementPosition = element.getBoundingClientRect().top;
        const offsetPosition = elementPosition + window.pageYOffset - offset;
        
        window.scrollTo({
            top: offsetPosition,
            behavior: 'smooth'
        });
    }
}

/**
 * Показать/скрыть элемент с анимацией
 * @param {string} elementId - ID элемента
 * @param {boolean} show - Показать или скрыть
 */
function toggleElement(elementId, show) {
    const element = document.getElementById(elementId);
    if (!element) return;
    
    if (show) {
        element.classList.remove('hidden');
        element.style.opacity = '0';
        element.style.transform = 'translateY(10px)';
        
        requestAnimationFrame(() => {
            element.style.transition = 'opacity 0.3s, transform 0.3s';
            element.style.opacity = '1';
            element.style.transform = 'translateY(0)';
        });
    } else {
        element.style.transition = 'opacity 0.3s, transform 0.3s';
        element.style.opacity = '0';
        element.style.transform = 'translateY(10px)';
        
        setTimeout(() => {
            element.classList.add('hidden');
            element.style.transition = '';
            element.style.opacity = '';
            element.style.transform = '';
        }, 300);
    }
}

/**
 * Анимация загрузки
 * @param {string} elementId - ID элемента
 * @param {boolean} show - Показать или скрыть анимацию
 */
function toggleLoading(elementId, show) {
    const element = document.getElementById(elementId);
    if (!element) return;
    
    if (show) {
        element.innerHTML = '<span class="loading"></span>';
    } else {
        element.innerHTML = element.dataset.originalText || '';
    }
}

// ====================
// 📦 ЭКСПОРТ
// ====================

// Экспортируем все функции
window.utils = {
    // Даты
    formatDate,
    formatTime,
    timeAgo,
    
    // Валидация
    isValidEmail,
    isValidPassword,
    passwordsMatch,
    
    // LocalStorage
    saveToStorage,
    loadFromStorage,
    removeFromStorage,
    isLocalStorageAvailable,
    
    // Форматирование
    formatNumber,
    formatPercent,
    truncateText,
    
    // Цвета
    getRandomColor,
    getScoreColorClass,
    getScoreStatusText,
    
    // Утилиты
    debounce,
    throttle,
    copyToClipboard,
    downloadFile,
    generateId,
    
    // Устройства
    isMobileDevice,
    isTabletDevice,
    isDesktopDevice,
    
    // URL
    getUrlParams,
    setUrlParams,
    updateUrlParam,
    
    // Формы
    showError,
    hideError,
    clearFormErrors,
    validateForm,
    
    // Анимации
    smoothScrollTo,
    toggleElement,
    toggleLoading
};