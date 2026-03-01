<template>
  <div class="app-shell" v-if="!isLogin">
    <header class="app-header card-glass">
      <div class="brand" @click="go('/')">
        <span class="dot" />
        <span class="title">AegisAI · 数据治理与隐私合规</span>
      </div>
      <div class="header-actions">
        <el-button class="floating-btn" size="small" type="primary" @click="go('/')">首页</el-button>
        <el-button class="floating-btn" size="small" @click="toggleTheme">{{ themeLabel }}</el-button>
        <el-button class="floating-btn" size="small" type="danger" @click="logout">退出</el-button>
      </div>
    </header>
    <div class="layout">
      <aside class="app-aside card-glass">
        <el-menu :default-active="active" @select="go" class="nav-menu">
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/data-asset">数据资产</el-menu-item>
          <el-menu-item index="/ai-model-manage">AI模型</el-menu-item>
          <el-menu-item index="/audit-log">审计日志</el-menu-item>
          <el-menu-item index="/model-cost">调用成本</el-menu-item>
          <el-menu-item index="/sensitive-scan">敏感扫描</el-menu-item>
          <el-menu-item index="/alerts">告警闭环</el-menu-item>
          <el-menu-item index="/data-share">资产共享</el-menu-item>
          <el-menu-item index="/subject-request">主体权利</el-menu-item>
          <el-menu-item index="/desense-preview">脱敏预览</el-menu-item>
          <el-menu-item index="/global-search">全局搜索</el-menu-item>
          <el-menu-item index="/user-manage">用户</el-menu-item>
          <el-menu-item index="/role-manage">角色</el-menu-item>
          <el-menu-item index="/permission-manage">权限</el-menu-item>
          <el-menu-item index="/approval-manage">审批</el-menu-item>
          <el-menu-item index="/policy-manage">策略</el-menu-item>
          <el-menu-item index="/risk-event-manage">风险</el-menu-item>
        </el-menu>
      </aside>
      <main class="app-main">
        <transition name="fade-slide" mode="out-in">
          <router-view />
        </transition>
      </main>
    </div>
  </div>
  <router-view v-else />
</template>

<script setup>
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
const route = useRoute();
const router = useRouter();
const active = computed(() => route.path);
const isLogin = computed(() => route.path === '/login');
const go = (path) => router.push(path);
const logout = () => { localStorage.removeItem('token'); router.push('/login'); };

const theme = ref('dark');
const themeLabel = computed(() => theme.value === 'dark' ? '护眼模式' : '科技蓝');
const toggleTheme = () => {
  theme.value = theme.value === 'dark' ? 'light' : 'dark';
  document.body.style.background = theme.value === 'dark'
    ? 'radial-gradient(circle at 20% 20%, rgba(22,93,255,0.15), rgba(0,0,0,0)), radial-gradient(circle at 80% 0%, rgba(0,180,42,0.12), rgba(0,0,0,0)), #0b1221'
    : '#f5f7fb';
};
</script>

<style scoped>
.app-shell { min-height: 100vh; padding: 20px; box-sizing: border-box; }
.app-header { height: 64px; display: flex; align-items: center; justify-content: space-between; padding: 0 16px; }
.brand { display: flex; align-items: center; gap: 10px; cursor: pointer; color: #fff; font-weight: 700; }
.dot { width: 10px; height: 10px; border-radius: 50%; background: linear-gradient(135deg, #165dff, #00b42a); box-shadow: 0 0 10px rgba(22,93,255,0.6); }
.title { letter-spacing: 0.3px; }
.layout { display: grid; grid-template-columns: 240px 1fr; gap: 16px; margin-top: 16px; min-height: calc(100vh - 90px); }
.app-aside { padding: 12px; }
.nav-menu { background: transparent; border-right: none; }
.app-main { padding: 0; }
.app-main > * { background: transparent; }
</style>
