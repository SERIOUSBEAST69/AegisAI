<template>
  <div class="home-grid">
    <el-card class="hero card-glass">
      <div class="hero-left">
        <div class="chip">2026 · 合规大屏</div>
        <h1>可信AI数据治理与隐私合规中枢</h1>
        <p>全链路数据资产可视化 · 国产AI工具合规接入 · 审计追溯一键生成</p>
        <el-button type="primary" class="floating-btn" @click="$router.push('/global-search')">
          <el-icon><Search /></el-icon>
          全局搜索
        </el-button>
      </div>
      <div class="hero-right">
        <div class="orb">
          <div class="orb-core"></div>
          <div class="orb-ring orb-ring-1"></div>
          <div class="orb-ring orb-ring-2"></div>
          <div class="orb-ring orb-ring-3"></div>
        </div>
      </div>
    </el-card>

    <div class="stat-grid">
      <stat-card 
        title="敏感数据资产" 
        :value="stats.dataAsset" 
        suffix="个"
        icon="DataAnalysis"
        color="var(--color-primary)"
      />
      <stat-card 
        title="AI模型" 
        :value="stats.aiModel" 
        suffix="个"
        icon="StarFilled"
        color="var(--color-success)"
      />
      <stat-card 
        title="风险事件" 
        :value="stats.riskEvent" 
        suffix="起"
        icon="Warning"
        color="var(--color-warning)"
      />
      <stat-card 
        title="用户" 
        :value="stats.user" 
        suffix="人"
        icon="UserFilled"
        color="var(--color-info)"
      />
    </div>

    <el-card class="card-glass" style="grid-column: span 2;">
      <div class="card-header">风险趋势预测（近7天）</div>
      <el-skeleton v-if="loading" animated :rows="3" class="skeleton-card" />
      <el-table v-else :data="trendRows" max-height="280">
        <el-table-column prop="hour" label="时段" width="120" />
        <el-table-column prop="count" label="事件数">
          <template #default="scope">
            <div class="stat-value">
              {{ scope.row.count }}
              <span class="trend-indicator" :class="{ positive: scope.row.count > 5, negative: scope.row.count <= 5 }">
                {{ scope.row.count > 5 ? '↑' : '↓' }}
              </span>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="forecast">
        <el-icon class="forecast-icon"><Warning /></el-icon>
        下一时段预测：<span class="forecast-value">{{ forecast }}</span>
      </div>
    </el-card>

    <el-card class="card-glass" style="grid-column: span 1;">
      <div class="card-header">待办速览</div>
      <el-timeline>
        <el-timeline-item 
          v-for="item in todo" 
          :key="item.text" 
          :timestamp="item.time" 
          type="primary"
          placement="top"
        >
          <div class="todo-item">
            <div class="todo-text">{{ item.text }}</div>
            <div class="todo-actions">
              <el-button size="small" type="primary" text>处理</el-button>
              <el-button size="small" text>详情</el-button>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import request from '../api/request';
import StatCard from '../components/StatCard.vue';
import { ElMessage } from 'element-plus';
import { Search, Warning } from '@element-plus/icons-vue';

const stats = ref({ dataAsset: 0, aiModel: 0, user: 0, riskEvent: 0 });
const trend = ref({ perHour: {}, forecastNextHour: 0 });
const loading = ref(false);
const todo = ref([
  { text: '审批高敏数据共享申请', time: '待办' },
  { text: '处理 2 条风险告警', time: '今日' },
  { text: '导出合规审计报告', time: '本周' }
]);

const trendRows = computed(() => Object.entries(trend.value.perHour || {}).map(([hour, count]) => ({ hour: `${hour}:00`, count })));
const forecast = computed(() => trend.value.forecastNextHour || 0);

async function fetchStats() {
  try {
    stats.value = await request.get('/dashboard/stats');
  } catch (e) {
    ElMessage.error('统计数据加载失败');
  }
}

async function fetchTrend() {
  loading.value = true;
  try {
    trend.value = await request.get('/risk/trend');
  } catch (e) {
    trend.value = { perHour: {}, forecastNextHour: 0 };
    ElMessage.error('风险趋势加载失败');
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  fetchStats();
  fetchTrend();
});
</script>

<style scoped>
.home-grid { 
  display: grid; 
  grid-template-columns: repeat(2, 1fr); 
  gap: 20px; 
  position: relative;
  z-index: 1;
}

.hero { 
  grid-column: span 2; 
  display:flex; 
  justify-content:space-between; 
  align-items:center; 
  padding: 40px 48px;
  position: relative;
  overflow: hidden;
}

.hero::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, var(--color-primary), transparent);
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
  z-index: 0;
}

.hero-left {
  flex: 1;
  position: relative;
  z-index: 1;
}

.hero-left h1 {
  margin: 12px 0 16px;
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--color-text), var(--color-text-secondary));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1.2;
}

.hero-left p {
  color: var(--color-text-muted);
  margin: 0 0 24px;
  font-size: 16px;
  line-height: 1.5;
  max-width: 600px;
}

.chip {
  display:inline-block;
  padding: 8px 16px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(22, 93, 255, 0.15), rgba(0, 180, 42, 0.15));
  color: var(--color-text-secondary);
  font-size: 12px;
  font-weight: 500;
  border: 1px solid rgba(22, 93, 255, 0.2);
}

.hero-right {
  width: 240px;
  height: 240px;
  position:relative;
  z-index: 1;
}

.orb {
  position:relative;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.orb-core {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: radial-gradient(circle, var(--color-primary), var(--color-primary-dark));
  box-shadow: 0 0 30px rgba(22, 93, 255, 0.6);
  animation: pulse 3s ease-in-out infinite;
}

.orb-ring {
  position: absolute;
  border: 2px solid rgba(22, 93, 255, 0.3);
  border-radius: 50%;
  animation: rotate 20s linear infinite;
}

.orb-ring-1 {
  width: 120px;
  height: 120px;
  animation-delay: 0s;
}

.orb-ring-2 {
  width: 160px;
  height: 160px;
  animation-delay: -5s;
  border-color: rgba(0, 180, 42, 0.3);
}

.orb-ring-3 {
  width: 200px;
  height: 200px;
  animation-delay: -10s;
  border-color: rgba(255, 125, 0, 0.3);
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.stat-grid {
  display:grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  grid-column: span 2;
}

.card-header {
  font-weight: 600;
  margin-bottom: 16px;
  color: var(--color-text);
  font-size: 18px;
}

.forecast {
  margin-top: 16px;
  color: var(--color-text-muted);
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: rgba(255, 125, 0, 0.1);
  border-radius: var(--radius-md);
  border: 1px solid rgba(255, 125, 0, 0.2);
}

.forecast-icon {
  color: var(--color-warning);
  font-size: 16px;
}

.forecast-value {
  color: var(--color-warning);
  font-weight: 700;
  font-size: 16px;
  margin-left: 4px;
}

.stat-value {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.trend-indicator {
  font-size: 12px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: var(--radius-sm);
}

.trend-indicator.positive {
  color: var(--color-danger);
  background: rgba(245, 63, 63, 0.1);
}

.trend-indicator.negative {
  color: var(--color-success);
  background: rgba(0, 180, 42, 0.1);
}

.todo-item {
  background: var(--color-card-strong);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 12px;
  transition: all var(--transition-fast);
}

.todo-item:hover {
  background: var(--color-card-hover);
  border-color: var(--color-primary-light);
  box-shadow: var(--shadow-sm);
}

.todo-text {
  font-size: 14px;
  color: var(--color-text);
  margin-bottom: 8px;
  line-height: 1.4;
}

.todo-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .hero {
    padding: 32px 24px;
  }
  
  .hero-left h1 {
    font-size: 24px;
  }
  
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .home-grid {
    grid-template-columns: 1fr;
  }
  
  .hero {
    flex-direction: column;
    text-align: center;
    gap: 24px;
  }
  
  .hero-right {
    width: 180px;
    height: 180px;
  }
  
  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
