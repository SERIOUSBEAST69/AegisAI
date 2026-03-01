<template>
  <div class="page-grid">
    <el-card class="card-glass">
      <div class="card-header">告警闭环管理</div>
      <el-button type="primary" @click="refresh">刷新</el-button>
      <el-table :data="list" style="margin-top:12px" v-loading="loading">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="level" label="等级" />
        <el-table-column prop="status" label="状态" />
        <el-table-column prop="assigneeId" label="处理人" />
        <el-table-column prop="relatedLogId" label="关联日志" />
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-select v-model="scope.row.status" size="small" style="width:140px" @change="update(scope.row)">
              <el-option label="open" value="open" />
              <el-option label="claimed" value="claimed" />
              <el-option label="resolved" value="resolved" />
              <el-option label="archived" value="archived" />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import request from '../api/request';

const list = ref([]);
const loading = ref(false);

async function refresh() {
  loading.value = true;
  list.value = await request.get('/alert/list');
  loading.value = false;
}

async function update(row) {
  await request.post('/alert/update', row);
  refresh();
}

refresh();
</script>

<style scoped>
.page-grid { display: grid; gap: 16px; }
.card-header { font-weight: 600; margin-bottom: 12px; }
</style>
