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
    
    // Загружаем данные дашборда
    await loadDashboardData();
});

// Обновление информации пользователя
function updateUserInfo() {
    const user = api.utils.getCurrentUser();
    if (user) {
        document.getElementById('userName').textContent = `${user.firstName} ${user.lastName}`;
        document.getElementById('userInitials').textContent = 
            `${user.firstName?.[0] || ''}${user.lastName?.[0] || ''}`.toUpperCase();
        document.getElementById('profileInitials').textContent = 
            `${user.firstName?.[0] || ''}${user.lastName?.[0] || ''}`.toUpperCase();
        
        // Заполняем поля профиля
        document.getElementById('profileFirstName').value = user.firstName || '';
        document.getElementById('profileLastName').value = user.lastName || '';
        document.getElementById('profileEmail').value = user.email || '';
    }
}

// Загрузка данных дашборда
async function loadDashboardData() {
    try {
        await Promise.all([
            loadAvailableTests(),
            loadMyAttempts(),
            updateStatistics()
        ]);
    } catch (error) {
        console.error('Error loading dashboard data:', error);
        api.utils.showNotification('Ошибка загрузки данных', 'error');
    }
}

// Загрузка доступных тестов
async function loadAvailableTests() {
    try {
        const tests = await api.employee.getAvailableTests();
        const grid = document.getElementById('availableTestsGrid');
        
        if (!grid) return;
        
        if (tests.length === 0) {
            grid.innerHTML = '<p class="text-center">Нет доступных тестов</p>';
            return;
        }
        
        grid.innerHTML = tests.map(test => {
            const deadline = test.deadline ? new Date(test.deadline) : null;
            const now = new Date();
            const daysLeft = deadline ? Math.ceil((deadline - now) / (1000 * 60 * 60 * 24)) : null;
            
            return `
                <div class="test-card" data-category="${deadline ? 'deadline' : 'new'}">
                    <div class="test-card-header">
                        <div class="test-card-title">${test.title}</div>
                        ${deadline ? `
                            <div class="test-card-deadline ${daysLeft <= 3 ? 'deadline-soon' : ''}">
                                До ${formatDate(deadline)} ${daysLeft > 0 ? `(${daysLeft} дн.)` : ''}
                            </div>
                        ` : ''}
                    </div>
                    <div class="test-card-content">
                        <p>${test.description || 'Описание отсутствует'}</p>
                        <div class="test-card-meta">
                            <span>⏱️ ${test.timeLimitMinutes} мин</span>
                            <span>📋 ${test.questionCount} вопросов</span>
                            <span>🎯 ${test.passingScore}% для прохода</span>
                        </div>
                        <p class="test-card-assigned">
                            <small>Назначил: ${test.assignerName || 'HR'}</small>
                        </p>
                    </div>
                    <div class="test-card-actions">
                        <button class="btn btn-primary" onclick="startTest(${test.id})">
                            ${deadline && daysLeft <= 0 ? 'Просмотреть' : 'Начать тест'}
                        </button>
                        <button class="btn btn-secondary" onclick="viewTestDetails(${test.id})">
                            Подробнее
                        </button>
                    </div>
                </div>
            `;
        }).join('');
        
    } catch (error) {
        console.error('Error loading available tests:', error);
        const grid = document.getElementById('availableTestsGrid');
        if (grid) {
            grid.innerHTML = '<p class="text-center text-danger">Ошибка загрузки тестов</p>';
        }
    }
}

// Загрузка истории тестирований
async function loadMyAttempts() {
    try {
        const attempts = await api.employee.getMyAttempts();
        const tableBody = document.getElementById('testHistoryTable');
        const deadlineList = document.getElementById('deadlineList');
        
        if (tableBody) {
            if (attempts.length === 0) {
                tableBody.innerHTML = `
                    <tr>
                        <td colspan="6" class="text-center">Нет пройденных тестов</td>
                    </tr>
                `;
            } else {
                tableBody.innerHTML = attempts.map(attempt => `
                    <tr>
                        <td>${attempt.testTitle}</td>
                        <td>${formatDate(attempt.completedAt || attempt.startedAt)}</td>
                        <td><strong>${attempt.score || 0}%</strong></td>
                        <td>${attempt.passingScore || 0}%</td>
                        <td>
                            ${attempt.status === 'EVALUATED' ? 
                                `<span class="status-badge ${attempt.passed ? 'status-active' : 'status-failed'}">
                                    ${attempt.passed ? 'Пройден' : 'Не пройден'}
                                </span>` : 
                                `<span class="status-badge status-pending">Ожидает</span>`
                            }
                        </td>
                        <td>
                            ${getStatusText(attempt.status)}
                        </td>
                    </tr>
                `).join('');
            }
        }
        
        // Загрузка дедлайнов (из доступных тестов)
        if (deadlineList) {
            const tests = await api.employee.getAvailableTests();
            const upcomingDeadlines = tests
                .filter(test => test.deadline)
                .sort((a, b) => new Date(a.deadline) - new Date(b.deadline))
                .slice(0, 5);
            
            if (upcomingDeadlines.length === 0) {
                deadlineList.innerHTML = '<p class="text-center">Нет предстоящих дедлайнов</p>';
            } else {
                deadlineList.innerHTML = upcomingDeadlines.map(test => {
                    const deadline = new Date(test.deadline);
                    const now = new Date();
                    const daysLeft = Math.ceil((deadline - now) / (1000 * 60 * 60 * 24));
                    
                    let priority = 'low';
                    if (daysLeft <= 3) priority = 'high';
                    else if (daysLeft <= 7) priority = 'medium';
                    
                    return `
                        <div class="deadline-item ${priority}-priority">
                            <div class="deadline-info">
                                <div class="deadline-test">${test.title}</div>
                                <div class="deadline-date">До ${formatDate(deadline)}</div>
                            </div>
                            <div class="deadline-days">
                                <span class="days-count">${daysLeft}</span>
                                <span class="days-text">дней</span>
                            </div>
                        </div>
                    `;
                }).join('');
            }
        }
        
    } catch (error) {
        console.error('Error loading my attempts:', error);
    }
}

// Обновление статистики
async function updateStatistics() {
    try {
        const attempts = await api.employee.getMyAttempts();
        const availableTests = await api.employee.getAvailableTests();
        
        const completedAttempts = attempts.filter(a => a.status === 'EVALUATED');
        const pendingAttempts = attempts.filter(a => a.status === 'EVALUATING');
        
        const stats = {
            availableTests: availableTests.length,
            completedTests: completedAttempts.length,
            averageScore: completedAttempts.length > 0 ? 
                Math.round(completedAttempts.reduce((sum, a) => sum + (a.score || 0), 0) / completedAttempts.length) : 0,
            pendingTests: pendingAttempts.length
        };
        
        // Обновляем DOM
        document.getElementById('availableTests').textContent = stats.availableTests;
        document.getElementById('completedTests').textContent = stats.completedTests;
        document.getElementById('averageScore').textContent = `${stats.averageScore}%`;
        document.getElementById('pendingTests').textContent = stats.pendingTests;
        
    } catch (error) {
        console.error('Error updating statistics:', error);
    }
}

// Вспомогательные функции
function formatDate(date) {
    if (!date) return '';
    const d = new Date(date);
    return d.toLocaleDateString('ru-RU', {
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
        case 'COMPLETED': return 'Завершен';
        case 'EVALUATING': return 'На проверке';
        case 'EVALUATED': return 'Проверен';
        default: return status;
    }
}

// Действия с тестами
async function startTest(testId) {
    try {
        const response = await api.employee.startTest(testId);
        
        if (response.success) {
            window.location.href = `take-test.html?attempt=${response.data.attemptId}`;
        } else {
            throw new Error(response.message || 'Ошибка начала теста');
        }
    } catch (error) {
        console.error('Error starting test:', error);
        api.utils.showNotification(`Ошибка: ${error.message}`, 'error');
    }
}

function viewTestDetails(testId) {
    // Можно открыть модальное окно с деталями
    console.log('View test details:', testId);
}

// Фильтрация тестов
function filterTests(category) {
    const buttons = document.querySelectorAll('.filter-buttons .btn');
    buttons.forEach(btn => btn.classList.remove('active'));
    event.target.classList.add('active');
    
    const cards = document.querySelectorAll('.test-card');
    cards.forEach(card => {
        if (category === 'all' || card.dataset.category === category) {
            card.style.display = 'block';
        } else {
            card.style.display = 'none';
        }
    });
}