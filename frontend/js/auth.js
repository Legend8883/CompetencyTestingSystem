// Инициализация при загрузке
document.addEventListener('DOMContentLoaded', function() {
    // Проверяем авторизацию при загрузке страниц, кроме index.html и register.html
    const currentPage = window.location.pathname.split('/').pop();
    const publicPages = ['index.html', 'register.html', ''];
    
    if (!publicPages.includes(currentPage)) {
        const user = api.utils.checkAuth();
        if (!user) {
            window.location.href = 'index.html';
            return;
        }
    }
    
    // Инициализация форм на текущей странице
    initLoginForm();
    initRegisterForm();
});

// Инициализация формы входа
function initLoginForm() {
    const loginForm = document.getElementById('loginForm');
    
    if (loginForm) {
        loginForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const email = document.getElementById('email').value.trim();
            const password = document.getElementById('password').value;
            
            // Валидация
            let isValid = true;
            
            if (!isValidEmail(email)) {
                showError('emailError', 'Введите корректный email');
                isValid = false;
            } else {
                hideError('emailError');
            }
            
            if (!isValidPassword(password)) {
                showError('passwordError', 'Пароль должен содержать минимум 6 символов');
                isValid = false;
            } else {
                hideError('passwordError');
            }
            
            if (!isValid) return;
            
            // Показываем состояние загрузки
            const submitBtn = document.getElementById('submitBtn');
            const originalText = submitBtn.textContent;
            submitBtn.innerHTML = '<span class="loading"></span> Вход...';
            submitBtn.disabled = true;
            
            try {
                const response = await api.auth.login(email, password);
                
                if (response.success) {
                    api.utils.showNotification('Вход выполнен успешно!', 'success');
                    
                    // Перенаправление в зависимости от роли
                    setTimeout(() => {
                        const user = api.utils.getCurrentUser();
                        if (user.role === 'HR') {
                            window.location.href = 'dashboard-hr.html';
                        } else {
                            window.location.href = 'dashboard-employee.html';
                        }
                    }, 1000);
                } else {
                    throw new Error(response.message || 'Ошибка при входе');
                }
                
            } catch (error) {
                api.utils.showNotification(error.message, 'error');
                console.error('Login error:', error);
            } finally {
                submitBtn.textContent = originalText;
                submitBtn.disabled = false;
            }
        });
    }
}

// Инициализация формы регистрации
function initRegisterForm() {
    const registerForm = document.getElementById('registerForm');
    
    if (registerForm) {
        registerForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const email = document.getElementById('email').value.trim();
            const firstName = document.getElementById('firstName').value.trim();
            const lastName = document.getElementById('lastName').value.trim();
            const role = document.getElementById('role').value;
            const password = document.getElementById('password').value;
            const confirmPassword = document.getElementById('confirmPassword').value;
            
            // Валидация
            let isValid = true;
            
            if (!isValidEmail(email)) {
                showError('emailError', 'Введите корректный email');
                isValid = false;
            } else {
                hideError('emailError');
            }
            
            if (!firstName) {
                showError('firstNameError', 'Введите имя');
                isValid = false;
            } else {
                hideError('firstNameError');
            }
            
            if (!lastName) {
                showError('lastNameError', 'Введите фамилию');
                isValid = false;
            } else {
                hideError('lastNameError');
            }
            
            if (!role) {
                showError('roleError', 'Выберите роль');
                isValid = false;
            } else {
                hideError('roleError');
            }
            
            if (!isValidPassword(password)) {
                showError('passwordError', 'Пароль должен содержать минимум 6 символов');
                isValid = false;
            } else {
                hideError('passwordError');
            }
            
            if (password !== confirmPassword) {
                showError('confirmPasswordError', 'Пароли не совпадают');
                isValid = false;
            } else {
                hideError('confirmPasswordError');
            }
            
            if (!isValid) return;
            
            // Показываем состояние загрузки
            const submitBtn = document.getElementById('submitBtn');
            const originalText = submitBtn.textContent;
            submitBtn.innerHTML = '<span class="loading"></span> Регистрация...';
            submitBtn.disabled = true;
            
            try {
                const userData = {
                    email,
                    firstName,
                    lastName,
                    password,
                    confirmPassword
                };
                
                let response;
                if (role === 'hr') {
                    response = await api.auth.registerHR(userData);
                } else {
                    response = await api.auth.registerEmployee(userData);
                }
                
                if (response.success) {
                    api.utils.showNotification('Регистрация успешна! Теперь войдите в систему.', 'success');
                    
                    setTimeout(() => {
                        window.location.href = 'index.html';
                    }, 2000);
                } else {
                    throw new Error(response.message || 'Ошибка при регистрации');
                }
                
            } catch (error) {
                api.utils.showNotification(error.message, 'error');
                console.error('Registration error:', error);
            } finally {
                submitBtn.textContent = originalText;
                submitBtn.disabled = false;
            }
        });
    }
}

// Вспомогательные функции для валидации
function isValidEmail(email) {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
}

function isValidPassword(password) {
    return password.length >= 6;
}

function showError(elementId, message) {
    const element = document.getElementById(elementId);
    if (element) {
        element.textContent = message;
        element.classList.add('show');
        const input = document.getElementById(elementId.replace('Error', ''));
        if (input) input.classList.add('error');
    }
}

function hideError(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
        element.classList.remove('show');
        const input = document.getElementById(elementId.replace('Error', ''));
        if (input) input.classList.remove('error');
    }
}