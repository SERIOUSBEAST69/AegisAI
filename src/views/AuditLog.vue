<template>
  <el-card>
    <h2>审计日志查询</h2>
    <el-form :inline="true" @submit.prevent>
      <el-form-item label="用户ID">
        <el-input v-model="query.userId" placeholder="用户ID" />
      </el-form-item>
      <el-form-item label="资产ID">
        <el-input v-model="query.assetId" placeholder="资产ID" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="fetchLogs">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="logs" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="userId" label="用户ID" />
      <el-table-column prop="assetId" label="资产ID" />
      <el-table-column prop="operation" label="操作类型" />
      <el-table-column prop="operationTime" label="操作时间" />
      <el-table-column prop="result" label="结果" />
    </el-table>
  </el-card>
</template>
<script setup>
import { ref } from 'vue';
import request from '../api/request';
const logs = ref([]);
const loading = ref(false);
const query = ref({ userId: '', assetId: '' });
async function fetchLogs() {
  loading.value = true;
  const res = await request.post('/audit-log/search', query.value);
  logs.value = res || [];
  loading.value = false;
}
fetchLogs();
</script>
