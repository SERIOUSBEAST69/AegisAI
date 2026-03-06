<template>
  <div class="page-grid">
    <el-card class="card-glass">
      <div class="card-header">数据资产共享审批</div>
      <el-form :inline="true" @submit.prevent ref="applyFormRef" :model="form" :rules="rules">
        <el-form-item label="资产ID" prop="assetId"><el-input v-model="form.assetId" /></el-form-item>
        <el-form-item label="申请人" prop="applicantId"><el-input v-model="form.applicantId" /></el-form-item>
        <el-form-item label="协作人ID，逗号分隔"><el-input v-model="form.collaborators" /></el-form-item>
        <el-form-item label="理由" prop="reason"><el-input v-model="form.reason" style="width:240px" /></el-form-item>
        <el-button type="primary" :loading="saving" @click="apply">提交申请</el-button>
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
            <el-button size="small" type="success" :loading="actionId===scope.row.id && actionType==='approve'" @click="approve(scope.row.id, 'approved')">通过</el-button>
            <el-button size="small" type="danger" :loading="actionId===scope.row.id && actionType==='reject'" @click="approve(scope.row.id, 'rejected')">拒绝</el-button>
            <el-button size="small" type="warning" :loading="actionId===scope.row.id && actionType==='delete'" @click="remove(scope.row.id)" style="margin-left:6px">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../api/request';

const form = ref({ assetId: '', applicantId: '', collaborators: '', reason: '' });
const list = ref([]);
const loading = ref(false);
const saving = ref(false);
const actionId = ref(null);
const actionType = ref('');
const applyFormRef = ref();
const rules = {
  assetId: [{ required: true, message: '资产ID不能为空', trigger: 'blur' }],
  applicantId: [{ required: true, message: '申请人不能为空', trigger: 'blur' }],
  reason: [{ required: true, message: '理由不能为空', trigger: 'blur' }]
};

async function load() {
  loading.value = true;
  try {
    list.value = await request.get('/data-share/list');
  } catch (err) {
    ElMessage.error(err?.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

async function apply() {
  if (!applyFormRef.value) return;
  applyFormRef.value.validate(async valid => {
    if (!valid) return;
    saving.value = true;
    try {
      await request.post('/data-share/apply', form.value);
      ElMessage.success('提交成功');
      load();
    } catch (err) {
      ElMessage.error(err?.message || '提交失败');
    } finally {
      saving.value = false;
    }
  });
}

async function approve(id, status) {
  actionId.value = id; actionType.value = status === 'approved' ? 'approve' : 'reject';
  try {
    await request.post('/data-share/approve', { id, status, approverId: 1 });
    ElMessage.success('处理成功');
    load();
  } catch (err) {
    ElMessage.error(err?.message || '处理失败');
  } finally {
    actionId.value = null; actionType.value = '';
  }
}

async function remove(id) {
  try {
    await ElMessageBox.confirm('确认删除该申请吗？', '提示', { type: 'warning' });
    actionId.value = id; actionType.value = 'delete';
    await request.post('/data-share/delete', { id });
    ElMessage.success('删除成功');
    load();
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(err?.message || '删除失败');
  } finally {
    actionId.value = null; actionType.value = '';
  }
}

load();
</script>

<style scoped>
.page-grid { display: grid; gap: 16px; }
.card-header { font-weight: 600; margin-bottom: 12px; }
</style>
