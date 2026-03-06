<template>
  <el-card>
    <h2>用户管理</h2>
    <el-form :inline="true" @submit.prevent>
      <el-form-item label="用户名">
        <el-input v-model="query.username" placeholder="输入用户名" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="fetchUsers">查询</el-button>
        <el-button @click="openAdd">新增用户</el-button>
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
          <el-button size="small" @click="editUser(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteUser(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="showAdd" title="新增用户">
      <el-form :model="addForm" :rules="addRules" ref="addFormRef">
        <el-form-item label="用户名" prop="username"><el-input v-model="addForm.username" /></el-form-item>
        <el-form-item label="密码" prop="password"><el-input v-model="addForm.password" type="password" /></el-form-item>
        <el-form-item label="真实姓名" prop="realName"><el-input v-model="addForm.realName" /></el-form-item>
        <el-form-item label="角色ID" prop="roleId"><el-input v-model="addForm.roleId" /></el-form-item>
        <el-form-item label="部门"><el-input v-model="addForm.department" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="addUser">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="showEdit" title="编辑用户">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef">
        <el-form-item label="真实姓名" prop="realName"><el-input v-model="editForm.realName" /></el-form-item>
        <el-form-item label="角色ID" prop="roleId"><el-input v-model="editForm.roleId" /></el-form-item>
        <el-form-item label="部门"><el-input v-model="editForm.department" /></el-form-item>
        <el-form-item label="状态" prop="status"><el-input v-model="editForm.status" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="updateUser">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../api/request';
const users = ref([]);
const loading = ref(false);
const showAdd = ref(false);
const showEdit = ref(false);
const saving = ref(false);
const addForm = ref({ username: '', password: '', realName: '', roleId: '', department: '' });
const editForm = ref({});
const query = ref({ username: '' });
const addFormRef = ref();
const editFormRef = ref();
const baseRules = {
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  realName: [{ required: true, message: '真实姓名不能为空', trigger: 'blur' }],
  roleId: [{ required: true, message: '角色ID不能为空', trigger: 'blur' }]
};
const addRules = { ...baseRules, password: [{ required: true, message: '密码不能为空', trigger: 'blur' }] };
const editRules = { ...baseRules, status: [{ required: true, message: '状态不能为空', trigger: 'blur' }] };
async function fetchUsers() {
  loading.value = true;
  try {
    const res = await request.get('/user/list', { params: query.value });
    users.value = res || [];
  } catch (err) {
    ElMessage.error(err?.message || '加载失败');
  } finally {
    loading.value = false;
  }
}
function openAdd() {
  addForm.value = { username: '', password: '', realName: '', roleId: '', department: '' };
  showAdd.value = true;
}
async function addUser() {
  if (!addFormRef.value) return;
  addFormRef.value.validate(async valid => {
    if (!valid) return;
    saving.value = true;
    try {
      await request.post('/user/register', addForm.value);
      ElMessage.success('保存成功');
      showAdd.value = false;
      fetchUsers();
    } catch (err) {
      ElMessage.error(err?.message || '保存失败');
    } finally {
      saving.value = false;
    }
  });
}
function editUser(row) {
  editForm.value = { ...row };
  showEdit.value = true;
}
async function updateUser() {
  if (!editFormRef.value) return;
  editFormRef.value.validate(async valid => {
    if (!valid) return;
    saving.value = true;
    try {
      await request.post('/user/update', editForm.value);
      ElMessage.success('更新成功');
      showEdit.value = false;
      fetchUsers();
    } catch (err) {
      ElMessage.error(err?.message || '更新失败');
    } finally {
      saving.value = false;
    }
  });
}
async function deleteUser(id) {
  try {
    await ElMessageBox.confirm('确认删除该用户吗？', '提示', { type: 'warning' });
    await request.post('/user/delete', { id });
    ElMessage.success('删除成功');
    fetchUsers();
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(err?.message || '删除失败');
  }
}
fetchUsers();
</script>
