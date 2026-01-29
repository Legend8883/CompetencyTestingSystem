class HRService {
    constructor() {
        this.api = apiService;
        this.currentEmployeesPage = 1;
        this.employeesPerPage = 10;
        this.allEmployees = [];
    }

    async loadDashboard() {
        try {
            const [testsRes, employeesRes] = await Promise.all([
                this.api.getHRTests(),
                this.api.getEmployees()
            ]);

            const tests = testsRes.data || [];
            const employees = employeesRes.data || [];
            this.allEmployees = employees; // Сохраняем всех сотрудников

            // Обновляем статистику
            document.getElementById('totalTests').textContent = tests.length;
            document.getElementById('totalEmployees').textContent = employees.length;
            document.getElementById('activeTests').textContent = tests.filter(t => t.isActive).length;
            
            // Добавляем пагинацию для сотрудников в модальном окне
            this.updateEmployeesTable(employees);
            
            // Обновляем таблицу тестов
            this.updateTestsTable(tests);
            
        } catch (error) {
            console.error('Ошибка загрузки:', error);
            document.getElementById('testsTable').innerHTML = `
                <tr><td colspan="5" style="color: var(--error);">Ошибка загрузки данных</td></tr>
            `;
        }
    }

    updateTestsTable(tests) {
        const tbody = document.getElementById('testsTable');
        if (tests.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5">Нет тестов</td></tr>';
            return;
        }
        
        tbody.innerHTML = tests.map(test => `
            <tr>
                <td>${test.title}</td>
                <td>${test.questionCount}</td>
                <td>
                    <span class="status ${test.isActive ? 'status-active' : 'status-inactive'}">
                        ${test.isActive ? 'Активен' : 'Не активен'}
                    </span>
                </td>
                <td>${Utils.formatDate(test.createdAt)}</td>
                <td>
                    <button class="btn btn-outline btn-small" onclick="hrService.viewTest(${test.id})">
                        Просмотр
                    </button>
                </td>
            </tr>
        `).join('');
    }

    showAssignModal() {
        if (this.allEmployees.length === 0) {
            alert('Нет сотрудников для назначения');
            return;
        }

        // Создаем HTML для пагинации сотрудников
        const start = (this.currentEmployeesPage - 1) * this.employeesPerPage;
        const end = start + this.employeesPerPage;
        const pageEmployees = this.allEmployees.slice(start, end);
        const totalPages = Math.ceil(this.allEmployees.length / this.employeesPerPage);

        let employeesHtml = '';
        if (pageEmployees.length === 0) {
            employeesHtml = '<p>Нет сотрудников</p>';
        } else {
            employeesHtml = pageEmployees.map(emp => `
                <label style="display: block; margin-bottom: 0.5rem; padding: 0.5rem; border: 1px solid var(--gray-border); border-radius: var(--radius);">
                    <input type="checkbox" class="employee-checkbox" value="${emp.id}">
                    ${emp.firstName} ${emp.lastName} (${emp.email})
                </label>
            `).join('');
        }

        // Пагинация
        let paginationHtml = '';
        if (totalPages > 1) {
            paginationHtml = `
                <div style="display: flex; justify-content: center; gap: 0.5rem; margin: 1rem 0;">
                    ${this.currentEmployeesPage > 1 ? 
                        `<button class="btn btn-outline btn-small" onclick="hrService.prevEmployeesPage()">←</button>` : ''}
                    <span>Страница ${this.currentEmployeesPage} из ${totalPages}</span>
                    ${this.currentEmployeesPage < totalPages ? 
                        `<button class="btn btn-outline btn-small" onclick="hrService.nextEmployeesPage()">→</button>` : ''}
                </div>
            `;
        }

        Utils.createModal('Назначение теста', `
            <p>Выберите тест и сотрудников для назначения (${this.allEmployees.length} всего)</p>
            
            <div class="form-group">
                <label>Выберите тест:</label>
                <select id="assignTestSelect" class="form-control">
                    <option value="">-- Выберите тест --</option>
                </select>
            </div>
            
            <div class="form-group">
                <label>Сотрудники (${start + 1}-${Math.min(end, this.allEmployees.length)} из ${this.allEmployees.length}):</label>
                <div style="max-height: 300px; overflow-y: auto; padding: 1rem; border: 1px solid var(--gray-border); border-radius: var(--radius); background: var(--white);">
                    ${employeesHtml}
                </div>
                ${paginationHtml}
                <div style="margin-top: 0.5rem;">
                    <button class="btn btn-outline btn-small" onclick="hrService.selectAllEmployees()">Выбрать всех</button>
                    <button class="btn btn-outline btn-small" onclick="hrService.deselectAllEmployees()">Снять всех</button>
                </div>
            </div>
            
            <div class="form-group">
                <label>Дедлайн:</label>
                <input type="datetime-local" id="assignDeadline" class="form-control" required>
            </div>
            
            <button class="btn btn-primary btn-block" onclick="hrService.assignTest()">
                Назначить тест
            </button>
        `, () => {
            // При закрытии модалки сбрасываем страницу
            this.currentEmployeesPage = 1;
        });

        // Загружаем тесты в select
        this.loadTestsForAssignment();
    }

    async loadTestsForAssignment() {
        try {
            const response = await this.api.getHRTests();
            const tests = response.data || [];
            
            const select = document.getElementById('assignTestSelect');
            select.innerHTML = '<option value="">-- Выберите тест --</option>' + 
                tests.filter(t => t.isActive).map(t => 
                    `<option value="${t.id}">${t.title} (${t.questionCount} вопросов)</option>`
                ).join('');
        } catch (error) {
            console.error('Ошибка загрузки тестов:', error);
        }
    }

    selectAllEmployees() {
        document.querySelectorAll('.employee-checkbox').forEach(cb => {
            cb.checked = true;
        });
    }

    deselectAllEmployees() {
        document.querySelectorAll('.employee-checkbox').forEach(cb => {
            cb.checked = false;
        });
    }

    prevEmployeesPage() {
        if (this.currentEmployeesPage > 1) {
            this.currentEmployeesPage--;
            this.showAssignModal();
        }
    }

    nextEmployeesPage() {
        const totalPages = Math.ceil(this.allEmployees.length / this.employeesPerPage);
        if (this.currentEmployeesPage < totalPages) {
            this.currentEmployeesPage++;
            this.showAssignModal();
        }
    }

    async assignTest() {
        const testId = document.getElementById('assignTestSelect').value;
        const deadline = document.getElementById('assignDeadline').value;
        
        if (!testId) {
            alert('Выберите тест');
            return;
        }
        
        const selectedEmployees = Array.from(document.querySelectorAll('.employee-checkbox:checked'))
            .map(cb => parseInt(cb.value));
            
        if (selectedEmployees.length === 0) {
            alert('Выберите хотя бы одного сотрудника');
            return;
        }
        
        if (!deadline) {
            alert('Укажите дедлайн');
            return;
        }
        
        try {
            await this.api.assignTest(testId, selectedEmployees, deadline);
            alert('Тест успешно назначен!');
            // Закрываем модалку
            document.querySelector('.modal-close').click();
        } catch (error) {
            alert('Ошибка назначения: ' + error.message);
        }
    }

    showEvaluationModal() {
        Utils.createModal('Проверка ответов', `
            <p>Ответы на открытые вопросы</p>
            <div style="max-height: 300px; overflow-y: auto;">
                <div style="margin-bottom: 1rem; padding: 1rem; border: 1px solid var(--gray-border); border-radius: var(--radius);">
                    <strong>Вопрос: Что такое ООП?</strong>
                    <p style="margin: 0.5rem 0;">Ответ: Объектно-ориентированное программирование...</p>
                    <input type="number" min="0" max="10" value="8" class="form-control" style="width: 100px;">
                </div>
            </div>
            <button class="btn btn-primary btn-block" onclick="alert('Оценка сохранена!')">
                Сохранить оценки
            </button>
        `);
    }

    async viewTest(testId) {
        try {
            const response = await this.api.getHRTest(testId);
            const test = response.data;
            
            Utils.createModal(test.title, `
                <p><strong>Описание:</strong> ${test.description || 'Нет описания'}</p>
                <p><strong>Время:</strong> ${test.timeLimitMinutes} минут</p>
                <p><strong>Проходной балл:</strong> ${test.passingScore}</p>
                <p><strong>Статус:</strong> ${test.isActive ? 'Активен' : 'Не активен'}</p>
                <p><strong>Создан:</strong> ${Utils.formatDate(test.createdAt)}</p>
                <button class="btn ${test.isActive ? 'btn-outline' : 'btn-primary'}" 
                        onclick="hrService.toggleTest(${test.id}, ${test.isActive})">
                    ${test.isActive ? 'Деактивировать' : 'Активировать'}
                </button>
            `);
        } catch (error) {
            alert('Ошибка: ' + error.message);
        }
    }

    async toggleTest(testId, isActive) {
        try {
            if (isActive) {
                await this.api.deactivateTest(testId);
                alert('Тест деактивирован');
            } else {
                await this.api.activateTest(testId);
                alert('Тест активирован');
            }
            this.loadDashboard();
        } catch (error) {
            alert('Ошибка: ' + error.message);
        }
    }

    showReports() {
        Utils.createModal('Отчеты', `
            <p>Статистика по тестированиям</p>
            <div style="margin: 1rem 0;">
                <strong>Всего тестов:</strong> 12
            </div>
            <div style="margin: 1rem 0;">
                <strong>Средний балл:</strong> 78%
            </div>
            <button class="btn btn-primary" onclick="alert('Отчет сформирован')">
                Экспортировать отчет
            </button>
        `);
    }
}

const hrService = new HRService();
window.hrService = hrService;