<template>
  <el-card>
    <h2>用户管理</h2>
    <el-form :inline="true" @submit.prevent>
      <el-form-item label="用户名">
        <el-input v-model="query.username" placeholder="输入用户名" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="fetchUsers">查询</el-button>
        <el-button @click="showAdd = true">新增用户</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="users" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="realName" label="真实姓名" />
      <el-table-column prop="roleId" label="角色ID" />
      <el-table-column prop="department" label="部门" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button size="mini" @click="editUser(scope.row)">编辑</el-button>
          <el-button size="mini" type="danger" @click="deleteUser(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="showAdd" title="新增用户">
      <el-form :model="addForm">
        <el-form-item label="用户名"><el-input v-model="addForm.username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="addForm.password" type="password" /></el-form-item>
        <el-form-item label="真实姓名"><el-input v-model="addForm.realName" /></el-form-item>
        <el-form-item label="角色ID"><el-input v-model="addForm.roleId" /></el-form-item>
        <el-form-item label="部门"><el-input v-model="addForm.department" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="addUser">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="showEdit" title="编辑用户">
      <el-form :model="editForm">
        <el-form-item label="真实姓名"><el-input v-model="editForm.realName" /></el-form-item>
        <el-form-item label="角色ID"><el-input v-model="editForm.roleId" /></el-form-item>
        <el-form-item label="部门"><el-input v-model="editForm.department" /></el-form-item>
        <el-form-item label="状态"><el-input v-model="editForm.status" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" @click="updateUser">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { ref } from 'vue';
import request from '../api/request';
const users = ref([]);
const loading = ref(false);
const showAdd = ref(false);
const showEdit = ref(false);
const addForm = ref({ username: '', password: '', realName: '', roleId: '', department: '' });
const editForm = ref({});
const query = ref({ username: '' });
async function fetchUsers() {
  loading.value = true;
  const res = await request.get('/user/list', { params: query.value });
  users.value = res || [];
  loading.value = false;
}
async function addUser() {
  await request.post('/user/register', addForm.value);
  showAdd.value = false;
  fetchUsers();
}
function editUser(row) {
  editForm.value = { ...row };
  showEdit.value = true;
}
async function updateUser() {
  await request.post('/user/update', editForm.value);
  showEdit.value = false;
  fetchUsers();
}
async function deleteUser(id) {
  await request.post('/user/delete', { id });
  fetchUsers();
}
fetchUsers();
</script>
