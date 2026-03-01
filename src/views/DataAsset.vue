<template>
  <el-card>
    <h2>数据资产管理</h2>
    <el-form :inline="true" @submit.prevent>
      <el-form-item label="资产名称">
        <el-input v-model="query.name" placeholder="输入名称" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="fetchAssets">查询</el-button>
        <el-button @click="showAdd = true">新增资产</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="assets" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="type" label="类型" />
      <el-table-column prop="sensitivityLevel" label="敏感等级" />
      <el-table-column prop="ownerId" label="负责人ID" />
      <el-table-column prop="createTime" label="创建时间" />
    </el-table>
    <el-dialog v-model="showAdd" title="新增数据资产">
      <el-form :model="addForm">
        <el-form-item label="名称"><el-input v-model="addForm.name" /></el-form-item>
        <el-form-item label="类型"><el-input v-model="addForm.type" /></el-form-item>
        <el-form-item label="敏感等级"><el-input v-model="addForm.sensitivityLevel" /></el-form-item>
        <el-form-item label="负责人ID"><el-input v-model="addForm.ownerId" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="addAsset">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { ref } from 'vue';
import request from '../api/request';
const assets = ref([]);
const loading = ref(false);
const showAdd = ref(false);
const addForm = ref({ name: '', type: '', sensitivityLevel: '', ownerId: '' });
const query = ref({ name: '' });
async function fetchAssets() {
  loading.value = true;
  const res = await request.get('/data-asset/list', { params: query.value });
  assets.value = res || [];
  loading.value = false;
}
async function addAsset() {
  await request.post('/data-asset/register', addForm.value);
  showAdd.value = false;
  fetchAssets();
}
fetchAssets();
</script>
