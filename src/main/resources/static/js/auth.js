/**
 * Auth Logic (Login, Register, Demo Accounts)
 */

async function handleLogin(e) {
  e.preventDefault();
  const username = document.getElementById('username').value.trim();
  const password = document.getElementById('password').value.trim();
  const errorBox = document.getElementById('auth-error');

  if (!username || !password) {
    showError('Please enter both username and password.');
    return;
  }

  try {
    const data = await API.post('/api/auth/login', { username, password });
    if (data.success) {
      API.setToken(data.token);
      API.setUser(data.user);
      window.location.href = 'dashboard.html';
    } else {
      showError(data.message || 'Login failed.');
    }
  } catch (err) {
    showError(err.message || 'Server error. Is the backend running?');
  }
}

async function handleRegister(e) {
  e.preventDefault();
  const fullName = document.getElementById('fullName').value.trim();
  const email = document.getElementById('email').value.trim();
  const username = document.getElementById('username').value.trim();
  const password = document.getElementById('password').value.trim();
  const confirmPassword = document.getElementById('confirmPassword').value.trim();

  if (password !== confirmPassword) {
    showError('Passwords do not match.');
    return;
  }

  try {
    const data = await API.post('/api/auth/register', { fullName, email, username, password });
    if (data.success) {
      API.setToken(data.token);
      API.setUser(data.user);
      window.location.href = 'dashboard.html';
    } else {
      showError(data.message || 'Registration failed.');
    }
  } catch (err) {
    showError(err.message || 'Server error during registration.');
  }
}

function fillDemo(username, password) {
  document.getElementById('username').value = username;
  document.getElementById('password').value = password;
  API.showToast(`Selected demo account: ${username}`, 'info');
}

function showError(msg) {
  const errorBox = document.getElementById('auth-error');
  if (errorBox) {
    errorBox.textContent = msg;
    errorBox.style.display = 'block';
  } else {
    API.showToast(msg, 'error');
  }
}
