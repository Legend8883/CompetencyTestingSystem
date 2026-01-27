// Проверяем авторизацию
document.addEventListener('DOMContentLoaded', async function() {
    const user = api.utils.checkAuth();
    if (!user) {
        window.location.href = 'index.html';
        return;
    }
    
    // Обновляем информацию пользователя
    updateUserInfo();
    
    // Настраиваем страницу в зависимости от роли
    setupPageForRole();
    
    // Загружаем данные
    await loadResults();
    
    // Инициализируем графики если нужно
    if (api.utils.isHR()) {
        initCharts();
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

// Настройка страницы для роли
function setupPageForRole() {
    const isHR = api.utils.isHR();
    
    if (isHR) {
        // Настройки для HR
        document.getElementById('logoText').textContent = 'ТестКомп | Результаты HR';
        document.getElementById('resultsTitle').textContent = 'Результаты тестирования';
        document.getElementById('resultsSubtitle').textContent = 'Все результаты сотрудников';
        document.getElementById('dashboardLink').href = 'dashboard-hr.html';
        document.getElementById('mainLink').href = 'dashboard-hr.html';
        document.getElementById('hrDashboard').classList.remove('hidden');
        document.getElementById('employeeDashboard').classList.add('hidden');
        
        // Показываем фильтр по сотрудникам
        document.getElementById('employeeFilterSection').classList.remove('hidden');
    } else {
        // Настройки для Employee
        document.getElementById('logoText').textContent = 'ТестКомп | Мои результаты';
        document.getElementById('resultsTitle').textContent = 'Мои результаты';
        document.getElementById('resultsSubtitle').textContent = 'История прохождения тестов';
        document.getElementById('dashboardLink').href = 'dashboard-employee.html';
        document.getElementById('mainLink').href = 'dashboard-employee.html';
        document.getElementById('hrDashboard').classList.add('hidden');
        document.getElementById('employeeDashboard').classList.remove('hidden');
        
        // Скрываем фильтр по сотрудникам
        document.getElementById('employeeFilterSection').classList.add('hidden');
    }
}

// Загрузка результатов
async function loadResults() {
    try {
        let attempts;
        
        if (api.utils.isHR()) {
            // Для HR загружаем все попытки
            // В реальном API нужно добавить endpoint для получения всех попыток
            // Пока будем использовать данные из localStorage или демо-данные
            attempts = await loadAllAttemptsForHR();
            
            // Загружаем сотрудников для фильтра
            await loadEmployeesForFilter();
        } else {
            // Для Employee загружаем только свои попытки
            attempts = await api.employee.getMyAttempts();
        }
        
        // Фильтруем по параметрам URL
        const urlParams = new URLSearchParams(window.location.search);
        const testId = urlParams.get('test');
        const employeeId = urlParams.get('employee');
        
        if (testId) {
            attempts = attempts.filter(a => a.testId == testId);
            document.getElementById('testFilter').value = testId;
        }
        
        if (employeeId) {
            attempts = attempts.filter(a => a.userId == employeeId);
            document.getElementById('employeeFilter').value = employeeId;
        }
        
        // Отображаем результаты
        displayResults(attempts);
        
        // Заполняем фильтр тестов
        populateTestFilter(attempts);
        
    } catch (error) {
        console.error('Error loading results:', error);
        api.utils.showNotification('Ошибка загрузки результатов', 'error');
        
        const container = document.getElementById('resultsList');
        if (container) {
            container.innerHTML = `
                <div class="text-center text-danger">
                    <p>Ошибка загрузки результатов</p>
                    <button class="btn btn-secondary mt-2" onclick="loadResults()">Повторить</button>
                </div>
            `;
        }
    }
}

// Загрузка всех попыток для HR (временное решение)
async function loadAllAttemptsForHR() {
    try {
        // В реальном приложении здесь должен быть API endpoint
        // Например: GET /hr/results/all
        // Пока возвращаем пустой массив
        return [];
        
    } catch (error) {
        console.error('Error loading all attempts:', error);
        return [];
    }
}

// Загрузка сотрудников для фильтра
async function loadEmployeesForFilter() {
    try {
        const employees = await api.hr.getEmployees();
        const filter = document.getElementById('employeeFilter');
        
        if (filter) {
            filter.innerHTML = `
                <option value="">Все сотрудники</option>
                ${employees.map(employee => `
                    <option value="${employee.id}">
                        ${employee.firstName} ${employee.lastName} (${employee.email})
                    </option>
                `).join('')}
            `;
        }
        
    } catch (error) {
        console.error('Error loading employees for filter:', error);
    }
}

// Заполнение фильтра тестов
function populateTestFilter(attempts) {
    const testFilter = document.getElementById('testFilter');
    const uniqueTests = [...new Map(attempts.map(a => [a.testId, a.testTitle])).entries()];
    
    testFilter.innerHTML = `
        <option value="">Все тесты</option>
        ${uniqueTests.map(([id, title]) => `
            <option value="${id}">${title}</option>
        `).join('')}
    `;
}

// Отображение результатов
function displayResults(attempts) {
    const container = document.getElementById('resultsList');
    const noResults = document.getElementById('noResults');
    
    if (!container) return;
    
    if (attempts.length === 0) {
        container.innerHTML = '';
        noResults.classList.remove('hidden');
        return;
    }
    
    noResults.classList.add('hidden');
    
    // Сортируем по дате (сначала новые)
    attempts.sort((a, b) => new Date(b.completedAt || b.startedAt) - new Date(a.completedAt || a.startedAt));
    
    container.innerHTML = attempts.map(attempt => {
        const isHR = api.utils.isHR();
        const statusClass = getStatusClass(attempt.status, attempt.passed);
        const scoreClass = getScoreClass(attempt.scorePercent);
        
        return `
            <div class="result-card ${statusClass}" 
                 data-status="${attempt.status}" 
                 data-test-id="${attempt.testId}"
                 data-employee-id="${attempt.userId}"
                 data-passed="${attempt.passed}">
                
                <div class="result-header">
                    <div class="result-title">
                        <h4>${attempt.testTitle}</h4>
                        ${isHR ? `<p class="result-employee">Сотрудник: ${attempt.employeeName}</p>` : ''}
                    </div>
                    <div class="result-status">
                        ${getStatusBadge(attempt.status, attempt.passed)}
                    </div>
                </div>
                
                <div class="result-body">
                    <div class="row">
                        <div class="col-md-8">
                            <div class="result-info">
                                <p><strong>Дата прохождения:</strong> ${formatDate(attempt.completedAt || attempt.startedAt)}</p>
                                <p><strong>Время затрачено:</strong> ${formatTime(attempt.timeSpent)}</p>
                                ${attempt.checkedBy ? `<p><strong>Проверил:</strong> ${attempt.checkedBy}</p>` : ''}
                                ${attempt.checkedAt ? `<p><strong>Дата проверки:</strong> ${formatDate(attempt.checkedAt)}</p>` : ''}
                            </div>
                        </div>
                        <div class="col-md-4 text-center">
                            <div class="score-circle ${scoreClass}">
                                ${attempt.scorePercent || 0}%
                            </div>
                            <div class="score-details mt-2">
                                <p>${attempt.score || 0}/${attempt.maxScore || 100} баллов</p>
                                <p>Проходной балл: ${attempt.passingScore || 70}%</p>
                            </div>
                        </div>
                    </div>
                    
                    <div class="result-details hidden" id="details-${attempt.id}">
                        <hr>
                        <h5>Детальная информация:</h5>
                        <div class="row">
                            <div class="col-md-6">
                                <p><strong>Статус:</strong> ${getStatusText(attempt.status)}</p>
                                <p><strong>Автооценка:</strong> ${attempt.autoScore || 0}%</p>
                                <p><strong>Ручная оценка:</strong> ${attempt.manualScore || 0}%</p>
                            </div>
                            <div class="col-md-6">
                                ${attempt.feedback ? `
                                    <p><strong>Обратная связь:</strong></p>
                                    <div class="feedback-text">${attempt.feedback}</div>
                                ` : ''}
                            </div>
                        </div>
                    </div>
                </div>
                
                <div class="result-footer">
                    <button class="btn btn-sm btn-secondary" onclick="toggleDetails(${attempt.id})">
                        Показать детали
                    </button>
                    ${isHR && attempt.status === 'EVALUATING' ? `
                        <button class="btn btn-sm btn-primary" onclick="reviewAttempt(${attempt.id})">
                            Проверить
                        </button>
                    ` : ''}
                    ${!isHR ? `
                        <button class="btn btn-sm btn-primary" onclick="viewCertificate(${attempt.id})">
                            Сертификат
                        </button>
                    ` : ''}
                </div>
            </div>
        `;
    }).join('');
}

// Вспомогательные функции для оформления
function getStatusClass(status, passed) {
    switch (status) {
        case 'EVALUATING': return 'result-pending';
        case 'EVALUATED': return passed ? 'result-passed' : 'result-failed';
        default: return '';
    }
}

function getScoreClass(scorePercent) {
    if (scorePercent >= 90) return 'score-excellent';
    if (scorePercent >= 75) return 'score-good';
    if (scorePercent >= 60) return 'score-average';
    return 'score-poor';
}

function getStatusBadge(status, passed) {
    switch (status) {
        case 'IN_PROGRESS':
            return '<span class="badge bg-info">В процессе</span>';
        case 'COMPLETED':
            return '<span class="badge bg-secondary">Завершен (только выбор)</span>';
        case 'EVALUATING':
            return '<span class="badge bg-warning">Ожидает проверки</span>';
        case 'EVALUATED':
            return passed ? 
                '<span class="badge bg-success">Пройден</span>' : 
                '<span class="badge bg-danger">Не пройден</span>';
        default:
            return `<span class="badge bg-secondary">${status}</span>`;
    }
}

function getStatusText(status) {
    switch (status) {
        case 'IN_PROGRESS': return 'В процессе';
        case 'COMPLETED': return 'Завершен (только вопросы с выбором)';
        case 'EVALUATING': return 'Ожидает проверки открытых вопросов';
        case 'EVALUATED': return 'Полностью проверен';
        default: return status;
    }
}

function formatDate(dateString) {
    if (!dateString) return 'Не завершен';
    const date = new Date(dateString);
    return date.toLocaleDateString('ru-RU', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function formatTime(seconds) {
    if (!seconds) return 'Не завершен';
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    
    if (hours > 0) {
        return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
    return `${minutes}:${secs.toString().padStart(2, '0')}`;
}

// Фильтрация результатов
function filterResults(filter) {
    const items = document.querySelectorAll('.result-card');
    
    items.forEach(item => {
        const passed = item.dataset.passed === 'true';
        const status = item.dataset.status;
        
        let show = true;
        if (filter === 'passed') show = passed && status === 'EVALUATED';
        if (filter === 'failed') show = !passed && status === 'EVALUATED';
        if (filter === 'pending') show = status === 'EVALUATING';
        if (filter === 'all') show = true;
        
        item.style.display = show ? 'block' : 'none';
    });
    
    // Обновляем активную кнопку
    document.querySelectorAll('.btn-group .btn').forEach(btn => {
        btn.classList.remove('active');
    });
    event.target.classList.add('active');
}

function filterByTest() {
    const testId = document.getElementById('testFilter').value;
    const items = document.querySelectorAll('.result-card');
    
    items.forEach(item => {
        const itemTestId = item.dataset.testId;
        const show = !testId || itemTestId == testId;
        item.style.display = show ? 'block' : 'none';
    });
}

function filterByEmployee() {
    const employeeId = document.getElementById('employeeFilter').value;
    const items = document.querySelectorAll('.result-card');
    
    items.forEach(item => {
        const itemEmployeeId = item.dataset.employeeId;
        const show = !employeeId || itemEmployeeId == employeeId;
        item.style.display = show ? 'block' : 'none';
    });
}

// Переключение деталей
function toggleDetails(attemptId) {
    const details = document.getElementById(`details-${attemptId}`);
    const button = event.target;
    
    if (details.classList.contains('hidden')) {
        details.classList.remove('hidden');
        button.textContent = 'Скрыть детали';
    } else {
        details.classList.add('hidden');
        button.textContent = 'Показать детали';
    }
}

// Действия с попытками
function reviewAttempt(attemptId) {
    window.location.href = `check-results.html?attempt=${attemptId}`;
}

function viewCertificate(attemptId) {
    // В реальном приложении здесь должен быть endpoint для генерации сертификата
    alert('Функция просмотра сертификата будет реализована в следующей версии');
}

// Инициализация графиков для HR
function initCharts() {
    // Пример инициализации Chart.js
    const ctx = document.getElementById('resultsChart');
    if (!ctx) return;
    
    // В реальном приложении здесь нужно загружать статистику с сервера
    const data = {
        labels: ['Янв', 'Фев', 'Мар', 'Апр', 'Май', 'Июн'],
        datasets: [{
            label: 'Пройдено тестов',
            data: [12, 19, 15, 25, 22, 30],
            backgroundColor: 'rgba(59, 130, 246, 0.5)',
            borderColor: 'rgb(59, 130, 246)',
            borderWidth: 2
        }, {
            label: 'Средний балл',
            data: [65, 70, 75, 80, 78, 85],
            backgroundColor: 'rgba(16, 185, 129, 0.5)',
            borderColor: 'rgb(16, 185, 129)',
            borderWidth: 2,
            type: 'line',
            yAxisID: 'y1'
        }]
    };
    
    new Chart(ctx, {
        type: 'bar',
        data: data,
        options: {
            responsive: true,
            scales: {
                y: {
                    beginAtZero: true,
                    title: {
                        display: true,
                        text: 'Количество тестов'
                    }
                },
                y1: {
                    position: 'right',
                    beginAtZero: true,
                    max: 100,
                    title: {
                        display: true,
                        text: 'Средний балл (%)'
                    },
                    grid: {
                        drawOnChartArea: false
                    }
                }
            }
        }
    });
}

// Добавляем стили
const resultsStyles = document.createElement('style');
resultsStyles.textContent = `
    .result-card {
        background-color: var(--white);
        border-radius: var(--radius-lg);
        padding: 1.5rem;
        margin-bottom: 1.5rem;
        box-shadow: var(--shadow);
        border-left: 4px solid var(--medium-gray);
    }
    
    .result-card.result-passed {
        border-left-color: var(--success);
    }
    
    .result-card.result-failed {
        border-left-color: var(--danger);
    }
    
    .result-card.result-pending {
        border-left-color: var(--warning);
    }
    
    .result-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 1rem;
    }
    
    .result-title h4 {
        margin: 0;
        color: var(--text-dark);
    }
    
    .result-employee {
        margin: 0.25rem 0 0 0;
        color: var(--dark-gray);
        font-size: 0.9rem;
    }
    
    .result-status {
        text-align: right;
    }
    
    .result-body {
        margin-bottom: 1rem;
    }
    
    .result-info {
        color: var(--dark-gray);
    }
    
    .result-info p {
        margin-bottom: 0.5rem;
    }
    
    .score-circle {
        width: 80px;
        height: 80px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: bold;
        font-size: 1.25rem;
        margin: 0 auto 0.5rem auto;
    }
    
    .score-excellent {
        background: linear-gradient(135deg, #d1fae5, #10b981);
        color: #065f46;
    }
    
    .score-good {
        background: linear-gradient(135deg, #dbeafe, #3b82f6);
        color: #1e40af;
    }
    
    .score-average {
        background: linear-gradient(135deg, #fef3c7, #f59e0b);
        color: #92400e;
    }
    
    .score-poor {
        background: linear-gradient(135deg, #fee2e2, #ef4444);
        color: #991b1b;
    }
    
    .score-details {
        font-size: 0.875rem;
        color: var(--dark-gray);
    }
    
    .score-details p {
        margin: 0.25rem 0;
    }
    
    .result-details {
        margin-top: 1rem;
        padding-top: 1rem;
    }
    
    .feedback-text {
        background-color: var(--light-gray);
        border-radius: var(--radius);
        padding: 1rem;
        margin-top: 0.5rem;
        font-style: italic;
    }
    
    .result-footer {
        display: flex;
        gap: 0.5rem;
        justify-content: flex-end;
    }
    
    .filters {
        background-color: var(--white);
        border-radius: var(--radius);
        padding: 1rem;
        margin-bottom: 1.5rem;
        box-shadow: var(--shadow);
    }
    
    .btn-group .btn.active {
        background-color: var(--primary-blue);
        color: var(--white);
        border-color: var(--primary-blue);
    }
    
    .chart-container {
        background-color: var(--white);
        border-radius: var(--radius);
        padding: 1.5rem;
        margin-top: 2rem;
        box-shadow: var(--shadow);
    }
`;
document.head.appendChild(resultsStyles);