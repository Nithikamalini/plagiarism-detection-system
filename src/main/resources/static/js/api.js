/**
 * API Client & Session Helper
 */
const API = {
  TOKEN_KEY: 'plagiarism_auth_token',
  USER_KEY: 'plagiarism_user_data',

  getToken() {
    return localStorage.getItem(this.TOKEN_KEY);
  },

  setToken(token) {
    if (token) localStorage.setItem(this.TOKEN_KEY, token);
    else localStorage.removeItem(this.TOKEN_KEY);
  },

  getUser() {
    const data = localStorage.getItem(this.USER_KEY);
    return data ? JSON.parse(data) : null;
  },

  setUser(user) {
    if (user) localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    else localStorage.removeItem(this.USER_KEY);
  },

  isLoggedIn() {
    return !!this.getToken();
  },

  logout() {
    this.setToken(null);
    this.setUser(null);
    window.location.href = 'login.html';
  },

  async request(endpoint, options = {}) {
    const headers = options.headers || {};
    const token = this.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    if (!(options.body instanceof FormData) && !headers['Content-Type']) {
      headers['Content-Type'] = 'application/json';
    }

    try {
      const response = await fetch(endpoint, {
        ...options,
        headers
      });

      if (response.status === 401 && !endpoint.includes('/api/auth/login')) {
        // Token expired or invalid
        this.setToken(null);
        this.setUser(null);
        if (!window.location.pathname.endsWith('login.html') && !window.location.pathname.endsWith('index.html')) {
          window.location.href = 'login.html';
        }
      }

      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(data.message || `HTTP error ${response.status}`);
      }
      return data;
    } catch (err) {
      console.error(`API Error on ${endpoint}:`, err);
      throw err;
    }
  },

  get(endpoint) {
    return this.request(endpoint, { method: 'GET' });
  },

  post(endpoint, body) {
    return this.request(endpoint, {
      method: 'POST',
      body: typeof body === 'string' ? body : JSON.stringify(body)
    });
  },

  delete(endpoint) {
    return this.request(endpoint, { method: 'DELETE' });
  },

  showToast(message, type = 'info') {
    let toast = document.getElementById('app-toast');
    if (!toast) {
      toast = document.createElement('div');
      toast.id = 'app-toast';
      toast.style.position = 'fixed';
      toast.style.bottom = '24px';
      toast.style.right = '24px';
      toast.style.padding = '12px 24px';
      toast.style.borderRadius = '8px';
      toast.style.zIndex = '9999';
      toast.style.fontWeight = '600';
      toast.style.boxShadow = '0 10px 15px -3px rgba(0,0,0,0.5)';
      toast.style.transition = 'all 0.3s ease';
      document.body.appendChild(toast);
    }

    if (type === 'error') {
      toast.style.background = '#ef4444';
      toast.style.color = '#fff';
    } else if (type === 'success') {
      toast.style.background = '#10b981';
      toast.style.color = '#fff';
    } else {
      toast.style.background = '#3b82f6';
      toast.style.color = '#fff';
    }

    toast.textContent = message;
    toast.style.display = 'block';
    toast.style.opacity = '1';

    setTimeout(() => {
      toast.style.opacity = '0';
      setTimeout(() => { toast.style.display = 'none'; }, 300);
    }, 3500);
  },

  updateNavAuth() {
    const userContainer = document.getElementById('nav-user-container');
    if (!userContainer) return;

    const user = this.getUser();
    if (user) {
      userContainer.innerHTML = `
        <div class="user-profile-badge">
          <span>👤</span>
          <span>${user.fullName || user.username}</span>
        </div>
        <button class="btn btn-outline btn-sm" onclick="API.logout()">Sign Out</button>
      `;
    } else {
      userContainer.innerHTML = `
        <a href="login.html" class="btn btn-outline btn-sm">Sign In</a>
        <a href="register.html" class="btn btn-primary btn-sm">Register</a>
      `;
    }
  }
};

document.addEventListener('DOMContentLoaded', () => {
  API.updateNavAuth();
});
