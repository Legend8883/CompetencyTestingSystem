// Проверяем авторизацию
document.addEventListener('DOMContentLoaded', async function() {
    const user = api.utils.checkAuth();
    if (!user) {
        window.location.href = 'index.html';
        return;
    }
    
    if (!api.utils.isHR()) {
        api.utils.showNotification('У вас нет прав для доступа к этой странице', 'error');
        window.location.href = 'dashboard-employee.html';
        return;
    }
    
    // Обновляем информацию пользователя
    updateUserInfo();
    
    // Загружаем данные
    await loadData();
    
    // Проверяем, есть ли конкретная попытка в URL
    const urlParams = new URLSearchParams(window.location.search);
    const attemptId = urlParams.get('attempt');
    if (attemptId) {
        await loadAttemptForReview(parseInt(attemptId));
    }
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

// Загрузка данных
async function loadData() {
    try {
        await Promise.all([
            loadOpenAnswers(),
            loadPendingAttempts()
        ]);
    } catch (error) {
        console.error('Error loading data:', error);
        api.utils.showNotification('Ошибка загрузки данных', 'error');
    }
}

// Загрузка открытых вопросов для проверки
async function loadOpenAnswers() {
    try {
        const openAnswers = await api.hr.getOpenAnswers();
        const container = document.getElementById('openAnswersList');
        
        if (!container) return;
        
        if (openAnswers.length === 0) {
            container.innerHTML = '<p class="text-center">Нет открытых вопросов для проверки</p>';
            return;
        }
        
        // Группируем по попыткам
        const attemptsMap = {};
        openAnswers.forEach(answer => {
            if (!attemptsMap[answer.attemptId]) {
                attemptsMap[answer.attemptId] = {
                    attemptId: answer.attemptId,
                    testTitle: answer.testTitle,
                    employeeName: answer.employeeName,
                    openAnswers: []
                };
            }
            attemptsMap[answer.attemptId].openAnswers.push(answer);
        });
        
        const attempts = Object.values(attemptsMap);
        
        container.innerHTML = attempts.map(attempt => `
            <div class="attempt-item" onclick="loadAttemptForReview(${attempt.attemptId})">
                <div class="attempt-header">
                    <h5>${attempt.testTitle}</h5>
                    <span class="badge bg-warning">${attempt.openAnswers.length} вопросов</span>
                </div>
                <div class="attempt-body">
                    <p><strong>Сотрудник:</strong> ${attempt.employeeName}</p>
                    <p class="small text-muted">
                        ${attempt.openAnswers.length} открытых вопросов требуют проверки
                    </p>
                </div>
                <div class="attempt-footer">
                    <button class="btn btn-sm btn-primary" onclick="event.stopPropagation(); loadAttemptForReview(${attempt.attemptId})">
                        Начать проверку
                    </button>
                </div>
            </div>
        `).join('');
        
    } catch (error) {
        console.error('Error loading open answers:', error);
        const container = document.getElementById('openAnswersList');
        if (container) {
            container.innerHTML = '<p class="text-center text-danger">Ошибка загрузки вопросов</p>';
        }
    }
}

// Загрузка ожидающих проверки попыток
async function loadPendingAttempts() {
    try {
        const attempts = await api.hr.getAttemptsForEvaluation();
        const container = document.getElementById('pendingAttemptsList');
        
        if (!container) return;
        
        if (attempts.length === 0) {
            container.innerHTML = '<p class="text-center">Нет попыток, ожидающих проверки</p>';
            return;
        }
        
        container.innerHTML = attempts.map(attempt => `
            <div class="attempt-card">
                <div class="attempt-card-header">
                    <div class="attempt-card-title">${attempt.testTitle}</div>
                    <div class="attempt-card-status">Ожидает проверки</div>
                </div>
                <div class="attempt-card-body">
                    <p><strong>Сотрудник:</strong> ${attempt.employeeName}</p>
                    <p><strong>Завершено:</strong> ${formatDate(attempt.completedAt)}</p>
                    <p><strong>Автооценка:</strong> ${attempt.autoScore || 0}%</p>
                    <p><strong>Вопросов для проверки:</strong> ${attempt.openQuestionsCount || 0}</p>
                </div>
                <div class="attempt-card-footer">
                    <button class="btn btn-sm btn-primary" onclick="loadAttemptForReview(${attempt.id})">
                        Проверить
                    </button>
                    <button class="btn btn-sm btn-secondary" onclick="viewAttemptDetails(${attempt.id})">
                        Подробнее
                    </button>
                </div>
            </div>
        `).join('');
        
    } catch (error) {
        console.error('Error loading pending attempts:', error);
    }
}

// Загрузка конкретной попытки для проверки
let currentAttempt = null;
let currentOpenAnswers = [];

async function loadAttemptForReview(attemptId) {
    try {
        // Показываем загрузку
        document.getElementById('reviewContent').classList.add('hidden');
        document.getElementById('loadingReview').classList.remove('hidden');
        
        // Загружаем открытые вопросы для этой попытки
        const allOpenAnswers = await api.hr.getOpenAnswers();
        currentOpenAnswers = allOpenAnswers.filter(answer => answer.attemptId === attemptId);
        
        if (currentOpenAnswers.length === 0) {
            api.utils.showNotification('Нет открытых вопросов для проверки в этой попытке', 'warning');
            document.getElementById('loadingReview').classList.add('hidden');
            return;
        }
        
        // Получаем информацию о попытке из первого вопроса
        currentAttempt = {
            id: attemptId,
            testTitle: currentOpenAnswers[0].testTitle,
            employeeName: currentOpenAnswers[0].employeeName
        };
        
        // Обновляем интерфейс
        document.getElementById('reviewTitle').textContent = 
            `Проверка: ${currentAttempt.testTitle} - ${currentAttempt.employeeName}`;
        
        // Показываем вопросы
        showQuestionsForReview();
        
        // Прячем загрузку, показываем контент
        document.getElementById('loadingReview').classList.add('hidden');
        document.getElementById('reviewContent').classList.remove('hidden');
        document.getElementById('emptyReview').classList.add('hidden');
        
        // Выделяем выбранную попытку
        document.querySelectorAll('.attempt-item').forEach(item => {
            item.classList.remove('selected');
        });
        
    } catch (error) {
        console.error('Error loading attempt for review:', error);
        api.utils.showNotification('Ошибка загрузки попытки', 'error');
        document.getElementById('loadingReview').classList.add('hidden');
    }
}

// Показать вопросы для проверки
function showQuestionsForReview() {
    const container = document.getElementById('questionsReview');
    
    container.innerHTML = currentOpenAnswers.map((answer, index) => `
        <div class="question-review" id="question-${answer.id}">
            <div class="question-review-header">
                <h5>Вопрос ${index + 1} (максимум ${answer.maxScore} баллов)</h5>
                <div class="question-score-status">
                    ${answer.assignedScore !== null ? 
                        `<span class="badge bg-success">Оценено: ${answer.assignedScore}/${answer.maxScore}</span>` :
                        `<span class="badge bg-warning">Требует оценки</span>`
                    }
                </div>
            </div>
            
            <div class="question-review-body">
                <div class="question-text">
                    <strong>Вопрос:</strong>
                    <p>${answer.questionText}</p>
                </div>
                
                <div class="answer-text">
                    <strong>Ответ сотрудника:</strong>
                    <div class="answer-content">${answer.openAnswerText || 'Нет ответа'}</div>
                </div>
                
                <div class="evaluation-form">
                    <div class="form-group">
                        <label for="score-${answer.id}">Оценка (0-${answer.maxScore} баллов):</label>
                        <div class="score-input-group">
                            <input type="number" 
                                   id="score-${answer.id}" 
                                   class="form-control score-input" 
                                   min="0" 
                                   max="${answer.maxScore}" 
                                   value="${answer.assignedScore || ''}"
                                   placeholder="Введите оценку">
                            <span class="score-max">/ ${answer.maxScore}</span>
                        </div>
                        <small class="text-muted">
                            Автоматическая оценка: ${answer.autoScore || 0} баллов
                        </small>
                    </div>
                    
                    <div class="form-group">
                        <label for="comment-${answer.id}">Комментарий (необязательно):</label>
                        <textarea id="comment-${answer.id}" 
                                  class="form-control" 
                                  rows="2" 
                                  placeholder="Добавьте комментарий к оценке">${answer.comment || ''}</textarea>
                    </div>
                </div>
            </div>
            
            <div class="question-review-footer">
                <button class="btn btn-sm btn-success" onclick="saveQuestionScore(${answer.id})">
                    Сохранить оценку
                </button>
            </div>
        </div>
    `).join('');
}

// Сохранение оценки вопроса
async function saveQuestionScore(answerId) {
    const scoreInput = document.getElementById(`score-${answerId}`);
    const score = parseInt(scoreInput.value);
    
    if (isNaN(score) || score < 0) {
        api.utils.showNotification('Введите корректную оценку', 'warning');
        scoreInput.focus();
        return;
    }
    
    const maxScore = parseInt(scoreInput.max);
    if (score > maxScore) {
        api.utils.showNotification(`Оценка не может превышать ${maxScore}`, 'warning');
        return;
    }
    
    try {
        await api.hr.evaluateAnswer(answerId, score);
        
        // Обновляем UI
        const questionElement = document.getElementById(`question-${answerId}`);
        const statusBadge = questionElement.querySelector('.question-score-status');
        statusBadge.innerHTML = `<span class="badge bg-success">Оценено: ${score}/${maxScore}</span>`;
        
        api.utils.showNotification('Оценка сохранена', 'success');
        
        // Проверяем, все ли вопросы оценены
        checkIfAllEvaluated();
        
    } catch (error) {
        console.error('Error saving question score:', error);
        api.utils.showNotification(`Ошибка сохранения оценки: ${error.message}`, 'error');
    }
}

// Проверка, все ли вопросы оценены
function checkIfAllEvaluated() {
    const allScores = currentOpenAnswers.map(answer => {
        const scoreInput = document.getElementById(`score-${answer.id}`);
        return scoreInput ? parseInt(scoreInput.value) || 0 : 0;
    });
    
    const allEvaluated = allScores.every(score => score > 0);
    
    const completeBtn = document.getElementById('completeEvaluationBtn');
    if (completeBtn) {
        completeBtn.disabled = !allEvaluated;
        if (allEvaluated) {
            completeBtn.classList.remove('btn-secondary');
            completeBtn.classList.add('btn-success');
        }
    }
}

// Завершение проверки всей попытки
async function completeEvaluation() {
    if (!currentAttempt) return;
    
    // Проверяем, что все вопросы оценены
    const allScores = currentOpenAnswers.map(answer => {
        const scoreInput = document.getElementById(`score-${answer.id}`);
        return scoreInput ? parseInt(scoreInput.value) || 0 : 0;
    });
    
    const allEvaluated = allScores.every(score => score > 0);
    
    if (!allEvaluated) {
        api.utils.showNotification('Оцените все вопросы перед завершением проверки', 'warning');
        return;
    }
    
    if (!confirm('Завершить проверку этой попытки? После этого результаты будут отправлены сотруднику.')) {
        return;
    }
    
    try {
        await api.hr.completeEvaluation(currentAttempt.id);
        
        api.utils.showNotification('Проверка завершена! Результаты отправлены сотруднику.', 'success');
        
        // Обновляем данные
        await loadData();
        
        // Сбрасываем текущую проверку
        document.getElementById('reviewContent').classList.add('hidden');
        document.getElementById('emptyReview').classList.remove('hidden');
        currentAttempt = null;
        currentOpenAnswers = [];
        
    } catch (error) {
        console.error('Error completing evaluation:', error);
        api.utils.showNotification(`Ошибка завершения проверки: ${error.message}`, 'error');
    }
}

// Просмотр деталей попытки
async function viewAttemptDetails(attemptId) {
    try {
        // Можно открыть модальное окно с деталями
        // Для простоты пока просто показываем alert
        const attempts = await api.hr.getAttemptsForEvaluation();
        const attempt = attempts.find(a => a.id === attemptId);
        
        if (attempt) {
            alert(
                `Детали попытки:\n\n` +
                `Тест: ${attempt.testTitle}\n` +
                `Сотрудник: ${attempt.employeeName}\n` +
                `Завершено: ${formatDate(attempt.completedAt)}\n` +
                `Автооценка: ${attempt.autoScore || 0}%\n` +
                `Статус: ${getStatusText(attempt.status)}\n` +
                `Вопросов для проверки: ${attempt.openQuestionsCount || 0}`
            );
        }
    } catch (error) {
        console.error('Error viewing attempt details:', error);
    }
}

// Вспомогательные функции
function formatDate(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('ru-RU', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function getStatusText(status) {
    switch (status) {
        case 'IN_PROGRESS': return 'В процессе';
        case 'COMPLETED': return 'Завершен (только выбор)';
        case 'EVALUATING': return 'Ожидает проверки';
        case 'EVALUATED': return 'Проверен';
        default: return status;
    }
}

// Добавляем стили для этой страницы
const styles = document.createElement('style');
styles.textContent = `
    .attempt-item {
        background-color: var(--white);
        border-radius: var(--radius);
        padding: 1.5rem;
        margin-bottom: 1rem;
        border: 2px solid var(--medium-gray);
        cursor: pointer;
        transition: all 0.2s;
    }
    
    .attempt-item:hover {
        border-color: var(--primary-blue);
        box-shadow: var(--shadow);
    }
    
    .attempt-item.selected {
        border-color: var(--primary-blue);
        background-color: var(--light-blue);
    }
    
    .attempt-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 1rem;
    }
    
    .attempt-body {
        margin-bottom: 1rem;
    }
    
    .attempt-footer {
        display: flex;
        justify-content: flex-end;
    }
    
    .attempt-card {
        background-color: var(--white);
        border-radius: var(--radius);
        padding: 1.5rem;
        margin-bottom: 1rem;
        box-shadow: var(--shadow);
    }
    
    .attempt-card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1rem;
        padding-bottom: 0.75rem;
        border-bottom: 1px solid var(--medium-gray);
    }
    
    .attempt-card-title {
        font-size: 1.125rem;
        font-weight: 600;
        color: var(--text-dark);
    }
    
    .attempt-card-status {
        background-color: var(--warning);
        color: white;
        padding: 0.25rem 0.75rem;
        border-radius: 20px;
        font-size: 0.75rem;
        font-weight: 600;
    }
    
    .attempt-card-body p {
        margin-bottom: 0.5rem;
    }
    
    .attempt-card-footer {
        display: flex;
        gap: 0.5rem;
        margin-top: 1rem;
    }
    
    .question-review {
        background-color: var(--white);
        border-radius: var(--radius);
        padding: 1.5rem;
        margin-bottom: 1.5rem;
        box-shadow: var(--shadow);
    }
    
    .question-review-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1rem;
        padding-bottom: 1rem;
        border-bottom: 1px solid var(--medium-gray);
    }
    
    .question-review-body {
        margin-bottom: 1.5rem;
    }
    
    .question-text, .answer-text {
        margin-bottom: 1.5rem;
    }
    
    .answer-content {
        background-color: var(--light-gray);
        border-radius: var(--radius);
        padding: 1rem;
        margin-top: 0.5rem;
        white-space: pre-wrap;
        font-family: 'Courier New', monospace;
        font-size: 0.9rem;
    }
    
    .score-input-group {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        max-width: 200px;
    }
    
    .score-input {
        text-align: center;
    }
    
    .score-max {
        color: var(--dark-gray);
        font-weight: 600;
    }
    
    .evaluation-form {
        background-color: var(--light-gray);
        border-radius: var(--radius);
        padding: 1rem;
        margin-top: 1rem;
    }
    
    .question-review-footer {
        display: flex;
        justify-content: flex-end;
    }
`;
document.head.appendChild(styles);