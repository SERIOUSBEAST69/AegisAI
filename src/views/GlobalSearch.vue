<template>
  <div class="page-grid">
    <el-card class="card-glass">
      <div class="card-header">全局搜索</div>
      <el-input v-model="keyword" placeholder="输入关键词，跨资产/模型/日志/审批搜索" @keyup.enter="search" />
      <el-button type="primary" style="margin-top:10px" @click="search">搜索</el-button>
      <el-skeleton v-if="loading" animated :rows="4" class="skeleton-card" />
      <div v-else style="margin-top:14px; display:grid; gap:12px;">
        <el-card class="card-glass" shadow="never">
          <div class="section-title">数据资产</div>
          <el-tag v-for="a in data.assets" :key="a.id" type="info" style="margin:4px;">{{ a.name }}</el-tag>
        </el-card>
        <el-card class="card-glass" shadow="never">
          <div class="section-title">AI 模型</div>
          <el-tag v-for="m in data.models" :key="m.id" type="success" style="margin:4px;">{{ m.name }}</el-tag>
        </el-card>
        <el-card class="card-glass" shadow="never">
          <div class="section-title">审计日志</div>
          <el-table :data="data.auditLogs" max-height="220">
            <el-table-column prop="operation" label="操作" />
            <el-table-column prop="operationTime" label="时间" />
            <el-table-column prop="result" label="结果" />
          </el-table>
        </el-card>
        <el-card class="card-glass" shadow="never">
          <div class="section-title">审批单</div>
          <el-table :data="data.approvals" max-height="220">
            <el-table-column prop="applicantId" label="申请人" />
            <el-table-column prop="assetId" label="资产" />
            <el-table-column prop="status" label="状态" />
          </el-table>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import request from '../api/request';

const keyword = ref('');
const loading = ref(false);
const data = reactive({ assets: [], models: [], auditLogs: [], approvals: [] });

async function search() {
  if (!keyword.value) return;
  loading.value = true;
  const res = await request.get('/search/global', { params: { keyword: keyword.value } });
  Object.assign(data, res || {});
  loading.value = false;
}
</script>

<style scoped>
.page-grid { display: grid; gap: 16px; }
.card-header { font-weight: 600; margin-bottom: 12px; }
.section-title { font-weight: 600; margin-bottom: 8px; }
</style>
