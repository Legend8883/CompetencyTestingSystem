// Базовый URL API
const API_BASE_URL = 'http://localhost:8080/api';

// Хранение токена и данных пользователя
let authToken = localStorage.getItem('authToken') || '';
let currentUser = JSON.parse(localStorage.getItem('user') || 'null');

// Установка данных пользователя
function setUserData(token, userData) {
    authToken = token;
    currentUser = userData;
    
    localStorage.setItem('authToken', token);
    localStorage.setItem('user', JSON.stringify(userData));
}

// Получение текущего пользователя
function getCurrentUser() {
    return currentUser;
}

// Получение роли пользователя
function getUserRole() {
    return currentUser?.role || '';
}

// Очистка данных
function clearAuthData() {
    authToken = '';
    currentUser = null;
    localStorage.removeItem('authToken');
    localStorage.removeItem('user');
}

// Проверка, является ли пользователь HR
function isHR() {
    return getUserRole() === 'HR';
}

// Проверка, является ли пользователем Employee
function isEmployee() {
    return getUserRole() === 'EMPLOYEE';
}

// Проверка авторизации
function checkAuth() {
    if (!authToken || !currentUser) {
        return null;
    }
    return currentUser;
}

// Выход из системы
function logout() {
    clearAuthData();
    window.location.href = 'index.html';
}

// Базовый запрос с обработкой ошибок
async function apiRequest(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };
    
    if (authToken) {
        headers['Authorization'] = `Bearer ${authToken}`;
    }
    
    const config = {
        ...options,
        headers
    };
    
    try {
        const response = await fetch(url, config);
        
        // Если 401 - перенаправляем на логин
        if (response.status === 401) {
            clearAuthData();
            window.location.href = 'index.html';
            throw new Error('Требуется авторизация');
        }
        
        // Если 403 - нет прав
        if (response.status === 403) {
            throw new Error('У вас нет прав для этого действия');
        }
        
        if (!response.ok) {
            const errorText = await response.text();
            let errorMessage = `HTTP ${response.status}: ${errorText}`;
            
            try {
                const errorJson = JSON.parse(errorText);
                errorMessage = errorJson.message || errorMessage;
            } catch (e) {
                // Оставляем текстовое сообщение
            }
            
            throw new Error(errorMessage);
        }
        
        // Парсим JSON ответ
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            const data = await response.json();
            return data;
        } else {
            return await response.text();
        }
        
    } catch (error) {
        console.error('API request failed:', error);
        
        // Показываем пользователю ошибку
        if (error.message !== 'Требуется авторизация') {
            showNotification(`Ошибка: ${error.message}`, 'error');
        }
        
        throw error;
    }
}

// API методы для аутентификации
const authApi = {
    async login(email, password) {
        const response = await apiRequest('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });
        
        if (response.success && response.data) {
            setUserData(response.data.token, {
                userId: response.data.userId,
                email: response.data.email,
                firstName: response.data.firstName,
                lastName: response.data.lastName,
                role: response.data.role
            });
        }
        
        return response;
    },
    
    async registerEmployee(userData) {
        return await apiRequest('/auth/register', {
            method: 'POST',
            body: JSON.stringify(userData)
        });
    },
    
    async registerHR(userData) {
        return await apiRequest('/auth/register-hr', {
            method: 'POST',
            body: JSON.stringify(userData)
        });
    }
};

// API методы для HR
const hrApi = {
    // Тесты
    async getTests() {
        const response = await apiRequest('/hr/tests');
        return response.success ? response.data : [];
    },
    
    async getTest(testId) {
        const response = await apiRequest(`/hr/tests/${testId}`);
        return response.success ? response.data : null;
    },
    
    async createTest(testData) {
        const response = await apiRequest('/hr/tests', {
            method: 'POST',
            body: JSON.stringify(testData)
        });
        return response;
    },
    
    async updateTest(testId, testData) {
        const response = await apiRequest(`/hr/tests/${testId}`, {
            method: 'PUT',
            body: JSON.stringify(testData)
        });
        return response;
    },
    
    async activateTest(testId) {
        const response = await apiRequest(`/hr/tests/${testId}/activate`, {
            method: 'PATCH'
        });
        return response;
    },
    
    async deactivateTest(testId) {
        const response = await apiRequest(`/hr/tests/${testId}/deactivate`, {
            method: 'PATCH'
        });
        return response;
    },
    
    // Назначение тестов
    async assignTest(testId, userIds, deadline) {
        const response = await apiRequest(`/hr/tests/${testId}/assign`, {
            method: 'POST',
            body: JSON.stringify({ userIds, deadline })
        });
        return response;
    },
    
    async getAssignments(testId) {
        const response = await apiRequest(`/hr/tests/${testId}/assignments`);
        return response.success ? response.data : [];
    },
    
    // Сотрудники
    async getEmployees() {
        const response = await apiRequest('/hr/employees');
        return response.success ? response.data : [];
    },
    
    async searchEmployees(query) {
        const response = await apiRequest(`/hr/employees/search?query=${encodeURIComponent(query)}`);
        return response.success ? response.data : [];
    },
    
    // Проверка результатов
    async getOpenAnswers() {
        const response = await apiRequest('/hr/evaluation/open-answers');
        return response.success ? response.data : [];
    },
    
    async evaluateAnswer(answerId, score) {
        const response = await apiRequest(`/hr/evaluation/answers/${answerId}`, {
            method: 'POST',
            body: JSON.stringify({ score })
        });
        return response;
    },
    
    async getAttemptsForEvaluation() {
        const response = await apiRequest('/hr/evaluation/attempts');
        return response.success ? response.data : [];
    },
    
    async completeEvaluation(attemptId) {
        const response = await apiRequest(`/hr/evaluation/attempts/${attemptId}/complete`, {
            method: 'POST'
        });
        return response;
    },
    
    // Статистика
    async getStatistics() {
        const response = await apiRequest('/hr/statistics');
        return response.success ? response.data : {};
    }
};

// API методы для Employee
const employeeApi = {
    // Доступные тесты
    async getAvailableTests() {
        const response = await apiRequest('/employee/tests/available');
        return response.success ? response.data : [];
    },
    
    // Прохождение теста
    async startTest(testId) {
        const response = await apiRequest('/employee/tests/start', {
            method: 'POST',
            body: JSON.stringify({ testId })
        });
        return response;
    },
    
    async submitAnswer(attemptId, questionId, answerData) {
        const response = await apiRequest(`/employee/attempts/${attemptId}/answers`, {
            method: 'POST',
            body: JSON.stringify(answerData)
        });
        return response;
    },
    
    async getProgress(attemptId) {
        const response = await apiRequest(`/employee/attempts/${attemptId}/progress`);
        return response.success ? response.data : null;
    },
    
    async completeTest(attemptId) {
        const response = await apiRequest(`/employee/attempts/${attemptId}/complete`, {
            method: 'POST'
        });
        return response;
    },
    
    // Результаты
    async getMyAttempts() {
        const response = await apiRequest('/employee/attempts');
        return response.success ? response.data : [];
    },
    
    async getAttemptDetails(attemptId) {
        const response = await apiRequest(`/employee/attempts/${attemptId}`);
        return response.success ? response.data : null;
    }
};

// Общие API методы
const commonApi = {
    // Профиль
    async getProfile() {
        const response = await apiRequest('/profile');
        return response.success ? response.data : null;
    },
    
    async updateProfile(profileData) {
        const response = await apiRequest('/profile', {
            method: 'PUT',
            body: JSON.stringify(profileData)
        });
        return response;
    }
};

// Утилита для показа уведомлений
function showNotification(message, type = 'info', duration = 5000) {
    // Создаем элемент уведомления
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.innerHTML = `
        <div class="notification-content">${message}</div>
        <button class="notification-close" onclick="this.parentElement.remove()">×</button>
    `;
    
    // Добавляем стили
    if (!document.querySelector('#notification-styles')) {
        const style = document.createElement('style');
        style.id = 'notification-styles';
        style.textContent = `
            .notification {
                position: fixed;
                top: 20px;
                right: 20px;
                padding: 1rem 1.5rem;
                border-radius: 8px;
                box-shadow: 0 4px 6px rgba(0,0,0,0.1);
                z-index: 9999;
                animation: slideIn 0.3s ease;
                max-width: 400px;
            }
            .notification-info { background: #dbeafe; color: #1e40af; border-left: 4px solid #3b82f6; }
            .notification-success { background: #d1fae5; color: #065f46; border-left: 4px solid #10b981; }
            .notification-warning { background: #fef3c7; color: #92400e; border-left: 4px solid #f59e0b; }
            .notification-error { background: #fee2e2; color: #991b1b; border-left: 4px solid #ef4444; }
            .notification-content { margin-right: 1.5rem; }
            .notification-close {
                position: absolute;
                top: 0.5rem;
                right: 0.5rem;
                background: none;
                border: none;
                font-size: 1.5rem;
                cursor: pointer;
                color: inherit;
                opacity: 0.7;
            }
            @keyframes slideIn {
                from { transform: translateX(100%); opacity: 0; }
                to { transform: translateX(0); opacity: 1; }
            }
        `;
        document.head.appendChild(style);
    }
    
    document.body.appendChild(notification);
    
    // Автоматическое удаление
    if (duration > 0) {
        setTimeout(() => {
            if (notification.parentElement) {
                notification.remove();
            }
        }, duration);
    }
    
    return notification;
}

// Экспорт
window.api = {
    auth: authApi,
    hr: hrApi,
    employee: employeeApi,
    common: commonApi,
    utils: {
        checkAuth,
        logout,
        getUserRole,
        isHR,
        isEmployee,
        getCurrentUser,
        showNotification
    }
};