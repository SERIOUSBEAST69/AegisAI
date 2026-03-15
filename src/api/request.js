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
  // The backend explicitly rejected the request — always clear the local session
  // (including stale mock sessions) and redirect to the login page so the user
  // can re-authenticate with real credentials.
  clearSession('expired');
  const error = createClientError(message || '未登录或会话已失效', {
    code: 40100,
    sessionExpired: true,
    detail: data || null,
  });
  if (window.location.pathname !== '/login') {
    redirectToLogin();
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
    if (body && body.code === 40310) {
      return rejectWith(body.msg || '跨站请求已被拦截', {
        code: 40310,
        crossSiteBlocked: true,
        detail: body.data || null,
      });
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
      if (err.response.data?.code === 40310) {
        return rejectWith(err.response.data?.msg || '跨站请求已被拦截', {
          code: 40310,
          status: 403,
          crossSiteBlocked: true,
          detail: err.response.data?.data || null,
        });
      }
      return rejectWith(err.response.data?.msg || '无权限访问当前资源', { code: 40300, status: 403 });
    }
    if (!err.response) {
      return rejectWith('网络连接失败，请检查后端服务、代理配置或跨域设置', { network: true });
    }
    return rejectWith(err.response.data?.msg || err.message || '请求失败', { status: err.response.status });
  }
);

export default service;
