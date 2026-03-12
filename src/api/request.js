import axios from 'axios';
import { clearSession, getAuthHeaderToken, getSession } from '../utils/auth';

const service = axios.create({
  baseURL: '/api',
  timeout: 12000
});

function createClientError(message, extra = {}) {
  const error = new Error(message || '请求失败');
  Object.assign(error, extra);
  return error;
}

function rejectWith(message, extra = {}) {
  return Promise.reject(createClientError(message, extra));
}

function redirectToLogin() {
  const redirect = encodeURIComponent(window.location.pathname + window.location.search);
  window.location.assign(`/login?reason=session-expired&redirect=${redirect}`);
}

function handleUnauthorized(message, data) {
  const session = getSession();
  const hasRealSession = Boolean(session?.token && session.mode !== 'mock');
  const error = createClientError(message || '未登录或会话已失效', {
    code: 40100,
    sessionExpired: hasRealSession,
    detail: data || null,
  });

  if (hasRealSession) {
    clearSession('expired');
    if (window.location.pathname !== '/login') {
      redirectToLogin();
    }
  }

  return Promise.reject(error);
}

service.interceptors.request.use(config => {
  const token = getAuthHeaderToken();
  if (token) config.headers['Authorization'] = 'Bearer ' + token;
  return config;
});

service.interceptors.response.use(
  res => {
    const body = res.data;
    // 后端统一返回 R { code, msg, data, timestamp }
    if (body && body.code === 20000) return body.data;
    if (body && body.code === 40100) {
      return handleUnauthorized(body.msg, body.data);
    }
    if (body && body.code === 40300) {
      return rejectWith(body.msg || '无权限访问当前资源', { code: 40300 });
    }
    return rejectWith((body && body.msg) || '请求失败', { code: body?.code });
  },
  err => {
    if (err.code === 'ECONNABORTED') {
      return rejectWith('请求超时，请稍后重试', { timeout: true });
    }
    if (err.response && err.response.status === 401) {
      return handleUnauthorized(err.response.data?.msg || '未登录或会话已失效', err.response.data?.data);
    }
    if (err.response && err.response.status === 403) {
      return rejectWith(err.response.data?.msg || '无权限访问当前资源', { code: 40300, status: 403 });
    }
    if (!err.response) {
      return rejectWith('网络连接失败，请检查后端服务、代理配置或跨域设置', { network: true });
    }
    return rejectWith(err.response.data?.msg || err.message || '请求失败', { status: err.response.status });
  }
);

export default service;
