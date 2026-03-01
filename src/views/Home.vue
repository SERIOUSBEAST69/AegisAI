<template>
  <div class="home-grid">
    <el-card class="hero card-glass">
      <div class="hero-left">
        <div class="chip">2026 · 合规大屏</div>
        <h2>可信AI数据治理与隐私合规中枢</h2>
        <p>全链路数据资产可视化 · 国产AI工具合规接入 · 审计追溯一键生成</p>
        <el-button type="primary" class="floating-btn" @click="$router.push('/global-search')">全局搜索</el-button>
      </div>
      <div class="hero-right">
        <div class="orb"></div>
      </div>
    </el-card>

    <div class="stat-grid">
      <stat-card title="敏感数据资产" :value="stats.dataAsset" suffix="个" />
      <stat-card title="AI模型" :value="stats.aiModel" suffix="个" />
      <stat-card title="风险事件" :value="stats.riskEvent" suffix="起" />
      <stat-card title="用户" :value="stats.user" suffix="人" />
    </div>

    <el-card class="card-glass" style="grid-column: span 2;">
      <div class="card-header">风险趋势预测（近7天）</div>
      <el-skeleton v-if="loading" animated :rows="3" class="skeleton-card" />
      <el-table v-else :data="trendRows" max-height="240">
        <el-table-column prop="hour" label="时段" />
        <el-table-column prop="count" label="事件数" />
      </el-table>
      <div class="forecast">下一时段预测：<span>{{ forecast }}</span></div>
    </el-card>

    <el-card class="card-glass" style="grid-column: span 1;">
      <div class="card-header">待办速览</div>
      <el-timeline>
        <el-timeline-item v-for="item in todo" :key="item.text" :timestamp="item.time" type="primary">{{ item.text }}</el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import request from '../api/request';
import StatCard from '../components/StatCard.vue';
import { ElMessage } from 'element-plus';

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
.home-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.hero { grid-column: span 2; display:flex; justify-content:space-between; align-items:center; padding: 26px; }
.hero-left h2 { margin: 6px 0; color:#fff; }
.hero-left p { color:#9fb2ce; margin:0 0 12px; }
.chip { display:inline-block; padding:6px 10px; border-radius:999px; background: rgba(22,93,255,0.15); color:#9fb2ce; font-size:12px; }
.hero-right { width:180px; height:120px; position:relative; }
.orb { position:absolute; inset:0; border-radius:50%; background:radial-gradient(circle, rgba(22,93,255,0.5), rgba(0,0,0,0)); box-shadow: 0 0 40px rgba(22,93,255,0.45); }
.stat-grid { display:grid; grid-template-columns: repeat(4, 1fr); gap: 12px; grid-column: span 2; }
.card-header { font-weight: 600; margin-bottom: 12px; color:#e8edf7; }
.forecast { margin-top: 10px; color:#9fb2ce; }
.forecast span { color:#00b42a; font-weight:700; }
</style>
