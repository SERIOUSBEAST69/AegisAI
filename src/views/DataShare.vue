<template>
  <div class="page-grid">
    <el-card class="card-glass">
      <div class="card-header">数据资产共享审批</div>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="资产ID"><el-input v-model="form.assetId" /></el-form-item>
        <el-form-item label="申请人"><el-input v-model="form.applicantId" /></el-form-item>
        <el-form-item label="协作人ID，逗号分隔"><el-input v-model="form.collaborators" /></el-form-item>
        <el-form-item label="理由"><el-input v-model="form.reason" style="width:240px" /></el-form-item>
        <el-button type="primary" @click="apply">提交申请</el-button>
      </el-form>
      <el-table :data="list" style="margin-top:12px" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="assetId" label="资产ID" />
        <el-table-column prop="applicantId" label="申请人" />
        <el-table-column prop="collaborators" label="协作人" />
        <el-table-column prop="reason" label="理由" />
        <el-table-column prop="status" label="状态" />
        <el-table-column label="审批" width="200">
          <template #default="scope">
            <el-button size="small" type="success" @click="approve(scope.row.id, 'approved')">通过</el-button>
            <el-button size="small" type="danger" @click="approve(scope.row.id, 'rejected')">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import request from '../api/request';

const form = ref({ assetId: '', applicantId: '', collaborators: '', reason: '' });
const list = ref([]);
const loading = ref(false);

async function load() {
  loading.value = true;
  list.value = await request.get('/data-share/list');
  loading.value = false;
}

async function apply() {
  await request.post('/data-share/apply', form.value);
  load();
}

async function approve(id, status) {
  await request.post('/data-share/approve', { id, status, approverId: 1 });
  load();
}

load();
</script>

<style scoped>
.page-grid { display: grid; gap: 16px; }
.card-header { font-weight: 600; margin-bottom: 12px; }
</style>
