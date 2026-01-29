class EmployeeService {
    constructor() {
        this.api = apiService;
        this.auth = authService;
        this.currentAttempt = null;
        this.timerInterval = null;
    }

    async loadDashboard() {
        if (!this.auth.checkRole('EMPLOYEE')) return;

        try {
            const [testsResponse, resultsResponse] = await Promise.all([
                this.api.getAvailableTests(),
                this.api.getMyResults()
            ]);

            const tests = testsResponse.data || [];
            const results = resultsResponse.data || [];

            this.renderDashboard(tests, results);
        } catch (error) {
            console.error('Ошибка загрузки дашборда:', error);
            Utils.showError(document.getElementById('dashboard-content'), error.message);
        }
    }

    renderDashboard(tests, results) {
        const dashboard = document.getElementById('dashboard-content');
        if (!dashboard) return;

        const user = this.auth.getUser();
        
        dashboard.innerHTML = `
            <div class="dashboard-header">
                <h2>Панель сотрудника</h2>
                <div class="user-info">
                    <div class="user-avatar">${user.firstName[0]}${user.lastName[0]}</div>
                    <div>
                        <strong>${user.firstName} ${user.lastName}</strong>
                        <div>${user.email}</div>
                    </div>
                </div>
            </div>

            <div class="stats-grid">
                <div class="stat-card">
                    <h3>Доступные тесты</h3>
                    <div class="stat-value">${tests.length}</div>
                </div>
                <div class="stat-card">
                    <h3>Пройдено тестов</h3>
                    <div class="stat-value">${results.filter(r => r.status === 'EVALUATED').length}</div>
                </div>
                <div class="stat-card">
                    <h3>Средний балл</h3>
                    <div class="stat-value">
                        ${results.length > 0 ? 
                            Math.round(results.reduce((sum, r) => sum + (r.score || 0), 0) / results.length) : 
                            '0'}
                    </div>
                </div>
                <div class="stat-card">
                    <h3>Успешность</h3>
                    <div class="stat-value">
                        ${results.length > 0 ? 
                            Math.round(results.filter(r => r.passed).length / results.length * 100) + '%' : 
                            '0%'}
                    </div>
                </div>
            </div>

            <h3 style="margin: 2rem 0 1rem;">Доступные тесты</h3>
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>Название теста</th>
                            <th>Вопросы</th>
                            <th>Время</th>
                            <th>Проходной балл</th>
                            <th>Действия</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${tests.map(test => `
                            <tr>
                                <td>
                                    <strong>${test.title}</strong>
                                    ${test.description ? `<br><small>${test.description}</small>` : ''}
                                </td>
                                <td>${test.questionCount}</td>
                                <td>${Utils.formatTime(test.timeLimitMinutes)}</td>
                                <td>${test.passingScore}</td>
                                <td>
                                    <button class="btn btn-primary btn-small" 
                                            onclick="employeeService.startTest(${test.id})">
                                        Начать тест
                                    </button>
                                </td>
                            </tr>
                        `).join('')}
                        ${tests.length === 0 ? `
                            <tr>
                                <td colspan="5" style="text-align: center; padding: 2rem;">
                                    Нет доступных тестов
                                </td>
                            </tr>
                        ` : ''}
                    </tbody>
                </table>
            </div>

            ${results.length > 0 ? `
                <h3 style="margin: 2rem 0 1rem;">История тестирований</h3>
                <div class="table-container">
                    <table>
                        <thead>
                            <tr>
                                <th>Тест</th>
                                <th>Дата</th>
                                <th>Балл</th>
                                <th>Статус</th>
                                <th>Результат</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${results.slice(0, 5).map(result => `
                                <tr>
                                    <td>${result.testTitle}</td>
                                    <td>${Utils.formatDate(result.startedAt)}</td>
                                    <td>${result.score || 'Нет'}</td>
                                    <td>
                                        <span class="status ${result.status === 'EVALUATED' ? 'status-active' : 'status-pending'}">
                                            ${this.getAttemptStatusLabel(result.status)}
                                        </span>
                                    </td>
                                    <td>
                                        ${result.passed ? 
                                            '<span style="color: #27ae60;">✓ Прошел</span>' : 
                                            '<span style="color: #e74c3c;">✗ Не прошел</span>'
                                        }
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            ` : ''}
        `;
    }

    async startTest(testId) {
        try {
            const response = await this.api.startTest(testId);
            this.currentAttempt = response.data.attemptId;
            
            // Сохраняем attemptId в sessionStorage для восстановления при перезагрузке
            sessionStorage.setItem('currentAttempt', this.currentAttempt);
            sessionStorage.setItem('testStartedAt', new Date().toISOString());
            
            window.location.href = 'take-test.html';
        } catch (error) {
            alert('Ошибка начала теста: ' + error.message);
        }
    }

    async loadTest() {
        const attemptId = sessionStorage.getItem('currentAttempt');
        if (!attemptId) {
            window.location.href = 'employee-dashboard.html';
            return;
        }

        try {
            const response = await this.api.getTestProgress(attemptId);
            const progress = response.data;
            this.currentAttempt = attemptId;
            
            this.renderTest(progress);
            this.startTimer(progress.timeLeftMinutes);
            
            // Автосохранение каждые 30 секунд
            setInterval(() => this.autoSaveProgress(), 30000);
            
        } catch (error) {
            console.error('Ошибка загрузки теста:', error);
            if (error.message.includes('401') || error.message.includes('404')) {
                sessionStorage.removeItem('currentAttempt');
                window.location.href = 'employee-dashboard.html';
            }
        }
    }

    renderTest(progress) {
        const testContainer = document.getElementById('test-container');
        if (!testContainer) return;

        const currentQuestion = progress.currentQuestion;
        const questionProgress = progress.questionProgress;
        
        testContainer.innerHTML = `
            <div class="test-header">
                <h2>${progress.testTitle}</h2>
                <div class="timer" id="timer">Осталось времени: ${progress.timeLeftMinutes} минут</div>
                <div class="progress">
                    Вопрос ${progress.currentQuestionIndex + 1} из ${progress.totalQuestions}
                </div>
            </div>

            <div class="progress-bar">
                ${questionProgress.map((q, idx) => `
                    <div class="progress-item ${q.answered ? 'answered' : ''} ${q.visited ? 'visited' : ''}"
                         title="Вопрос ${idx + 1}"
                         onclick="employeeService.goToQuestion(${q.questionId})">
                    </div>
                `).join('')}
            </div>

            <div class="question">
                <h3>${currentQuestion.text}</h3>
                
                ${this.renderQuestionInput(currentQuestion)}
            </div>

            <div class="test-controls">
                <button class="btn btn-outline" onclick="employeeService.saveAnswer()">
                    Сохранить ответ
                </button>
                
                ${progress.currentQuestionIndex === progress.totalQuestions - 1 ? `
                    <button class="btn btn-primary" onclick="employeeService.completeTest()">
                        Завершить тест
                    </button>
                ` : `
                    <button class="btn btn-primary" onclick="employeeService.nextQuestion()">
                        Следующий вопрос
                    </button>
                `}
            </div>

            <div id="save-status" style="margin-top: 1rem;"></div>
        `;

        // Восстанавливаем сохраненные ответы
        this.restoreSavedAnswer(currentQuestion);
    }

    renderQuestionInput(question) {
        switch (question.type) {
            case 'SINGLE_CHOICE':
                return `
                    <div class="options">
                        ${question.options.map(option => `
                            <label class="option">
                                <input type="radio" 
                                       name="answer" 
                                       value="${option.id}"
                                       data-question-id="${question.id}">
                                ${option.text}
                            </label>
                        `).join('')}
                    </div>
                `;

            case 'MULTIPLE_CHOICE':
                return `
                    <div class="options">
                        ${question.options.map(option => `
                            <label class="option">
                                <input type="checkbox" 
                                       value="${option.id}"
                                       data-question-id="${question.id}">
                                ${option.text}
                            </label>
                        `).join('')}
                    </div>
                `;

            case 'OPEN_ANSWER':
                return `
                    <textarea id="open-answer" 
                              data-question-id="${question.id}"
                              placeholder="Введите ваш ответ..."
                              rows="6"
                              maxlength="5000"></textarea>
                `;

            default:
                return '<p>Неизвестный тип вопроса</p>';
        }
    }

    restoreSavedAnswer(question) {
        const savedAnswers = JSON.parse(sessionStorage.getItem('savedAnswers') || '{}');
        const savedAnswer = savedAnswers[question.id];

        if (!savedAnswer) return;

        if (question.type === 'OPEN_ANSWER') {
            const textarea = document.querySelector('textarea[data-question-id="' + question.id + '"]');
            if (textarea) textarea.value = savedAnswer.text;
        } else if (question.type === 'SINGLE_CHOICE') {
            const radio = document.querySelector(`input[type="radio"][value="${savedAnswer.selectedOptionIds[0]}"]`);
            if (radio) radio.checked = true;
        } else if (question.type === 'MULTIPLE_CHOICE') {
            savedAnswer.selectedOptionIds.forEach(optionId => {
                const checkbox = document.querySelector(`input[type="checkbox"][value="${optionId}"]`);
                if (checkbox) checkbox.checked = true;
            });
        }
    }

    async saveAnswer() {
        if (!this.currentAttempt) return;

        const question = await this.getCurrentQuestion();
        if (!question) return;

        let answerData = {};
        const questionId = question.id;

        if (question.type === 'OPEN_ANSWER') {
            const textarea = document.querySelector('textarea[data-question-id="' + questionId + '"]');
            if (!textarea || !textarea.value.trim()) {
                this.showSaveStatus('Введите ответ', 'error');
                return;
            }
            answerData = { openAnswerText: textarea.value.trim() };
        } else {
            const selectedOptions = this.getSelectedOptions(question);
            if (selectedOptions.length === 0) {
                this.showSaveStatus('Выберите хотя бы один вариант', 'error');
                return;
            }
            answerData = { selectedOptionIds: selectedOptions };
        }

        try {
            await this.api.submitAnswer(this.currentAttempt, questionId, answerData);
            
            // Сохраняем ответ в sessionStorage
            const savedAnswers = JSON.parse(sessionStorage.getItem('savedAnswers') || '{}');
            savedAnswers[questionId] = answerData;
            sessionStorage.setItem('savedAnswers', JSON.stringify(savedAnswers));
            
            this.showSaveStatus('Ответ сохранен', 'success');
            
            // Обновляем прогресс бар
            this.loadTest();
            
        } catch (error) {
            this.showSaveStatus('Ошибка сохранения: ' + error.message, 'error');
        }
    }

    async getCurrentQuestion() {
        try {
            const response = await this.api.getTestProgress(this.currentAttempt);
            return response.data.currentQuestion;
        } catch (error) {
            console.error('Ошибка получения вопроса:', error);
            return null;
        }
    }

    getSelectedOptions(question) {
        if (question.type === 'SINGLE_CHOICE') {
            const radio = document.querySelector('input[type="radio"][name="answer"]:checked');
            return radio ? [parseInt(radio.value)] : [];
        } else if (question.type === 'MULTIPLE_CHOICE') {
            const checkboxes = document.querySelectorAll('input[type="checkbox"]:checked');
            return Array.from(checkboxes).map(cb => parseInt(cb.value));
        }
        return [];
    }

    showSaveStatus(message, type = 'info') {
        const statusDiv = document.getElementById('save-status');
        if (!statusDiv) return;

        statusDiv.innerHTML = `
            <div class="${type === 'error' ? 'error-message' : 'success-message'}" style="margin: 0;">
                ${message}
            </div>
        `;

        setTimeout(() => {
            statusDiv.innerHTML = '';
        }, 3000);
    }

    async nextQuestion() {
        await this.saveAnswer();
        
        try {
            const response = await this.api.getTestProgress(this.currentAttempt);
            const progress = response.data;
            
            if (progress.currentQuestionIndex < progress.totalQuestions - 1) {
                // Переходим к следующему вопросу через обновление страницы
                this.loadTest();
            }
        } catch (error) {
            console.error('Ошибка перехода:', error);
        }
    }

    async goToQuestion(questionId) {
        // В реальном приложении здесь был бы endpoint для перехода к конкретному вопросу
        alert('Переход к вопросу будет реализован в следующей версии');
    }

    startTimer(minutesLeft) {
        let secondsLeft = minutesLeft * 60;
        
        const updateTimer = () => {
            const minutes = Math.floor(secondsLeft / 60);
            const seconds = secondsLeft % 60;
            
            const timerElement = document.getElementById('timer');
            if (timerElement) {
                timerElement.textContent = `Осталось времени: ${minutes}:${seconds.toString().padStart(2, '0')}`;
            }
            
            if (secondsLeft <= 0) {
                clearInterval(this.timerInterval);
                this.autoCompleteTest();
            }
            
            secondsLeft--;
        };
        
        updateTimer();
        this.timerInterval = setInterval(updateTimer, 1000);
    }

    async autoSaveProgress() {
        // Автоматически сохраняем текущий ответ
        const question = await this.getCurrentQuestion();
        if (!question) return;

        let answerData = {};
        const questionId = question.id;

        if (question.type === 'OPEN_ANSWER') {
            const textarea = document.querySelector('textarea[data-question-id="' + questionId + '"]');
            if (textarea && textarea.value.trim()) {
                answerData = { openAnswerText: textarea.value.trim() };
            }
        } else {
            const selectedOptions = this.getSelectedOptions(question);
            if (selectedOptions.length > 0) {
                answerData = { selectedOptionIds: selectedOptions };
            }
        }

        if (Object.keys(answerData).length > 0) {
            try {
                await this.api.submitAnswer(this.currentAttempt, questionId, answerData);
                console.log('Автосохранение выполнено');
            } catch (error) {
                console.error('Ошибка автосохранения:', error);
            }
        }
    }

    async completeTest() {
        Utils.confirm('Завершить тест? После завершения изменить ответы будет нельзя.', async () => {
            try {
                await this.api.completeTest(this.currentAttempt);
                
                // Очищаем sessionStorage
                sessionStorage.removeItem('currentAttempt');
                sessionStorage.removeItem('savedAnswers');
                sessionStorage.removeItem('testStartedAt');
                
                if (this.timerInterval) {
                    clearInterval(this.timerInterval);
                }
                
                alert('Тест завершен! Результаты будут доступны после проверки HR.');
                window.location.href = 'employee-dashboard.html';
                
            } catch (error) {
                alert('Ошибка завершения теста: ' + error.message);
            }
        });
    }

    async autoCompleteTest() {
        try {
            await this.api.completeTest(this.currentAttempt);
            
            sessionStorage.removeItem('currentAttempt');
            sessionStorage.removeItem('savedAnswers');
            sessionStorage.removeItem('testStartedAt');
            
            alert('Время вышло! Тест автоматически завершен.');
            window.location.href = 'employee-dashboard.html';
            
        } catch (error) {
            console.error('Ошибка авто-завершения:', error);
        }
    }

    getAttemptStatusLabel(status) {
        const statuses = {
            'IN_PROGRESS': 'В процессе',
            'COMPLETED': 'Завершен',
            'EVALUATING': 'На проверке',
            'EVALUATED': 'Проверен'
        };
        return statuses[status] || status;
    }
}

const employeeService = new EmployeeService();
window.employeeService = employeeService;