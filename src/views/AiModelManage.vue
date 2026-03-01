<template>
  <el-card>
    <h2>AI模型管理</h2>
    <el-form :inline="true" @submit.prevent>
      <el-form-item label="模型名称">
        <el-input v-model="query.name" placeholder="输入模型名称" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="fetchModels">查询</el-button>
        <el-button @click="showAdd = true">新增模型</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="models" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="模型名称" />
      <el-table-column prop="type" label="类型" />
      <el-table-column prop="riskLevel" label="风险等级" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button size="mini" @click="editModel(scope.row)">编辑</el-button>
          <el-button size="mini" type="danger" @click="deleteModel(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="showAdd" title="新增模型">
      <el-form :model="addForm">
        <el-form-item label="模型名称"><el-input v-model="addForm.name" /></el-form-item>
        <el-form-item label="类型"><el-input v-model="addForm.type" /></el-form-item>
        <el-form-item label="风险等级"><el-input v-model="addForm.riskLevel" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="addModel">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="showEdit" title="编辑模型">
      <el-form :model="editForm">
        <el-form-item label="模型名称"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="类型"><el-input v-model="editForm.type" /></el-form-item>
        <el-form-item label="风险等级"><el-input v-model="editForm.riskLevel" /></el-form-item>
        <el-form-item label="状态"><el-input v-model="editForm.status" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" @click="updateModel">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { ref } from 'vue';
import request from '../api/request';
const models = ref([]);
const loading = ref(false);
const showAdd = ref(false);
const showEdit = ref(false);
const addForm = ref({ name: '', type: '', riskLevel: '' });
const editForm = ref({});
const query = ref({ name: '' });
async function fetchModels() {
  loading.value = true;
  const res = await request.get('/ai-model/list', { params: query.value });
  models.value = res || [];
  loading.value = false;
}
async function addModel() {
  await request.post('/ai-model/add', addForm.value);
  showAdd.value = false;
  fetchModels();
}
function editModel(row) {
  editForm.value = { ...row };
  showEdit.value = true;
}
async function updateModel() {
  await request.post('/ai-model/update', editForm.value);
  showEdit.value = false;
  fetchModels();
}
async function deleteModel(id) {
  await request.post('/ai-model/delete', { id });
  fetchModels();
}
fetchModels();
</script>
