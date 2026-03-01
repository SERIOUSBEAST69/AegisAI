<template>
  <el-card>
    <h2>权限管理</h2>
    <el-form :inline="true" @submit.prevent>
      <el-form-item label="权限名称">
        <el-input v-model="query.name" placeholder="输入权限名称" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="fetchPermissions">查询</el-button>
        <el-button @click="showAdd = true">新增权限</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="permissions" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="权限名称" />
      <el-table-column prop="code" label="权限编码" />
      <el-table-column prop="type" label="类型" />
      <el-table-column prop="parentId" label="父ID" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button size="mini" @click="editPermission(scope.row)">编辑</el-button>
          <el-button size="mini" type="danger" @click="deletePermission(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="showAdd" title="新增权限">
      <el-form :model="addForm">
        <el-form-item label="权限名称"><el-input v-model="addForm.name" /></el-form-item>
        <el-form-item label="权限编码"><el-input v-model="addForm.code" /></el-form-item>
        <el-form-item label="类型"><el-input v-model="addForm.type" /></el-form-item>
        <el-form-item label="父ID"><el-input v-model="addForm.parentId" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="addPermission">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="showEdit" title="编辑权限">
      <el-form :model="editForm">
        <el-form-item label="权限名称"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="权限编码"><el-input v-model="editForm.code" /></el-form-item>
        <el-form-item label="类型"><el-input v-model="editForm.type" /></el-form-item>
        <el-form-item label="父ID"><el-input v-model="editForm.parentId" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" @click="updatePermission">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { ref } from 'vue';
import request from '../api/request';
const permissions = ref([]);
const loading = ref(false);
const showAdd = ref(false);
const showEdit = ref(false);
const addForm = ref({ name: '', code: '', type: '', parentId: '' });
const editForm = ref({});
const query = ref({ name: '' });
async function fetchPermissions() {
  loading.value = true;
  const res = await request.get('/permission/list', { params: query.value });
  permissions.value = res || [];
  loading.value = false;
}
async function addPermission() {
  await request.post('/permission/add', addForm.value);
  showAdd.value = false;
  fetchPermissions();
}
function editPermission(row) {
  editForm.value = { ...row };
  showEdit.value = true;
}
async function updatePermission() {
  await request.post('/permission/update', editForm.value);
  showEdit.value = false;
  fetchPermissions();
}
async function deletePermission(id) {
  await request.post('/permission/delete', { id });
  fetchPermissions();
}
fetchPermissions();
</script>
