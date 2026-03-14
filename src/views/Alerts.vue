<template>
  <div class="page-grid">
    <el-card class="card-glass">
      <div class="card-header">告警闭环管理</div>
      <div class="toolbar-row">
        <el-button type="primary" :loading="loading" @click="refresh">刷新</el-button>
        <el-button @click="openCreate">新建告警</el-button>
      </div>
      <el-table :data="list" style="margin-top:12px" v-loading="loading">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="level" label="等级" />
        <el-table-column prop="status" label="状态" />
        <el-table-column prop="assigneeId" label="处理人" />
        <el-table-column prop="relatedLogId" label="关联日志" />
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-select v-model="scope.row.status" size="small" style="width:140px" @change="update(scope.row)">
              <el-option label="open" value="open" />
              <el-option label="claimed" value="claimed" />
              <el-option label="resolved" value="resolved" />
              <el-option label="archived" value="archived" />
            </el-select>
            <el-button size="small" type="danger" @click="remove(scope.row.id)" style="margin-left:8px">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="showCreate" title="新建告警" width="520px">
        <el-form :model="createForm" label-position="top">
          <el-form-item label="标题"><el-input v-model="createForm.title" /></el-form-item>
          <el-form-item label="等级">
            <el-select v-model="createForm.level" style="width: 100%">
              <el-option label="low" value="low" />
              <el-option label="medium" value="medium" />
              <el-option label="high" value="high" />
              <el-option label="critical" value="critical" />
            </el-select>
          </el-form-item>
          <el-form-item label="关联日志ID"><el-input v-model="createForm.relatedLogId" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showCreate = false">取消</el-button>
          <el-button type="primary" :loading="creating" @click="createAlert">创建</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../api/request';

const list = ref([]);
const loading = ref(false);
const showCreate = ref(false);
const creating = ref(false);
const createForm = ref({ title: '', level: 'medium', relatedLogId: '' });

async function refresh() {
  loading.value = true;
  try {
    list.value = await request.get('/alert/list');
  } catch (err) {
    ElMessage.error(err?.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

async function update(row) {
  try {
    await request.post('/alert/update', row);
    ElMessage.success('更新成功');
    refresh();
  } catch (err) {
    ElMessage.error(err?.message || '更新失败');
  }
}

async function remove(id) {
  try {
    await ElMessageBox.confirm('确认删除该告警吗？', '提示', { type: 'warning' });
    await request.post('/alert/delete', { id });
    ElMessage.success('删除成功');
    refresh();
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(err?.message || '删除失败');
  }
}

function openCreate() {
  createForm.value = { title: '', level: 'medium', relatedLogId: '' };
  showCreate.value = true;
}

async function createAlert() {
  if (!createForm.value.title.trim()) {
    ElMessage.warning('请填写告警标题');
    return;
  }
  creating.value = true;
  try {
    await request.post('/alert/create', {
      title: createForm.value.title,
      level: createForm.value.level,
      relatedLogId: createForm.value.relatedLogId ? Number(createForm.value.relatedLogId) : null,
    });
    ElMessage.success('告警已创建');
    showCreate.value = false;
    await refresh();
  } catch (err) {
    ElMessage.error(err?.message || '告警创建失败');
  } finally {
    creating.value = false;
  }
}

refresh();
</script>

<style scoped>
.page-grid { display: grid; gap: 16px; }
.card-header { font-weight: 600; margin-bottom: 12px; }
.toolbar-row { display: flex; gap: 10px; }
</style>
