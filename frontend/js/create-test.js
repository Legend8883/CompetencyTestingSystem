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
    
    // Инициализируем редактор
    initQuestionEditor();
    
    // Проверяем, редактируем ли существующий тест
    const urlParams = new URLSearchParams(window.location.search);
    const testId = urlParams.get('edit');
    if (testId) {
        await loadTestForEditing(testId);
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

// Инициализация редактора вопросов
let questions = [];
let nextQuestionId = 1;
let currentQuestionType = 'SINGLE_CHOICE';
let editingTestId = null;

function initQuestionEditor() {
    showQuestionForm(currentQuestionType);
    addOption(); // Добавляем начальные варианты
    updatePreview();
}

// Остальной код create-test.js остается похожим, но с изменениями в формате данных

// Функция создания теста через API
async function createTest() {
    // Валидация
    if (!validateTest()) return;
    
    const testData = {
        title: document.getElementById('testName').value.trim(),
        description: document.getElementById('testDescription').value.trim(),
        timeLimitMinutes: parseInt(document.getElementById('testTime').value),
        passingScore: parseInt(document.getElementById('passingScore').value),
        questions: questions.map((q, index) => {
            const question = {
                text: q.text,
                type: q.type,
                maxScore: q.points,
                orderIndex: index,
                options: q.type !== 'OPEN_ANSWER' ? q.options.map((opt, optIndex) => ({
                    text: opt.text,
                    isCorrect: opt.correct,
                    orderIndex: optIndex
                })) : [],
                correctOpenAnswer: q.type === 'OPEN_ANSWER' ? q.criteria : null
            };
            return question;
        })
    };
    
    try {
        const submitBtn = document.getElementById('publishBtn');
        const originalText = submitBtn.textContent;
        submitBtn.innerHTML = '<span class="loading"></span> Публикация...';
        submitBtn.disabled = true;
        
        let response;
        if (editingTestId) {
            response = await api.hr.updateTest(editingTestId, testData);
        } else {
            response = await api.hr.createTest(testData);
        }
        
        if (response.success) {
            api.utils.showNotification(
                editingTestId ? 'Тест обновлен!' : 'Тест успешно создан!', 
                'success'
            );
            
            setTimeout(() => {
                window.location.href = 'dashboard-hr.html';
            }, 1500);
        } else {
            throw new Error(response.message || 'Ошибка при сохранении теста');
        }
        
    } catch (error) {
        console.error('Error saving test:', error);
        api.utils.showNotification(`Ошибка: ${error.message}`, 'error');
    } finally {
        const submitBtn = document.getElementById('publishBtn');
        if (submitBtn) {
            submitBtn.textContent = editingTestId ? 'Обновить тест' : 'Опубликовать тест';
            submitBtn.disabled = false;
        }
    }
}

// Загрузка теста для редактирования
async function loadTestForEditing(testId) {
    try {
        const test = await api.hr.getTest(testId);
        
        if (!test) {
            api.utils.showNotification('Тест не найден', 'error');
            window.location.href = 'dashboard-hr.html';
            return;
        }
        
        editingTestId = testId;
        
        // Заполняем поля
        document.getElementById('testName').value = test.title;
        document.getElementById('testDescription').value = test.description || '';
        document.getElementById('testTime').value = test.timeLimitMinutes;
        document.getElementById('passingScore').value = test.passingScore;
        
        // Преобразуем вопросы
        questions = test.questions.map((q, index) => {
            const question = {
                id: nextQuestionId++,
                text: q.text,
                type: q.type,
                points: q.maxScore,
                order: index
            };
            
            if (q.type !== 'OPEN_ANSWER') {
                question.options = q.options.map(opt => ({
                    text: opt.text,
                    correct: opt.isCorrect
                }));
            } else {
                question.criteria = q.correctOpenAnswer || '';
            }
            
            return question;
        });
        
        // Обновляем интерфейс
        updateQuestionsList();
        updatePreview();
        
        // Меняем текст кнопки
        document.getElementById('publishBtn').textContent = 'Обновить тест';
        document.getElementById('pageTitle').textContent = 'Редактирование теста';
        
    } catch (error) {
        console.error('Error loading test for editing:', error);
        api.utils.showNotification('Ошибка загрузки теста', 'error');
    }
}