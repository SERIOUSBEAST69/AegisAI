<template>
  <el-card>
    <h2>数据资产管理</h2>
    <el-form :inline="true" @submit.prevent>
      <el-form-item label="资产名称">
        <el-input v-model="query.name" placeholder="输入名称" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="fetchAssets">查询</el-button>
        <el-button @click="openAdd">新增资产</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="assets" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="type" label="类型" />
      <el-table-column prop="sensitivityLevel" label="敏感等级" />
      <el-table-column prop="ownerId" label="负责人ID" />
      <el-table-column prop="createTime" label="创建时间" />
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button size="small" @click="editAsset(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteAsset(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="showAdd" title="新增数据资产">
      <el-form :model="addForm" :rules="rules" ref="addFormRef">
        <el-form-item label="名称" prop="name"><el-input v-model="addForm.name" /></el-form-item>
        <el-form-item label="类型" prop="type"><el-input v-model="addForm.type" /></el-form-item>
        <el-form-item label="敏感等级" prop="sensitivityLevel"><el-input v-model="addForm.sensitivityLevel" /></el-form-item>
        <el-form-item label="负责人ID" prop="ownerId"><el-input v-model="addForm.ownerId" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="addAsset">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showEdit" title="编辑数据资产">
      <el-form :model="editForm" :rules="rules" ref="editFormRef">
        <el-form-item label="名称" prop="name"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="类型" prop="type"><el-input v-model="editForm.type" /></el-form-item>
        <el-form-item label="敏感等级" prop="sensitivityLevel"><el-input v-model="editForm.sensitivityLevel" /></el-form-item>
        <el-form-item label="负责人ID" prop="ownerId"><el-input v-model="editForm.ownerId" /></el-form-item>
        <el-form-item label="位置"><el-input v-model="editForm.location" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="updateAsset">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../api/request';
const assets = ref([]);
const loading = ref(false);
const showAdd = ref(false);
const showEdit = ref(false);
const saving = ref(false);
const addForm = ref({ name: '', type: '', sensitivityLevel: '', ownerId: '' });
const editForm = ref({});
const query = ref({ name: '' });
const addFormRef = ref();
const editFormRef = ref();
const rules = {
  name: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
  type: [{ required: true, message: '类型不能为空', trigger: 'blur' }],
  sensitivityLevel: [{ required: true, message: '敏感等级不能为空', trigger: 'blur' }],
  ownerId: [{ required: true, message: '负责人不能为空', trigger: 'blur' }]
};
async function fetchAssets() {
  loading.value = true;
  try {
    const res = await request.get('/data-asset/list', { params: query.value });
    assets.value = res || [];
  } catch (err) {
    ElMessage.error(err?.message || '加载失败');
  } finally {
    loading.value = false;
  }
}
function openAdd() {
  addForm.value = { name: '', type: '', sensitivityLevel: '', ownerId: '' };
  showAdd.value = true;
}
async function addAsset() {
  if (!addFormRef.value) return;
  addFormRef.value.validate(async valid => {
    if (!valid) return;
    saving.value = true;
    try {
      await request.post('/data-asset/register', addForm.value);
      ElMessage.success('保存成功');
      showAdd.value = false;
      fetchAssets();
    } catch (err) {
      ElMessage.error(err?.message || '保存失败');
    } finally {
      saving.value = false;
    }
  });
}
function editAsset(row) {
  editForm.value = { ...row };
  showEdit.value = true;
}
async function updateAsset() {
  if (!editFormRef.value) return;
  editFormRef.value.validate(async valid => {
    if (!valid) return;
    saving.value = true;
    try {
      await request.post('/data-asset/update', editForm.value);
      ElMessage.success('更新成功');
      showEdit.value = false;
      fetchAssets();
    } catch (err) {
      ElMessage.error(err?.message || '更新失败');
    } finally {
      saving.value = false;
    }
  });
}
async function deleteAsset(id) {
  try {
    await ElMessageBox.confirm('确认删除该资产吗？', '提示', { type: 'warning' });
    await request.post('/data-asset/delete', { id });
    ElMessage.success('删除成功');
    fetchAssets();
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(err?.message || '删除失败');
  }
}
fetchAssets();
</script>
