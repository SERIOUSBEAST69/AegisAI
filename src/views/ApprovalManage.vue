<template>
  <el-card>
    <h2>审批流管理</h2>
    <el-form :inline="true" @submit.prevent>
      <el-form-item label="申请人ID">
        <el-input v-model="query.applicantId" placeholder="申请人ID" />
      </el-form-item>
      <el-form-item label="资产ID">
        <el-input v-model="query.assetId" placeholder="资产ID" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="fetchApprovals">查询</el-button>
        <el-button @click="showAdd = true">新建申请</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="approvals" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="applicantId" label="申请人ID" />
      <el-table-column prop="assetId" label="资产ID" />
      <el-table-column prop="reason" label="理由" />
      <el-table-column prop="status" label="状态" />
      <el-table-column prop="approverId" label="审批人ID" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button size="mini" @click="approve(scope.row, '通过')">通过</el-button>
          <el-button size="mini" type="danger" @click="approve(scope.row, '拒绝')">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="showAdd" title="新建审批申请">
      <el-form :model="addForm">
        <el-form-item label="申请人ID"><el-input v-model="addForm.applicantId" /></el-form-item>
        <el-form-item label="资产ID"><el-input v-model="addForm.assetId" /></el-form-item>
        <el-form-item label="理由"><el-input v-model="addForm.reason" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="addApproval">提交</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { ref } from 'vue';
import request from '../api/request';
const approvals = ref([]);
const loading = ref(false);
const showAdd = ref(false);
const addForm = ref({ applicantId: '', assetId: '', reason: '' });
const query = ref({ applicantId: '', assetId: '' });
async function fetchApprovals() {
  loading.value = true;
  const res = await request.get('/approval/list', { params: query.value });
  approvals.value = res || [];
  loading.value = false;
}
async function addApproval() {
  await request.post('/approval/apply', addForm.value);
  showAdd.value = false;
  fetchApprovals();
}
async function approve(row, status) {
  await request.post('/approval/approve', { requestId: row.id, approverId: 1, status });
  fetchApprovals();
}
fetchApprovals();
</script>
