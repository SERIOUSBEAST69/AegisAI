import axios from 'axios';

const service = axios.create({
  baseURL: '/api',
  timeout: 10000
});

service.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) config.headers['Authorization'] = 'Bearer ' + token;
  return config;
});

service.interceptors.response.use(
  res => {
    const body = res.data;
    // 后端统一返回 R { code, msg, data, timestamp }
    if (body && body.code === 20000) return body.data;
    if (body && body.code === 40100) {
      localStorage.removeItem('token');
      window.location.href = '/login';
      return Promise.reject(body.msg || '未授权');
    }
    return Promise.reject((body && body.msg) || '请求失败');
  },
  err => {
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export default service;
