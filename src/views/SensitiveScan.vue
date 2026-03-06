<template>
  <div class="page-grid">
    <el-card class="card-glass">
      <div class="card-header">敏感数据自动扫描</div>
      <el-form :inline="true" @submit.prevent ref="formRef" :model="form" :rules="rules">
        <el-form-item label="来源类型" prop="sourceType">
          <el-select v-model="form.sourceType" style="width:140px">
            <el-option label="文件" value="file" />
            <el-option label="数据库" value="db" />
          </el-select>
        </el-form-item>
        <el-form-item label="路径/表" prop="sourcePath">
          <el-input v-model="form.sourcePath" placeholder="/data/users.xlsx 或 db.table" />
        </el-form-item>
        <el-button type="primary" :loading="saving" @click="create">创建任务</el-button>
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
            <el-button size="small" type="danger" @click="remove(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../api/request';

const form = ref({ sourceType: 'file', sourcePath: '' });
const list = ref([]);
const loading = ref(false);
const saving = ref(false);
const formRef = ref();
const rules = {
  sourceType: [{ required: true, message: '请选择来源类型', trigger: 'change' }],
  sourcePath: [{ required: true, message: '请输入路径/表', trigger: 'blur' }]
};

async function fetchList() {
  loading.value = true;
  try {
    list.value = await request.get('/sensitive-scan/list');
  } catch (err) {
    ElMessage.error(err?.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

async function create() {
  if (!formRef.value) return;
  formRef.value.validate(async valid => {
    if (!valid) return;
    saving.value = true;
    try {
      await request.post('/sensitive-scan/create', form.value);
      ElMessage.success('创建成功');
      fetchList();
    } catch (err) {
      ElMessage.error(err?.message || '创建失败');
    } finally {
      saving.value = false;
    }
  });
}

async function run(id) {
  try {
    await request.post('/sensitive-scan/run', { id });
    ElMessage.success('任务已执行');
    fetchList();
  } catch (err) {
    ElMessage.error(err?.message || '执行失败');
  }
}

async function remove(id) {
  try {
    await ElMessageBox.confirm('确认删除该任务吗？', '提示', { type: 'warning' });
    await request.post('/sensitive-scan/delete', { id });
    ElMessage.success('删除成功');
    fetchList();
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(err?.message || '删除失败');
  }
}

fetchList();
</script>

<style scoped>
.page-grid { display: grid; gap: 16px; }
.card-header { font-weight: 600; margin-bottom: 12px; }
</style>
