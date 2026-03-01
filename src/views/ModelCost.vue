<template>
  <div class="page-grid">
    <el-card class="card-glass">
      <div class="card-header">AI 调用成本统计</div>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="模型ID"><el-input v-model="query.modelId" /></el-form-item>
        <el-form-item label="用户ID"><el-input v-model="query.userId" /></el-form-item>
        <el-button type="primary" @click="fetch">查询</el-button>
      </el-form>
      <el-skeleton v-if="loading" animated :rows="3" class="skeleton-card" />
      <el-row v-else :gutter="16">
        <el-col :span="6"><stat-card title="总调用" :value="summary.callCount" suffix="次" /></el-col>
        <el-col :span="6"><stat-card title="平均延迟" :value="summary.avgLatencyMs" suffix="ms" /></el-col>
        <el-col :span="6"><stat-card title="成本" :value="(summary.costCents/100).toFixed(2)" suffix="元" /></el-col>
        <el-col :span="6"><stat-card title="时间范围" :value="rangeLabel" /></el-col>
      </el-row>
      <el-table :data="list" style="margin-top:16px;" v-loading="loading">
        <el-table-column prop="modelId" label="模型ID" />
        <el-table-column prop="userId" label="用户ID" />
        <el-table-column prop="date" label="日期" />
        <el-table-column prop="callCount" label="调用次数" />
        <el-table-column prop="totalLatencyMs" label="总耗时(ms)" />
        <el-table-column prop="costCents" label="成本(分)" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import request from '../api/request';
import StatCard from '../components/StatCard.vue';

const list = ref([]);
const summary = ref({});
const loading = ref(false);
const query = ref({ modelId: '', userId: '' });

const rangeLabel = computed(() => {
  if (!summary.value.from) return '-';
  return `${summary.value.from || ''} ~ ${summary.value.to || ''}`;
});

async function fetch() {
  loading.value = true;
  summary.value = await request.get('/model-stat/summary', { params: query.value });
  list.value = await request.get('/model-stat/list', { params: query.value });
  loading.value = false;
}

fetch();
</script>

<style scoped>
.page-grid { display: grid; gap: 16px; }
.card-header { font-weight: 600; margin-bottom: 12px; }
</style>
