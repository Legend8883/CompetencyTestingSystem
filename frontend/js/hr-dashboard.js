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
    
    // Загружаем данные дашборда
    await loadDashboardData();
    
    // Назначаем обработчики
    document.getElementById('assignTestBtn')?.addEventListener('click', openAssignModal);
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

// Загрузка данных дашборда
async function loadDashboardData() {
    try {
        await Promise.all([
            loadStatistics(),
            loadRecentTests(),
            loadPendingReviews()
        ]);
    } catch (error) {
        console.error('Error loading dashboard data:', error);
        api.utils.showNotification('Ошибка загрузки данных', 'error');
    }
}

// Загрузка статистики
async function loadStatistics() {
    try {
        const stats = await api.hr.getStatistics();
        
        // Обновляем значения
        document.getElementById('totalTests').textContent = stats.totalTests || 0;
        document.getElementById('totalEmployees').textContent = stats.totalEmployees || 0;
        document.getElementById('completedTests').textContent = stats.completedAttempts || 0;
        document.getElementById('pendingReviews').textContent = stats.pendingReviews || 0;
        
    } catch (error) {
        console.error('Error loading statistics:', error);
    }
}

// Загрузка последних тестов
async function loadRecentTests() {
    try {
        const tests = await api.hr.getTests();
        const tableBody = document.getElementById('recentTestsTable');
        
        if (!tableBody) return;
        
        if (tests.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center">Нет созданных тестов</td>
                </tr>
            `;
            return;
        }
        
        // Берем последние 5 тестов
        const recentTests = tests.slice(0, 5);
        
        tableBody.innerHTML = recentTests.map(test => `
            <tr>
                <td>${test.title}</td>
                <td>${formatDate(test.createdAt)}</td>
                <td>${test.questionCount || 0}</td>
                <td>${test.passingScore}%</td>
                <td>
                    <span class="status-badge ${test.isActive ? 'status-active' : 'status-draft'}">
                        ${test.isActive ? 'Активный' : 'Черновик'}
                    </span>
                </td>
                <td>
                    <button class="btn btn-sm btn-secondary" onclick="editTest(${test.id})">Изменить</button>
                    <button class="btn btn-sm btn-primary" onclick="viewTestResults(${test.id})">Результаты</button>
                </td>
            </tr>
        `).join('');
        
    } catch (error) {
        console.error('Error loading recent tests:', error);
        const tableBody = document.getElementById('recentTestsTable');
        if (tableBody) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center text-danger">Ошибка загрузки</td>
                </tr>
            `;
        }
    }
}

// Загрузка ожидающих проверки
async function loadPendingReviews() {
    try {
        const attempts = await api.hr.getAttemptsForEvaluation();
        const grid = document.getElementById('pendingReviewsGrid');
        
        if (!grid) return;
        
        if (attempts.length === 0) {
            grid.innerHTML = '<p class="text-center">Нет попыток, ожидающих проверки</p>';
            return;
        }
        
        grid.innerHTML = attempts.map(attempt => `
            <div class="review-card">
                <div class="review-card-header">
                    <div class="review-card-title">${attempt.testTitle}</div>
                    <div class="review-card-badge">Ожидает проверки</div>
                </div>
                <div class="review-card-content">
                    <p><strong>Сотрудник:</strong> ${attempt.employeeName}</p>
                    <div class="review-card-meta">
                        <span>Завершено: ${formatDate(attempt.completedAt)}</span>
                        <span>Автооценка: ${attempt.autoScore || 0}%</span>
                    </div>
                </div>
                <div class="review-card-actions">
                    <button class="btn btn-sm btn-primary" onclick="startReview(${attempt.id})">Начать проверку</button>
                    <button class="btn btn-sm btn-secondary" onclick="viewAttemptDetails(${attempt.id})">Подробнее</button>
                </div>
            </div>
        `).join('');
        
    } catch (error) {
        console.error('Error loading pending reviews:', error);
        const grid = document.getElementById('pendingReviewsGrid');
        if (grid) {
            grid.innerHTML = '<p class="text-center text-danger">Ошибка загрузки</p>';
        }
    }
}

// Модальное окно назначения теста
let selectedTestId = null;

async function openAssignModal(testId) {
    selectedTestId = testId || selectedTestId;
    
    if (!selectedTestId) {
        api.utils.showNotification('Выберите тест для назначения', 'warning');
        return;
    }
    
    try {
        // Загружаем тест и сотрудников
        const [test, employees] = await Promise.all([
            api.hr.getTest(selectedTestId),
            api.hr.getEmployees()
        ]);
        
        if (!test) {
            api.utils.showNotification('Тест не найден', 'error');
            return;
        }
        
        // Обновляем модальное окно
        document.getElementById('modalTestName').textContent = test.title;
        document.getElementById('testSelect').value = test.id;
        
        const employeesList = document.getElementById('employeesList');
        employeesList.innerHTML = employees.map(employee => `
            <label class="checkbox-label">
                <input type="checkbox" name="employee" value="${employee.id}">
                <span>${employee.firstName} ${employee.lastName} (${employee.email})</span>
            </label>
        `).join('');
        
        // Показываем модальное окно
        document.getElementById('assignTestModal').classList.remove('hidden');
        
    } catch (error) {
        console.error('Error opening assign modal:', error);
        api.utils.showNotification('Ошибка загрузки данных', 'error');
    }
}

async function assignTest() {
    const testId = document.getElementById('testSelect').value;
    const deadline = document.getElementById('deadline').value;
    const selectedEmployees = Array.from(document.querySelectorAll('input[name="employee"]:checked'))
        .map(cb => parseInt(cb.value));
    
    if (!testId) {
        api.utils.showNotification('Выберите тест', 'warning');
        return;
    }
    
    if (selectedEmployees.length === 0) {
        api.utils.showNotification('Выберите хотя бы одного сотрудника', 'warning');
        return;
    }
    
    if (!deadline) {
        api.utils.showNotification('Установите дедлайн', 'warning');
        return;
    }
    
    try {
        await api.hr.assignTest(testId, selectedEmployees, deadline);
        api.utils.showNotification('Тест успешно назначен!', 'success');
        closeModal();
        
        // Обновляем данные
        await loadRecentTests();
        
    } catch (error) {
        console.error('Error assigning test:', error);
        api.utils.showNotification(`Ошибка назначения теста: ${error.message}`, 'error');
    }
}

function closeModal() {
    document.getElementById('assignTestModal').classList.add('hidden');
    document.getElementById('deadline').value = '';
    document.querySelectorAll('input[name="employee"]').forEach(cb => cb.checked = false);
}

// Вспомогательные функции
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('ru-RU', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Действия с тестами
function editTest(testId) {
    window.location.href = `create-test.html?edit=${testId}`;
}

function viewTestResults(testId) {
    window.location.href = `results.html?test=${testId}`;
}

function startReview(attemptId) {
    window.location.href = `check-results.html?attempt=${attemptId}`;
}

function viewAttemptDetails(attemptId) {
    // Можно открыть модальное окно с деталями
    console.log('View attempt details:', attemptId);
}

// Закрытие модального окна по клику вне его
document.addEventListener('click', function(event) {
    const modal = document.getElementById('assignTestModal');
    if (event.target === modal) {
        closeModal();
    }
});