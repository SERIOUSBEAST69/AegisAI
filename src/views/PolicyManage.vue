<template>
  <el-card>
    <h2>合规策略管理</h2>
    <el-form :inline="true" @submit.prevent>
      <el-form-item label="策略名称">
        <el-input v-model="query.name" placeholder="输入策略名称" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="fetchPolicies">查询</el-button>
        <el-button @click="showAdd = true">新增策略</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="policies" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="策略名称" />
      <el-table-column prop="ruleContent" label="规则内容" />
      <el-table-column prop="scope" label="生效范围" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button size="mini" @click="editPolicy(scope.row)">编辑</el-button>
          <el-button size="mini" type="danger" @click="deletePolicy(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="showAdd" title="新增策略">
      <el-form :model="addForm">
        <el-form-item label="策略名称"><el-input v-model="addForm.name" /></el-form-item>
        <el-form-item label="规则内容"><el-input v-model="addForm.ruleContent" /></el-form-item>
        <el-form-item label="生效范围"><el-input v-model="addForm.scope" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="addPolicy">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="showEdit" title="编辑策略">
      <el-form :model="editForm">
        <el-form-item label="策略名称"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="规则内容"><el-input v-model="editForm.ruleContent" /></el-form-item>
        <el-form-item label="生效范围"><el-input v-model="editForm.scope" /></el-form-item>
        <el-form-item label="状态"><el-input v-model="editForm.status" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" @click="updatePolicy">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { ref } from 'vue';
import request from '../api/request';
const policies = ref([]);
const loading = ref(false);
const showAdd = ref(false);
const showEdit = ref(false);
const addForm = ref({ name: '', ruleContent: '', scope: '' });
const editForm = ref({});
const query = ref({ name: '' });
async function fetchPolicies() {
  loading.value = true;
  const res = await request.get('/policy/list', { params: query.value });
  policies.value = res || [];
  loading.value = false;
}
async function addPolicy() {
  await request.post('/policy/save', addForm.value);
  showAdd.value = false;
  fetchPolicies();
}
function editPolicy(row) {
  editForm.value = { ...row };
  showEdit.value = true;
}
async function updatePolicy() {
  await request.post('/policy/save', editForm.value);
  showEdit.value = false;
  fetchPolicies();
}
async function deletePolicy(id) {
  await request.post('/policy/delete', { id });
  fetchPolicies();
}
fetchPolicies();
</script>
