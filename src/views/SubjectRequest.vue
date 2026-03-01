<template>
  <div class="page-grid">
    <el-card class="card-glass">
      <div class="card-header">数据主体权利工单</div>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="用户ID"><el-input v-model="form.userId" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width:140px">
            <el-option label="查询" value="access" />
            <el-option label="导出" value="export" />
            <el-option label="删除" value="delete" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.comment" style="width:220px" /></el-form-item>
        <el-button type="primary" @click="create">提交申请</el-button>
      </el-form>
      <el-table :data="list" style="margin-top:12px" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="userId" label="用户ID" />
        <el-table-column prop="type" label="类型" />
        <el-table-column prop="status" label="状态" />
        <el-table-column prop="comment" label="备注" />
        <el-table-column prop="handlerId" label="处理人" />
        <el-table-column label="处理" width="220">
          <template #default="scope">
            <el-select v-model="scope.row.status" size="small" style="width:140px" @change="update(scope.row)">
              <el-option label="pending" value="pending" />
              <el-option label="processing" value="processing" />
              <el-option label="done" value="done" />
              <el-option label="rejected" value="rejected" />
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

const form = ref({ userId: '', type: 'access', comment: '' });
const list = ref([]);
const loading = ref(false);

async function load() {
  loading.value = true;
  list.value = await request.get('/subject-request/list');
  loading.value = false;
}

async function create() {
  await request.post('/subject-request/create', form.value);
  load();
}

async function update(row) {
  await request.post('/subject-request/process', row);
  load();
}

load();
</script>

<style scoped>
.page-grid { display: grid; gap: 16px; }
.card-header { font-weight: 600; margin-bottom: 12px; }
</style>
