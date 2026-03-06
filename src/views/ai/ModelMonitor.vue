<template>
  <div class="page">
    <div class="header">
      <div>
        <h2>调用监控</h2>
        <p class="hint">近 500 次调用聚合，展示成功率与耗时；趋势图无数据时将显示为空</p>
      </div>
      <el-button @click="fetchData">刷新</el-button>
    </div>
    <div class="cards">
      <el-card v-for="item in summary" :key="item.modelCode" class="card">
        <div class="card-title">{{ item.modelCode }} ({{ item.provider }})</div>
        <div class="metric">今日调用：{{ item.total }}</div>
        <div class="metric">成功率：{{ successRate(item) }}%</div>
        <div class="metric">平均耗时：{{ item.avgDuration }} ms</div>
      </el-card>
    </div>
    <div ref="chartRef" class="chart"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import * as echarts from 'echarts';
import request from '../../api/request';

const summary = ref([]);
const trend = ref([]);
const chartRef = ref();
let chart;

function successRate(item) {
  if (!item.total) return 0;
  return Math.round((item.success / item.total) * 100);
}

async function fetchData() {
  summary.value = await request.get('/ai/monitor/summary');
  trend.value = await request.get('/ai/monitor/trend');
}

function renderChart() {
  if (!chartRef.value) return;
  if (!chart) chart = echarts.init(chartRef.value);
  const x = summary.value.map(i => i.modelCode);
  const trendSeries = buildTrendSeries();
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['调用次数', '成功率%', ...trendSeries.legend] },
    xAxis: { type: 'category', data: x },
    yAxis: [
      { type: 'value', name: '次数' },
      { type: 'value', name: '成功率%', min: 0, max: 100 }
    ],
    series: [
      { name: '调用次数', type: 'bar', data: summary.value.map(i => i.total) },
      { name: '成功率%', type: 'line', yAxisIndex: 1, data: summary.value.map(successRate) },
      ...trendSeries.series
    ]
  });
}

function buildTrendSeries() {
  if (!trend.value || trend.value.length === 0) return { legend: [], series: [] };
  const grouped = {};
  const datesSet = new Set();
  trend.value.forEach(item => {
    datesSet.add(item.date);
    if (!grouped[item.modelCode]) grouped[item.modelCode] = {};
    grouped[item.modelCode][item.date] = item.total;
  });
  const dates = Array.from(datesSet).sort();
  const series = Object.keys(grouped).map(code => ({
    name: `${code} 趋势`,
    type: 'line',
    smooth: true,
    data: dates.map(d => grouped[code][d] || 0)
  }));
  return { legend: series.map(s => s.name), series };
}

watch(summary, renderChart);
watch(trend, renderChart);

onMounted(() => {
  fetchData().then(renderChart);
  window.addEventListener('resize', () => chart && chart.resize());
});
</script>

<style scoped>
.page { padding: 16px; }
.header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.hint { color: #6b7280; margin: 4px 0 0; }
.cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; margin-bottom: 16px; }
.card-title { font-weight: 600; margin-bottom: 4px; }
.metric { color: #374151; margin: 2px 0; }
.chart { width: 100%; height: 360px; }
</style>
