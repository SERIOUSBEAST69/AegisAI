<template>
  <el-card>
    <h2>角色管理</h2>
    <el-form :inline="true" @submit.prevent>
      <el-form-item label="角色名称">
        <el-input v-model="query.name" placeholder="输入角色名称" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="fetchRoles">查询</el-button>
        <el-button @click="showAdd = true">新增角色</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="roles" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="角色名称" />
      <el-table-column prop="code" label="角色编码" />
      <el-table-column prop="description" label="描述" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button size="mini" @click="editRole(scope.row)">编辑</el-button>
          <el-button size="mini" type="danger" @click="deleteRole(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="showAdd" title="新增角色">
      <el-form :model="addForm">
        <el-form-item label="角色名称"><el-input v-model="addForm.name" /></el-form-item>
        <el-form-item label="角色编码"><el-input v-model="addForm.code" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="addForm.description" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="addRole">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="showEdit" title="编辑角色">
      <el-form :model="editForm">
        <el-form-item label="角色名称"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="角色编码"><el-input v-model="editForm.code" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="editForm.description" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" @click="updateRole">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { ref } from 'vue';
import request from '../api/request';
const roles = ref([]);
const loading = ref(false);
const showAdd = ref(false);
const showEdit = ref(false);
const addForm = ref({ name: '', code: '', description: '' });
const editForm = ref({});
const query = ref({ name: '' });
async function fetchRoles() {
  loading.value = true;
  const res = await request.get('/role/list', { params: query.value });
  roles.value = res || [];
  loading.value = false;
}
async function addRole() {
  await request.post('/role/add', addForm.value);
  showAdd.value = false;
  fetchRoles();
}
function editRole(row) {
  editForm.value = { ...row };
  showEdit.value = true;
}
async function updateRole() {
  await request.post('/role/update', editForm.value);
  showEdit.value = false;
  fetchRoles();
}
async function deleteRole(id) {
  await request.post('/role/delete', { id });
  fetchRoles();
}
fetchRoles();
</script>
