<template>
  <div class="login-page">
    <div class="bg-illu"></div>
    <el-card class="login-card card-glass" shadow="hover">
      <h2 class="title">AegisAI · 合规控制台</h2>
      <p class="subtitle">2026 极简科技风 · 支持国产AI安全接入</p>
      <el-form :model="form" @submit.prevent>
        <el-form-item label="用户名" :rules="[{ required: true, message: '请输入用户名' }]"><el-input v-model="form.username" autocomplete="off" /></el-form-item>
        <el-form-item label="密码" :rules="[{ required: true, message: '请输入密码' }]"><el-input type="password" v-model="form.password" autocomplete="off" /></el-form-item>
        <el-form-item label="验证码">
          <div style="display:flex; gap:8px; align-items:center;">
            <el-input v-model="form.captcha" style="flex:1" />
            <div class="captcha" @click="refreshCaptcha">{{ captcha }}</div>
          </div>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="remember">记住密码</el-checkbox>
          <el-link type="primary" style="float:right;" @click="reset">忘记密码?</el-link>
        </el-form-item>
        <el-button type="primary" class="login-btn" @click="onSubmit" :loading="loading">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue';
import request from '../api/request';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
const router = useRouter();
const form = reactive({ username: 'admin', password: 'admin123', captcha: '' });
const loading = ref(false);
const captcha = ref('');
const remember = ref(true);

const refreshCaptcha = () => {
  const chars = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';
  captcha.value = Array.from({ length: 4 }, () => chars[Math.floor(Math.random() * chars.length)]).join('');
};

onMounted(() => {
  refreshCaptcha();
  const saved = localStorage.getItem('login_cache');
  if (saved) Object.assign(form, JSON.parse(saved));
});

async function onSubmit() {
  if (form.captcha.toUpperCase() !== captcha.value) {
    return ElMessage.error('验证码错误');
  }
  loading.value = true;
  try {
    const res = await request.post('/auth/login', form);
    localStorage.setItem('token', res.token);
    if (remember.value) localStorage.setItem('login_cache', JSON.stringify({ username: form.username, password: form.password }));
    router.push('/');
  } catch (e) {
    ElMessage.error(e?.message || '登录失败，检查后端服务是否已启动');
  } finally {
    loading.value = false;
  }
}

function reset() {
  form.password = '';
  ElMessage.info('请联系管理员重置密码');
}
</script>

<style scoped>
.login-page { display:flex; justify-content:center; align-items:center; min-height:100vh; position:relative; overflow:hidden; }
.bg-illu { position:absolute; inset:0; background:radial-gradient(circle at 20% 20%, rgba(22,93,255,0.25), rgba(0,0,0,0)), radial-gradient(circle at 80% 0%, rgba(0,180,42,0.2), rgba(0,0,0,0)); filter: blur(20px); }
.login-card { width:420px; position:relative; z-index:2; padding: 10px 16px 20px; color: #e8edf7; }
.title { text-align:center; margin:10px 0 4px; color:#fff; }
.subtitle { text-align:center; color:#9fb2ce; margin-bottom:16px; font-size:13px; }
.captcha { padding: 10px 14px; background: rgba(255,255,255,0.08); border-radius: var(--radius-md); cursor:pointer; letter-spacing:4px; font-weight:700; color:#fff; }
.login-btn { width:100%; margin-top:6px; }
</style>
