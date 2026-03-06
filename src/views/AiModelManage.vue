<template>
  <el-card>
    <h2>AI模型管理</h2>
    <el-form :inline="true" @submit.prevent>
      <el-form-item label="关键字">
        <el-input v-model="query.keyword" placeholder="名称/编码" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="fetchModels">查询</el-button>
        <el-button @click="openAdd">新增模型</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="models" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="modelName" label="模型名称" />
      <el-table-column prop="modelCode" label="编码" />
      <el-table-column prop="provider" label="提供方" />
      <el-table-column prop="modelType" label="类型" />
      <el-table-column prop="riskLevel" label="风险等级" />
      <el-table-column prop="status" label="状态" />
      <el-table-column prop="callLimit" label="日调用上限" />
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button size="small" @click="editModel(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteModel(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showAdd" title="新增模型" width="640px">
      <el-form :model="addForm" :rules="addRules" ref="addFormRef" label-width="120px">
        <el-form-item label="模型名称"><el-input v-model="addForm.modelName" /></el-form-item>
        <el-form-item label="模型编码"><el-input v-model="addForm.modelCode" /></el-form-item>
        <el-form-item label="提供方"><el-input v-model="addForm.provider" placeholder="如 qwen / openai" /></el-form-item>
        <el-form-item label="接口地址"><el-input v-model="addForm.apiUrl" /></el-form-item>
        <el-form-item label="API Key"><el-input v-model="addForm.apiKey" type="password" /></el-form-item>
        <el-form-item label="模型类型"><el-input v-model="addForm.modelType" placeholder="chat/embedding/image" /></el-form-item>
        <el-form-item label="风险等级"><el-input v-model="addForm.riskLevel" placeholder="low/medium/high" /></el-form-item>
        <el-form-item label="状态"><el-input v-model="addForm.status" placeholder="enabled/disabled" /></el-form-item>
        <el-form-item label="日调用上限"><el-input-number v-model="addForm.callLimit" :min="0" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="addForm.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="addModel">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showEdit" title="编辑模型" width="640px">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="120px">
        <el-form-item label="模型名称"><el-input v-model="editForm.modelName" /></el-form-item>
        <el-form-item label="模型编码"><el-input v-model="editForm.modelCode" /></el-form-item>
        <el-form-item label="提供方"><el-input v-model="editForm.provider" /></el-form-item>
        <el-form-item label="接口地址"><el-input v-model="editForm.apiUrl" /></el-form-item>
        <el-form-item label="API Key"><el-input v-model="editForm.apiKey" type="password" placeholder="留空则不修改" /></el-form-item>
        <el-form-item label="模型类型"><el-input v-model="editForm.modelType" /></el-form-item>
        <el-form-item label="风险等级"><el-input v-model="editForm.riskLevel" /></el-form-item>
        <el-form-item label="状态"><el-input v-model="editForm.status" /></el-form-item>
        <el-form-item label="日调用上限"><el-input-number v-model="editForm.callLimit" :min="0" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="editForm.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="updateModel">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../api/request';
const models = ref([]);
const loading = ref(false);
const showAdd = ref(false);
const showEdit = ref(false);
const saving = ref(false);
const addForm = ref({
  modelName: '',
  modelCode: '',
  provider: '',
  apiUrl: '',
  apiKey: '',
  modelType: 'chat',
  riskLevel: '',
  status: 'enabled',
  callLimit: 0,
  description: ''
});
const editForm = ref({});
const query = ref({ keyword: '' });
const addFormRef = ref();
const editFormRef = ref();
const baseRules = {
  modelName: [{ required: true, message: '模型名称不能为空', trigger: 'blur' }],
  modelCode: [{ required: true, message: '编码不能为空', trigger: 'blur' }],
  provider: [{ required: true, message: '提供方不能为空', trigger: 'blur' }],
  apiUrl: [{ required: true, message: '接口地址不能为空', trigger: 'blur' }],
  modelType: [{ required: true, message: '类型不能为空', trigger: 'blur' }]
};
const addRules = { ...baseRules, apiKey: [{ required: true, message: 'API Key不能为空', trigger: 'blur' }] };
const editRules = baseRules; // apiKey 可选

async function fetchModels() {
  loading.value = true;
  try {
    const res = await request.get('/ai-model/list', { params: query.value });
    models.value = res || [];
  } catch (err) {
    ElMessage.error(err?.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

function openAdd() {
  addForm.value = {
    modelName: '',
    modelCode: '',
    provider: '',
    apiUrl: '',
    apiKey: '',
    modelType: 'chat',
    riskLevel: '',
    status: 'enabled',
    callLimit: 0,
    description: ''
  };
  showAdd.value = true;
}

async function addModel() {
  if (!addFormRef.value) return;
  addFormRef.value.validate(async valid => {
    if (!valid) return;
    saving.value = true;
    try {
      await request.post('/ai-model/add', addForm.value);
      ElMessage.success('添加成功');
      showAdd.value = false;
      fetchModels();
    } catch (err) {
      ElMessage.error(err?.message || '添加失败');
    } finally {
      saving.value = false;
    }
  });
}

function editModel(row) {
  editForm.value = { ...row, apiKey: '' };
  showEdit.value = true;
}

async function updateModel() {
  if (!editFormRef.value) return;
  editFormRef.value.validate(async valid => {
    if (!valid) return;
    saving.value = true;
    try {
      await request.post('/ai-model/update', editForm.value);
      ElMessage.success('更新成功');
      showEdit.value = false;
      fetchModels();
    } catch (err) {
      ElMessage.error(err?.message || '更新失败');
    } finally {
      saving.value = false;
    }
  });
}

async function deleteModel(id) {
  try {
    await ElMessageBox.confirm('确认删除该模型吗？', '提示', { type: 'warning' });
    await request.post('/ai-model/delete', { id });
    ElMessage.success('删除成功');
    fetchModels();
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(err?.message || '删除失败');
  }
}

fetchModels();
</script>
