const API_BASE_URL = 'http://localhost:8080/api';

class ApiService {
    constructor() {
        this.token = localStorage.getItem('authToken');
        this.user = JSON.parse(localStorage.getItem('user') || 'null');
    }

    setToken(token) {
        this.token = token;
        localStorage.setItem('authToken', token);
    }

    setUser(user) {
        this.user = user;
        localStorage.setItem('user', JSON.stringify(user));
    }

    clearAuth() {
        this.token = null;
        this.user = null;
        localStorage.removeItem('authToken');
        localStorage.removeItem('user');
    }

    getHeaders() {
        const headers = {
            'Content-Type': 'application/json',
        };

        if (this.token) {
            headers['Authorization'] = `Bearer ${this.token}`;
        }

        return headers;
    }

    async getMyAttempts() {
        console.log('Fetching my attempts...');
        const response = await fetch(`${API_BASE_URL}/employee/attempts`, {
            headers: this.getHeaders()
        });

        return this.handleResponse(response);
    }

    async handleResponse(response) {
        const contentType = response.headers.get('content-type');

        // Если ответ пустой (например, 204 No Content)
        if (response.status === 204) {
            return { success: true };
        }

        // Проверяем, есть ли контент для парсинга JSON
        if (contentType && contentType.includes('application/json')) {
            const data = await response.json();

            if (!response.ok) {
                console.error('API Error:', {
                    status: response.status,
                    data: data,
                    url: response.url
                });

                if (response.status === 401) {
                    // Unauthorized - токен не валидный
                    this.clearAuth();
                    window.location.href = '../login.html';
                }

                if (response.status === 403) {
                    // Forbidden - нет прав доступа
                    throw new Error('Доступ запрещен. У вас нет прав для этого действия. Требуется роль HR.');
                }

                throw new Error(data.message || data.error || `Ошибка ${response.status}`);
            }

            return data;
        } else {
            // Если не JSON, просто возвращаем статус
            if (!response.ok) {
                throw new Error(`Ошибка ${response.status}`);
            }
            return { success: true };
        }
    }

    // Регистрация
    async register(userData) {
        const response = await fetch(`${API_BASE_URL}/auth/register`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify(userData)
        });

        return this.handleResponse(response);
    }

    async registerHR(userData) {
        const response = await fetch(`${API_BASE_URL}/auth/register-hr`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify(userData)
        });

        return this.handleResponse(response);
    }

    // Вход
    async login(email, password) {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify({ email, password })
        });

        const data = await this.handleResponse(response);
        if (data.data?.token) {
            this.setToken(data.data.token);
            this.setUser(data.data);
        }
        return data;
    }

    // HR функционал - добавьте эти методы
    async getHRTests() {
        console.log('Fetching HR tests...');
        try {
            const response = await fetch(`${API_BASE_URL}/hr/tests`, {
                headers: this.getHeaders()
            });

            const data = await this.handleResponse(response);
            console.log('HR tests loaded:', data.data?.length || 0);
            return data;
        } catch (error) {
            console.error('Error loading HR tests:', error);
            return { success: false, data: [], message: error.message };
        }
    }

    async createTest(testData) {
        console.log('Creating test:', testData);
        const response = await fetch(`${API_BASE_URL}/hr/tests`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify(testData)
        });

        return this.handleResponse(response);
    }

    async activateTest(testId) {
        console.log('Activating test:', testId);
        const response = await fetch(`${API_BASE_URL}/hr/tests/${testId}/activate`, {
            method: 'PATCH',
            headers: this.getHeaders()
        });

        return this.handleResponse(response);
    }

    async deactivateTest(testId) {
        console.log('Deactivating test:', testId);
        const response = await fetch(`${API_BASE_URL}/hr/tests/${testId}/deactivate`, {
            method: 'PATCH',
            headers: this.getHeaders()
        });

        return this.handleResponse(response);
    }

    async getEmployees() {
        console.log('Fetching employees...');
        try {
            const response = await fetch(`${API_BASE_URL}/hr/employees`, {
                headers: this.getHeaders()
            });

            const data = await this.handleResponse(response);
            console.log('Employees loaded:', data.data?.length || 0);
            return data;
        } catch (error) {
            console.error('Error loading employees:', error);
            return { success: false, data: [], message: error.message };
        }
    }

    async assignTest(testId, userIds, deadline) {
        console.log('Assigning test:', { testId, userIds, deadline });
        const response = await fetch(`${API_BASE_URL}/hr/tests/${testId}/assign`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify({ userIds, deadline })
        });

        return this.handleResponse(response);
    }

    // Employee функционал
    async getAvailableTests() {
        console.log('Fetching available tests for employee...');
        try {
            const response = await fetch(`${API_BASE_URL}/employee/tests/available`, {
                headers: this.getHeaders()
            });

            const data = await this.handleResponse(response);
            console.log('Available tests loaded:', data.data?.length || 0);
            return data;
        } catch (error) {
            console.error('Error loading available tests:', error);
            return { success: false, data: [], message: error.message };
        }
    }

    async startTest(testId) {
        console.log('Starting test:', testId);
        const response = await fetch(`${API_BASE_URL}/employee/tests/start`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify({ testId })
        });

        return this.handleResponse(response);
    }

    async getTestProgress(attemptId) {
        console.log('Fetching test progress for attempt:', attemptId);
        try {
            const response = await fetch(`${API_BASE_URL}/employee/attempts/${attemptId}/progress`, {
                headers: this.getHeaders()
            });

            const data = await this.handleResponse(response);
            console.log('Test progress loaded successfully');
            return data;
        } catch (error) {
            console.error('Error loading test progress:', error);
            throw error;
        }
    }

    async submitAnswer(attemptId, questionId, answerData) {
        console.log('Submitting answer:', { attemptId, questionId, answerData });
        const response = await fetch(`${API_BASE_URL}/employee/attempts/${attemptId}/answers`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify({
                questionId, // <-- Должен быть первым параметром
                ...answerData
            })
        });

        return this.handleResponse(response);
    }

    async completeTest(attemptId) {
        console.log('Completing test attempt:', attemptId);
        const response = await fetch(`${API_BASE_URL}/employee/attempts/${attemptId}/complete`, {
            method: 'POST',
            headers: this.getHeaders()
        });

        return this.handleResponse(response);
    }

    async getOpenAnswers() {
        console.log('Fetching open answers for evaluation...');
        const response = await fetch(`${API_BASE_URL}/hr/evaluation/open-answers`, {
            headers: this.getHeaders()
        });

        return this.handleResponse(response);
    }

    // Оценить открытый вопрос
    async evaluateAnswer(answerId, score) {
        console.log('Evaluating answer:', { answerId, score });
        const response = await fetch(`${API_BASE_URL}/hr/evaluation/answers/${answerId}`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify({ score })
        });

        return this.handleResponse(response);
    }

    // Получить попытки на проверке
    async getAttemptsForEvaluation() {
        console.log('Fetching attempts for evaluation...');
        const response = await fetch(`${API_BASE_URL}/hr/evaluation/attempts`, {
            headers: this.getHeaders()
        });

        return this.handleResponse(response);
    }

    // Завершить проверку попытки
    async completeEvaluation(attemptId) {
        console.log('Completing evaluation for attempt:', attemptId);
        const response = await fetch(`${API_BASE_URL}/hr/evaluation/attempts/${attemptId}/complete`, {
            method: 'POST',
            headers: this.getHeaders()
        });

        return this.handleResponse(response);
    }

    async getAllQuestions(attemptId) {
        console.log('Fetching all questions for attempt:', attemptId);
        const response = await fetch(`${API_BASE_URL}/employee/attempts/${attemptId}/questions`, {
            headers: this.getHeaders()
        });

        return this.handleResponse(response);
    }

    // Получить конкретный вопрос
    async getQuestion(attemptId, questionId) {
        console.log('Fetching question:', { attemptId, questionId });
        const response = await fetch(`${API_BASE_URL}/employee/attempts/${attemptId}/questions/${questionId}`, {
            headers: this.getHeaders()
        });

        return this.handleResponse(response);
    }

    // Перейти к вопросу
    async goToQuestion(attemptId, questionId) {
        console.log('Going to question:', { attemptId, questionId });
        const response = await fetch(`${API_BASE_URL}/employee/attempts/${attemptId}/go-to-question`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify({ questionId })
        });

        return this.handleResponse(response);
    }

    // Для сотрудника: получить результаты попытки
    async getAttemptResults(attemptId) {
        console.log('Fetching attempt results:', attemptId);
        const response = await fetch(`${API_BASE_URL}/employee/attempts/${attemptId}/results`, {
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    }

    // Для сотрудника: получить попытку с ответами (для статуса "На проверке")
    async getAttemptWithAnswers(attemptId) {
        console.log('Fetching attempt with answers:', attemptId);
        const response = await fetch(`${API_BASE_URL}/employee/attempts/${attemptId}/with-answers`, {
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    }

    // Для HR: получить все попытки
    async getAllAttempts() {
        console.log('Fetching all attempts for HR...');
        const response = await fetch(`${API_BASE_URL}/hr/attempts/all`, {
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    }

    // Для HR: получить детали попытки
    async getAttemptDetailsForHR(attemptId) {
        console.log('Fetching attempt details for HR:', attemptId);
        const response = await fetch(`${API_BASE_URL}/hr/attempts/${attemptId}/full`, {
            headers: this.getHeaders()
        });
        return this.handleResponse(response);
    }
}

// Создаем глобальный экземпляр
window.apiService = new ApiService();