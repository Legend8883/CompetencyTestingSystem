class AuthService {
    constructor() {
        this.api = apiService;
    }

    async login(email, password) {
        const response = await this.api.login(email, password);
        return response;
    }

    async register(userData) {
        return await this.api.register(userData);
    }

    async registerHR(userData) {
        return await this.api.registerHR(userData);
    }

    logout() {
        // Только при явном выходе
        this.api.clearAuth();
        window.location.href = '../index.html';
    }

    isAuthenticated() {
        return !!this.api.token;
    }

    getUser() {
        return this.api.user;
    }

    getRole() {
        return this.api.user?.role;
    }

    isHR() {
        return this.getRole() === 'HR';
    }

    isEmployee() {
        return this.getRole() === 'EMPLOYEE';
    }

    checkAuth() {
        if (!this.isAuthenticated()) {
            window.location.href = '../login.html';
            return false;
        }
        return true;
    }

    checkRole(requiredRole) {
        if (!this.checkAuth()) {
            return false;
        }

        if (this.getRole() !== requiredRole) {
            // Просто перенаправляем на правильный дашборд
            window.location.href = this.isHR() ? '../pages/hr-dashboard.html' : '../pages/employee-dashboard.html';
            return false;
        }

        return true;
    }
}

const authService = new AuthService();
window.authService = authService;