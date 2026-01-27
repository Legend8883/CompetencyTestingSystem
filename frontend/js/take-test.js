// Глобальные переменные
let currentAttempt = null;
let testData = null;
let answers = {};
let currentQuestionIndex = 0;
let timeLeft = 0;
let timerInterval = null;
let attemptId = null;

// Проверяем авторизацию
document.addEventListener('DOMContentLoaded', async function() {
    const user = api.utils.checkAuth();
    if (!user) {
        window.location.href = 'index.html';
        return;
    }
    
    if (!api.utils.isEmployee()) {
        api.utils.showNotification('У вас нет прав для доступа к этой странице', 'error');
        window.location.href = 'dashboard-hr.html';
        return;
    }
    
    // Обновляем информацию пользователя
    updateUserInfo();
    
    // Получаем attemptId из URL
    const urlParams = new URLSearchParams(window.location.search);
    attemptId = urlParams.get('attempt');
    
    if (!attemptId) {
        api.utils.showNotification('Не указан ID попытки', 'error');
        window.location.href = 'dashboard-employee.html';
        return;
    }
    
    // Загружаем данные теста
    await loadTestData();
});

// Обновление информации пользователя
function updateUserInfo() {
    const user = api.utils.getCurrentUser();
    if (user) {
        document.getElementById('userName').textContent = `${user.firstName} ${user.lastName}`;
        document.getElementById('userInitials').textContent = 
            `${user.firstName?.[0] || ''}${user.lastName?.[0] || ''}`.toUpperCase();
    }
}

// Загрузка данных теста
async function loadTestData() {
    try {
        // Получаем прогресс теста
        const progress = await api.employee.getProgress(attemptId);
        
        if (!progress) {
            throw new Error('Не удалось загрузить данные теста');
        }
        
        currentAttempt = progress;
        testData = {
            id: progress.testId,
            title: progress.testTitle,
            timeLimitMinutes: progress.timeLeftMinutes
        };
        
        // Инициализируем ответы
        if (progress.questionProgress) {
            progress.questionProgress.forEach(q => {
                answers[q.questionId] = {
                    questionId: q.questionId,
                    answered: q.answered || false,
                    visited: q.visited || false
                };
            });
        }
        
        // Обновляем инструкции
        updateInstructions();
        
        // Если тест уже начат, показываем экран теста
        if (progress.startedAt) {
            document.getElementById('instructionsScreen').classList.add('hidden');
            document.getElementById('testScreen').classList.remove('hidden');
            
            // Рассчитываем оставшееся время
            const startedAt = new Date(progress.startedAt);
            const autoSubmitAt = new Date(progress.autoSubmitAt);
            timeLeft = Math.max(0, Math.floor((autoSubmitAt - new Date()) / 1000));
            
            startTimer();
            
            // Показываем текущий вопрос
            if (progress.currentQuestion) {
                showQuestion(progress.currentQuestion, progress.currentQuestionIndex || 0);
            }
        }
        
    } catch (error) {
        console.error('Error loading test data:', error);
        api.utils.showNotification(`Ошибка загрузки теста: ${error.message}`, 'error');
        setTimeout(() => {
            window.location.href = 'dashboard-employee.html';
        }, 2000);
    }
}

// Обновление инструкций
function updateInstructions() {
    if (!currentAttempt) return;
    
    document.getElementById('testTitle').textContent = currentAttempt.testTitle;
    document.getElementById('totalQuestionsCount').textContent = currentAttempt.totalQuestions;
    document.getElementById('testTimeLimit').textContent = currentAttempt.timeLeftMinutes;
    document.getElementById('testPassingScore').textContent = currentAttempt.passingScore || 70;
}

// Начало теста
async function startTest() {
    try {
        // Загружаем первый вопрос
        if (currentAttempt.currentQuestion) {
            document.getElementById('instructionsScreen').classList.add('hidden');
            document.getElementById('testScreen').classList.remove('hidden');
            
            // Рассчитываем время
            const startedAt = new Date(currentAttempt.startedAt);
            const autoSubmitAt = new Date(currentAttempt.autoSubmitAt);
            timeLeft = Math.max(0, Math.floor((autoSubmitAt - new Date()) / 1000));
            
            startTimer();
            showQuestion(currentAttempt.currentQuestion, currentAttempt.currentQuestionIndex || 0);
        }
        
    } catch (error) {
        console.error('Error starting test:', error);
        api.utils.showNotification('Ошибка начала теста', 'error');
    }
}

// Таймер
function startTimer() {
    updateTimerDisplay();
    
    timerInterval = setInterval(() => {
        timeLeft--;
        updateTimerDisplay();
        
        if (timeLeft <= 0) {
            clearInterval(timerInterval);
            autoSubmitTest();
        }
    }, 1000);
}

function updateTimerDisplay() {
    const minutes = Math.floor(timeLeft / 60);
    const seconds = timeLeft % 60;
    document.getElementById('timer').textContent = 
        `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
}

// Показать вопрос
function showQuestion(question, index) {
    if (!question) return;
    
    currentQuestionIndex = index;
    
    // Обновляем заголовок
    document.getElementById('currentTestTitle').textContent = currentAttempt.testTitle;
    document.getElementById('currentQuestion').textContent = index + 1;
    document.getElementById('totalQuestions').textContent = currentAttempt.totalQuestions;
    
    // Обновляем текст вопроса
    document.getElementById('questionText').textContent = question.text;
    
    // Показываем соответствующий тип вопроса
    if (question.type === 'SINGLE_CHOICE' || question.type === 'MULTIPLE_CHOICE') {
        document.getElementById('choiceQuestion').classList.remove('hidden');
        document.getElementById('openQuestion').classList.add('hidden');
        showChoiceQuestion(question);
    } else if (question.type === 'OPEN_ANSWER') {
        document.getElementById('choiceQuestion').classList.add('hidden');
        document.getElementById('openQuestion').classList.remove('hidden');
        showOpenQuestion(question);
    }
    
    // Обновляем прогресс
    updateProgress();
    
    // Обновляем кнопки навигации
    updateButtons();
}

function showChoiceQuestion(question) {
    const container = document.getElementById('choiceQuestion');
    container.innerHTML = '';
    
    question.options.forEach((option, optionIndex) => {
        const optionDiv = document.createElement('div');
        optionDiv.className = 'option-item';
        
        if (question.type === 'SINGLE_CHOICE') {
            optionDiv.innerHTML = `
                <div class="option-radio">
                    <input type="radio" name="choice" value="${option.id}" 
                           id="option_${option.id}">
                </div>
                <label class="option-content" for="option_${option.id}">
                    ${option.text}
                </label>
            `;
            
            optionDiv.querySelector('input').onchange = function() {
                saveChoiceAnswer(question.id, [option.id], question.type);
            };
        } else {
            optionDiv.innerHTML = `
                <div class="option-radio">
                    <input type="checkbox" name="choice" value="${option.id}" 
                           id="option_${option.id}">
                </div>
                <label class="option-content" for="option_${option.id}">
                    ${option.text}
                </label>
            `;
            
            optionDiv.querySelector('input').onchange = function() {
                const selected = Array.from(container.querySelectorAll('input:checked'))
                    .map(input => parseInt(input.value));
                saveChoiceAnswer(question.id, selected, question.type);
            };
        }
        
        optionDiv.onclick = (e) => {
            if (e.target.type !== 'radio' && e.target.type !== 'checkbox') {
                const input = optionDiv.querySelector('input');
                input.checked = !input.checked;
                input.dispatchEvent(new Event('change'));
            }
        };
        
        container.appendChild(optionDiv);
    });
}

function showOpenQuestion(question) {
    const textarea = document.getElementById('openAnswer');
    textarea.value = '';
    textarea.oninput = function() {
        saveOpenAnswer(question.id, this.value);
        document.getElementById('answerLength').textContent = this.value.length;
    };
    document.getElementById('answerLength').textContent = 0;
}

// Сохранение ответов
async function saveChoiceAnswer(questionId, selectedOptionIds, questionType) {
    try {
        const answerData = {
            questionId: questionId,
            selectedOptionIds: selectedOptionIds
        };
        
        await api.employee.submitAnswer(attemptId, questionId, answerData);
        
        // Обновляем локальное состояние
        if (answers[questionId]) {
            answers[questionId].answered = selectedOptionIds.length > 0;
        }
        
        updateProgress();
        
    } catch (error) {
        console.error('Error saving choice answer:', error);
        api.utils.showNotification('Ошибка сохранения ответа', 'error');
    }
}

async function saveOpenAnswer(questionId, answerText) {
    try {
        const answerData = {
            questionId: questionId,
            openAnswerText: answerText
        };
        
        await api.employee.submitAnswer(attemptId, questionId, answerData);
        
        // Обновляем локальное состояние
        if (answers[questionId]) {
            answers[questionId].answered = answerText.trim().length > 0;
        }
        
    } catch (error) {
        console.error('Error saving open answer:', error);
        api.utils.showNotification('Ошибка сохранения ответа', 'error');
    }
}

// Обновление прогресса
function updateProgress() {
    if (!currentAttempt || !currentAttempt.questionProgress) return;
    
    const answeredCount = currentAttempt.questionProgress.filter(q => q.answered).length;
    const progress = (answeredCount / currentAttempt.totalQuestions) * 100;
    
    document.getElementById('progressFill').style.width = `${progress}%`;
    document.getElementById('progressPercent').textContent = `${Math.round(progress)}%`;
}

function updateButtons() {
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    const submitBtn = document.getElementById('submitBtn');
    
    // В реальном приложении здесь нужно загружать следующий/предыдущий вопрос
    // Для демо просто обновляем состояние кнопок
    
    prevBtn.disabled = currentQuestionIndex === 0;
    
    if (currentQuestionIndex === (currentAttempt.totalQuestions - 1)) {
        nextBtn.classList.add('hidden');
        submitBtn.classList.remove('hidden');
    } else {
        nextBtn.classList.remove('hidden');
        submitBtn.classList.add('hidden');
    }
}

// Навигация по вопросам
async function prevQuestion() {
    try {
        // В реальном приложении нужно запросить предыдущий вопрос с сервера
        // Для демо просто уменьшаем индекс
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--;
            // Здесь должен быть запрос к API для получения вопроса
        }
    } catch (error) {
        console.error('Error navigating to previous question:', error);
    }
}

async function nextQuestion() {
    try {
        // В реальном приложении нужно запросить следующий вопрос с сервера
        if (currentQuestionIndex < (currentAttempt.totalQuestions - 1)) {
            currentQuestionIndex++;
            // Здесь должен быть запрос к API для получения вопроса
        }
    } catch (error) {
        console.error('Error navigating to next question:', error);
    }
}

// Завершение теста
async function submitTest() {
    openModal(
        'Вы уверены, что хотите завершить тест? После завершения изменить ответы будет невозможно.',
        confirmSubmit
    );
}

async function confirmSubmit() {
    try {
        clearInterval(timerInterval);
        
        await api.employee.completeTest(attemptId);
        
        api.utils.showNotification('Тест завершен! Результаты будут доступны после проверки.', 'success');
        
        setTimeout(() => {
            window.location.href = 'dashboard-employee.html';
        }, 2000);
        
    } catch (error) {
        console.error('Error submitting test:', error);
        api.utils.showNotification(`Ошибка завершения теста: ${error.message}`, 'error');
    }
}

// Автоматическое завершение при истечении времени
async function autoSubmitTest() {
    try {
        await api.employee.completeTest(attemptId);
        
        api.utils.showNotification('Время вышло! Тест автоматически завершен.', 'warning');
        
        setTimeout(() => {
            window.location.href = 'dashboard-employee.html';
        }, 2000);
        
    } catch (error) {
        console.error('Error auto-submitting test:', error);
    }
}

// Модальное окно
function openModal(message, confirmCallback) {
    document.getElementById('modalMessage').textContent = message;
    document.getElementById('modalConfirmBtn').onclick = function() {
        closeModal();
        confirmCallback();
    };
    document.getElementById('confirmationModal').classList.remove('hidden');
}

function closeModal() {
    document.getElementById('confirmationModal').classList.add('hidden');
}

// Закрытие модального окна
document.addEventListener('click', function(event) {
    const modal = document.getElementById('confirmationModal');
    if (event.target === modal) {
        closeModal();
    }
});

// Предотвращаем закрытие страницы
window.addEventListener('beforeunload', function(e) {
    if (timerInterval && timeLeft > 0) {
        e.preventDefault();
        e.returnValue = '';
        return 'Тест все еще в процессе. Вы уверены, что хотите уйти?';
    }
});