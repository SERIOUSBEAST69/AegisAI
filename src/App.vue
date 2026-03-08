<template>
  <div class="app-shell" :class="{ 'light-mode': theme === 'light' }" v-if="!isLogin">
    <header class="app-header card-glass">
      <div class="brand" @click="go('/')">
        <div class="logo">
          <span class="dot" />
          <span class="logo-text">AegisAI</span>
        </div>
        <span class="subtitle">数据治理与隐私合规平台</span>
      </div>
      <div class="header-actions">
        <el-dropdown trigger="click" @command="handleDropdown">
          <div class="user-info">
            <el-avatar :size="32" :icon="UserFilled" />
            <span class="user-name">管理员</span>
            <el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人资料</el-dropdown-item>
              <el-dropdown-item command="settings">系统设置</el-dropdown-item>
              <el-dropdown-item command="theme">{{ themeLabel }}</el-dropdown-item>
              <el-dropdown-item command="logout" divided @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>
    <div class="layout">
      <aside class="app-aside card-glass">
        <el-menu 
          :default-active="active" 
          @select="go" 
          class="nav-menu"
          background-color="transparent"
          text-color="var(--color-text-secondary)"
          active-text-color="#ffffff"
        >
          <div class="menu-section">
            <div class="section-title">核心功能</div>
            <el-menu-item index="/">
              <el-icon><HomeFilled /></el-icon>
              <template #title>首页</template>
            </el-menu-item>
            <el-menu-item index="/data-asset">
              <el-icon><DataAnalysis /></el-icon>
              <template #title>数据资产</template>
            </el-menu-item>
            <el-menu-item index="/ai-model-manage">
              <el-icon><StarFilled /></el-icon>
              <template #title>AI模型</template>
            </el-menu-item>
            <el-menu-item index="/audit-log">
              <el-icon><Timer /></el-icon>
              <template #title>审计日志</template>
            </el-menu-item>
            <el-menu-item index="/model-cost">
              <el-icon><Money /></el-icon>
              <template #title>调用成本</template>
            </el-menu-item>
          </div>
          
          <div class="menu-section">
            <div class="section-title">安全合规</div>
            <el-menu-item index="/sensitive-scan">
              <el-icon><Search /></el-icon>
              <template #title>敏感扫描</template>
            </el-menu-item>
            <el-menu-item index="/alerts">
              <el-icon><Warning /></el-icon>
              <template #title>告警闭环</template>
            </el-menu-item>
            <el-menu-item index="/data-share">
              <el-icon><Share /></el-icon>
              <template #title>资产共享</template>
            </el-menu-item>
            <el-menu-item index="/subject-request">
              <el-icon><UserFilled /></el-icon>
              <template #title>主体权利</template>
            </el-menu-item>
            <el-menu-item index="/desense-preview">
              <el-icon><Lock /></el-icon>
              <template #title>脱敏预览</template>
            </el-menu-item>
            <el-menu-item index="/global-search">
              <el-icon><Search /></el-icon>
              <template #title>全局搜索</template>
            </el-menu-item>
          </div>
          
          <div class="menu-section">
            <div class="section-title">系统管理</div>
            <el-menu-item index="/user-manage">
              <el-icon><UserFilled /></el-icon>
              <template #title>用户管理</template>
            </el-menu-item>
            <el-menu-item index="/role-manage">
              <el-icon><Avatar /></el-icon>
              <template #title>角色管理</template>
            </el-menu-item>
            <el-menu-item index="/permission-manage">
              <el-icon><Key /></el-icon>
              <template #title>权限管理</template>
            </el-menu-item>
            <el-menu-item index="/approval-manage">
              <el-icon><Check /></el-icon>
              <template #title>审批管理</template>
            </el-menu-item>
            <el-menu-item index="/policy-manage">
              <el-icon><Document /></el-icon>
              <template #title>策略管理</template>
            </el-menu-item>
            <el-menu-item index="/risk-event-manage">
              <el-icon><Warning /></el-icon>
              <template #title>风险事件</template>
            </el-menu-item>
          </div>
        </el-menu>
      </aside>
      <main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
  <router-view v-else />
</template>

<script setup>
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  UserFilled,
  ArrowDown,
  HomeFilled,
  DataAnalysis,
  StarFilled,
  Timer,
  Money,
  Search,
  Warning,
  Share,
  Lock,
  Avatar,
  Key,
  Check,
  Document
} from '@element-plus/icons-vue';

const route = useRoute();
const router = useRouter();
const active = computed(() => route.path);
const isLogin = computed(() => route.path === '/login');
const go = (path) => router.push(path);
const logout = () => { localStorage.removeItem('token'); router.push('/login'); };

const theme = ref('light');
const themeLabel = computed(() => theme.value === 'dark' ? '白天模式' : '夜间模式');

// 从localStorage读取主题设置
const savedTheme = localStorage.getItem('theme');
if (savedTheme) {
  theme.value = savedTheme;
}

const toggleTheme = () => {
  theme.value = theme.value === 'dark' ? 'light' : 'dark';
  localStorage.setItem('theme', theme.value);
};

const handleDropdown = (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile');
      break;
    case 'settings':
      router.push('/settings');
      break;
    case 'theme':
      toggleTheme();
      break;
    case 'logout':
      logout();
      break;
  }
};
</script>

<style scoped>
.app-shell { 
  min-height: 100vh; 
  padding: 24px; 
  box-sizing: border-box; 
  transition: all var(--transition-normal);
}

.app-header { 
  height: 72px; 
  display: flex; 
  align-items: center; 
  justify-content: space-between; 
  padding: 0 24px; 
  position: relative;
  overflow: hidden;
}

.brand {
  display: flex;
  align-items: center;
  gap: 20px;
  cursor: pointer;
  flex: 1;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--color-text), var(--color-text-secondary));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  font-size: 14px;
  color: var(--color-text-muted);
  font-weight: 400;
  margin-left: 20px;
  padding-left: 20px;
  border-left: 1px solid var(--color-border-light);
}

.dot { 
  width: 12px; 
  height: 12px; 
  border-radius: 50%; 
  background: linear-gradient(135deg, var(--color-primary), var(--color-success)); 
  box-shadow: 0 0 12px rgba(22, 93, 255, 0.6); 
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); box-shadow: 0 0 20px rgba(22, 93, 255, 0.8); }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--gap-md);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  cursor: pointer;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid var(--color-border-light);
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.04);
  border-color: var(--color-border);
  box-shadow: var(--shadow-sm);
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
}

.layout { 
  display: grid; 
  grid-template-columns: 280px 1fr; 
  gap: 20px; 
  margin-top: 20px; 
  min-height: calc(100vh - 116px); 
  transition: all var(--transition-normal);
}

.app-aside { 
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.app-aside::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, var(--color-primary), transparent);
  border-radius: 50%;
  filter: blur(40px);
  opacity: 0.3;
  z-index: 0;
}

.nav-menu { 
  background: transparent; 
  border-right: none;
  position: relative;
  z-index: 1;
}

.menu-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin: 0 16px 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--color-border-light);
}

.app-main { 
  padding: 0;
  position: relative;
}

.app-main::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 120px;
  height: 120px;
  background: linear-gradient(135deg, var(--color-success), transparent);
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.2;
  z-index: 0;
}

.app-main > * { 
  background: transparent;
  position: relative;
  z-index: 1;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .layout {
    grid-template-columns: 240px 1fr;
  }
  
  .app-shell {
    padding: 16px;
  }
  
  .app-header {
    padding: 0 16px;
  }
  
  .subtitle {
    display: none;
  }
}

@media (max-width: 768px) {
  .layout {
    grid-template-columns: 1fr;
  }
  
  .app-aside {
    display: none;
  }
  
  .brand {
    gap: 12px;
  }
  
  .logo-text {
    font-size: 16px;
  }
}
</style>
