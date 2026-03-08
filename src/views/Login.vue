<template>
  <div class="login-page">
    <div class="bg-illu"></div>
    <div class="bg-particles"></div>
    <el-card class="login-card card-glass" shadow="hover">
      <div class="login-header">
        <div class="logo-icon">🛡️</div>
        <h2 class="title">AegisAI</h2>
        <p class="subtitle">可信AI数据治理与隐私合规平台</p>
        <div class="version-badge">2026 · 企业版</div>
      </div>
      <el-form :model="form" @submit.prevent class="login-form">
        <el-form-item label="用户名" :rules="[{ required: true, message: '请输入用户名' }]">
          <el-input v-model="form.username" autocomplete="off" placeholder="请输入用户名" size="large" />
        </el-form-item>
        <el-form-item label="密码" :rules="[{ required: true, message: '请输入密码' }]">
          <el-input type="password" v-model="form.password" autocomplete="off" placeholder="请输入密码" size="large" show-password />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="captcha-wrapper">
            <el-input v-model="form.captcha" placeholder="请输入验证码" size="large" />
            <div class="captcha" @click="refreshCaptcha">{{ captcha }}</div>
          </div>
        </el-form-item>
        <div class="form-options">
          <el-checkbox v-model="remember">记住密码</el-checkbox>
          <el-link type="primary" @click="reset">忘记密码?</el-link>
        </div>
        <el-button type="primary" class="login-btn" @click="onSubmit" :loading="loading" size="large">
          <span v-if="!loading">登录</span>
          <span v-else>登录中...</span>
        </el-button>
      </el-form>
      <div class="login-footer">
        <p>安全登录 · 数据加密传输</p>
      </div>
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
.bg-illu { position:absolute; inset:0; background:radial-gradient(circle at 20% 20%, rgba(22,93,255,0.15), rgba(0,0,0,0)), radial-gradient(circle at 80% 0%, rgba(0,180,42,0.1), rgba(0,0,0,0)); filter: blur(30px); }
.bg-particles { position:absolute; inset:0; background-image: radial-gradient(rgba(22,93,255,0.1) 1px, transparent 1px), radial-gradient(rgba(0,180,42,0.08) 1px, transparent 1px); background-size: 50px 50px; background-position: 0 0, 25px 25px; opacity: 0.3; animation: particleFloat 20s linear infinite; }

.login-card { width:480px; position:relative; z-index:2; padding: 40px 36px; color: var(--color-text); animation: slideUp 0.6s ease-out; background: linear-gradient(135deg, rgba(255,255,255,0.12), rgba(255,255,255,0.08)); border: 1px solid rgba(255,255,255,0.15); }

.login-header { text-align: center; margin-bottom: 32px; }
.logo-icon { font-size: 48px; margin-bottom: 16px; animation: float 3s ease-in-out infinite; }
.title { text-align:center; margin: 0 0 8px; color:#fff; font-size: 28px; font-weight: 700; letter-spacing: 1px; text-shadow: 0 2px 8px rgba(0,0,0,0.4); }
.subtitle { text-align:center; color:var(--color-text-secondary); margin-bottom: 16px; font-size: 14px; line-height: 1.5; }
.version-badge { display: inline-block; padding: 4px 12px; border-radius: 999px; background: linear-gradient(135deg, rgba(22,93,255,0.25), rgba(0,180,42,0.2)); color: var(--color-text-secondary); font-size: 12px; font-weight: 600; border: 1px solid rgba(22,93,255,0.3); }

.login-form { margin-top: 24px; }
.captcha-wrapper { display: flex; gap: 12px; align-items: center; }
.captcha { padding: 0 20px; height: 40px; background: linear-gradient(135deg, rgba(22,93,255,0.25), rgba(0,180,42,0.2)); border-radius: 8px; cursor: pointer; letter-spacing: 6px; font-weight: 700; color: #fff; transition: all 0.3s ease; border: 1px solid rgba(22,93,255,0.3); font-size: 18px; display: flex; align-items: center; justify-content: center; min-width: 120px; }
.captcha:hover { background: linear-gradient(135deg, rgba(22,93,255,0.35), rgba(0,180,42,0.3)); transform: scale(1.05); box-shadow: 0 4px 12px rgba(22,93,255,0.3); }

.form-options { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.form-options .el-checkbox { color: var(--color-text-secondary); }
.form-options .el-link { font-weight: 500; }

.login-btn { width: 100%; height: 48px; font-size: 16px; font-weight: 600; letter-spacing: 1px; margin-top: 8px; }

.login-footer { text-align: center; margin-top: 24px; padding-top: 20px; border-top: 1px solid rgba(255,255,255,0.1); }
.login-footer p { color: var(--color-text-muted); font-size: 12px; margin: 0; }

@keyframes slideUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

@keyframes particleFloat {
  0% { background-position: 0 0, 25px 25px; }
  100% { background-position: 50px 50px, 75px 75px; }
}

.login-card :deep(.el-form-item__label) { color: var(--color-text-secondary); font-weight: 600; font-size: 14px; }
.login-card :deep(.el-input__wrapper) { background: rgba(255,255,255,0.1); border-radius: 10px; transition: all 0.3s ease; border: 1px solid rgba(255,255,255,0.1); box-shadow: none; }
.login-card :deep(.el-input__wrapper:hover) { background: rgba(255,255,255,0.15); border-color: rgba(22,93,255,0.3); }
.login-card :deep(.el-input__wrapper.is-focus) { background: rgba(255,255,255,0.18); border-color: rgba(22,93,255,0.5); box-shadow: 0 0 0 3px rgba(22,93,255,0.1); }
.login-card :deep(.el-input__inner) { color: var(--color-text); background: transparent; font-weight: 500; font-size: 14px; }
.login-card :deep(.el-input__inner::placeholder) { color: rgba(176,196,222,0.5); }
.login-card :deep(.el-checkbox__label) { color: var(--color-text-secondary); }
.login-card :deep(.el-link) { color: var(--color-primary-light); font-weight: 500; }
.login-card :deep(.el-link:hover) { color: var(--color-primary); }

/* 护眼模式 */
:global(body.light-theme) .login-card { background: linear-gradient(135deg, rgba(255,255,255,0.98), rgba(255,255,255,0.95)); border: 1px solid rgba(0,0,0,0.1); box-shadow: 0 20px 60px rgba(0,0,0,0.15); }
:global(body.light-theme) .title { color: #000000; text-shadow: none; }
:global(body.light-theme) .subtitle { color: #666666; }
:global(body.light-theme) .version-badge { color: #666666; border-color: rgba(22,93,255,0.4); }
:global(body.light-theme) .login-footer p { color: #999999; }
:global(body.light-theme) .login-card :deep(.el-form-item__label) { color: #666666; }
:global(body.light-theme) .login-card :deep(.el-input__wrapper) { background: rgba(0,0,0,0.03); border-color: rgba(0,0,0,0.1); }
:global(body.light-theme) .login-card :deep(.el-input__wrapper:hover) { background: rgba(0,0,0,0.05); border-color: rgba(22,93,255,0.3); }
:global(body.light-theme) .login-card :deep(.el-input__wrapper.is-focus) { background: rgba(0,0,0,0.05); border-color: rgba(22,93,255,0.5); }
:global(body.light-theme) .login-card :deep(.el-input__inner) { color: #000000; }
:global(body.light-theme) .login-card :deep(.el-input__inner::placeholder) { color: rgba(0,0,0,0.3); }
:global(body.light-theme) .login-card :deep(.el-checkbox__label) { color: #666666; }
:global(body.light-theme) .login-card :deep(.el-link) { color: #165dff; }
</style>
