<template>
  <div class="page-grid">
    <el-card class="card-glass">
      <div class="card-header">敏感数据自动扫描</div>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="来源类型">
          <el-select v-model="form.sourceType" style="width:140px">
            <el-option label="文件" value="file" />
            <el-option label="数据库" value="db" />
          </el-select>
        </el-form-item>
        <el-form-item label="路径/表">
          <el-input v-model="form.sourcePath" placeholder="/data/users.xlsx 或 db.table" />
        </el-form-item>
        <el-button type="primary" @click="create">创建任务</el-button>
      </el-form>
      <el-table :data="list" style="margin-top:16px" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="sourceType" label="类型" />
        <el-table-column prop="sourcePath" label="来源" />
        <el-table-column prop="status" label="状态" />
        <el-table-column prop="sensitiveRatio" label="敏感占比(%)" />
        <el-table-column prop="reportPath" label="报表" />
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button size="small" type="primary" @click="run(scope.row.id)">执行</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import request from '../api/request';

const form = ref({ sourceType: 'file', sourcePath: '' });
const list = ref([]);
const loading = ref(false);

async function fetchList() {
  loading.value = true;
  list.value = await request.get('/sensitive-scan/list');
  loading.value = false;
}

async function create() {
  await request.post('/sensitive-scan/create', form.value);
  fetchList();
}

async function run(id) {
  await request.post('/sensitive-scan/run', { id });
  fetchList();
}

fetchList();
</script>

<style scoped>
.page-grid { display: grid; gap: 16px; }
.card-header { font-weight: 600; margin-bottom: 12px; }
</style>
