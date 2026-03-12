<template>
  <div class="page">
    <div class="header">
      <div>
        <h2>AI 模型列表</h2>
        <p class="hint">仅接入大赛指定国产免费模型，密钥脱敏显示</p>
      </div>
      <el-button type="primary" @click="openAdd">新增模型</el-button>
    </div>
    <el-table :data="models" class="page-table" stripe v-loading="loading">
      <el-table-column prop="modelName" label="名称" />
      <el-table-column prop="modelCode" label="代码" width="160" />
      <el-table-column prop="provider" label="供应商" width="120" />
      <el-table-column prop="modelType" label="类型" width="100" />
      <el-table-column prop="riskLevel" label="风险" width="100" />
      <el-table-column prop="callLimit" label="每日限额" width="110" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'enabled' ? 'success' : 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="apiKey" label="密钥(脱敏)" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <model-form
      :visible="formVisible"
      :title="formTitle"
      :model="current"
      :loading="saving"
      @submit="handleSubmit"
      @close="formVisible = false"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue';
import request from '../../api/request';
import ModelForm from './ModelForm.vue';
import { ElMessage, ElMessageBox } from 'element-plus';

const models = ref([]);
const loading = ref(false);
const saving = ref(false);
const formVisible = ref(false);
const formTitle = ref('新增模型');
const current = ref({});

async function fetchData() {
  loading.value = true;
  try {
    models.value = await request.get('/ai-model/list');
  } finally {
    loading.value = false;
  }
}

function openAdd() {
  current.value = {};
  formTitle.value = '新增模型';
  formVisible.value = true;
}
function openEdit(row) {
  current.value = { ...row };
  formTitle.value = '编辑模型';
  formVisible.value = true;
}

async function handleSubmit(payload) {
  saving.value = true;
  try {
    if (payload.id) {
      await request.post('/ai-model/update', payload);
    } else {
      await request.post('/ai-model/add', payload);
    }
    ElMessage.success('保存成功');
    formVisible.value = false;
    await fetchData();
  } finally {
    saving.value = false;
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确认删除该模型吗？', '提示');
    await request.post('/ai-model/delete', { id });
    models.value = models.value.filter(model => model.id !== id);
    ElMessage.success('已删除');
    await fetchData();
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') {
      ElMessage.error(err?.message || '删除失败');
    }
  }
}

fetchData();
</script>

<style scoped>
.page { padding: 16px; color: var(--color-text); }
.header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.header h2 { margin: 0; color: var(--color-text); }
.hint { color: var(--color-text-secondary); margin: 4px 0 0; }

:deep(.page-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-row-hover-bg-color: rgba(255, 255, 255, 0.04);
  --el-table-header-bg-color: rgba(255, 255, 255, 0.04);
  --el-table-border-color: var(--color-border-light);
  --el-table-text-color: var(--color-text);
  --el-table-header-text-color: var(--color-text-secondary);
}
</style>
