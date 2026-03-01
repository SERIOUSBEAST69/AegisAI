<template>
  <el-card>
    <h2>风险事件管理</h2>
    <el-form :inline="true" @submit.prevent>
      <el-form-item label="事件类型">
        <el-input v-model="query.type" placeholder="输入事件类型" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="fetchEvents">查询</el-button>
        <el-button @click="showAdd = true">新增事件</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="events" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="type" label="类型" />
      <el-table-column prop="level" label="风险等级" />
      <el-table-column prop="status" label="状态" />
      <el-table-column prop="handlerId" label="处置人ID" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button size="mini" @click="editEvent(scope.row)">编辑</el-button>
          <el-button size="mini" type="danger" @click="deleteEvent(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="showAdd" title="新增风险事件">
      <el-form :model="addForm">
        <el-form-item label="类型"><el-input v-model="addForm.type" /></el-form-item>
        <el-form-item label="风险等级"><el-input v-model="addForm.level" /></el-form-item>
        <el-form-item label="状态"><el-input v-model="addForm.status" /></el-form-item>
        <el-form-item label="处置人ID"><el-input v-model="addForm.handlerId" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="addEvent">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="showEdit" title="编辑风险事件">
      <el-form :model="editForm">
        <el-form-item label="类型"><el-input v-model="editForm.type" /></el-form-item>
        <el-form-item label="风险等级"><el-input v-model="editForm.level" /></el-form-item>
        <el-form-item label="状态"><el-input v-model="editForm.status" /></el-form-item>
        <el-form-item label="处置人ID"><el-input v-model="editForm.handlerId" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" @click="updateEvent">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { ref } from 'vue';
import request from '../api/request';
const events = ref([]);
const loading = ref(false);
const showAdd = ref(false);
const showEdit = ref(false);
const addForm = ref({ type: '', level: '', status: '', handlerId: '' });
const editForm = ref({});
const query = ref({ type: '' });
async function fetchEvents() {
  loading.value = true;
  const res = await request.get('/risk-event/list', { params: query.value });
  events.value = res || [];
  loading.value = false;
}
async function addEvent() {
  await request.post('/risk-event/add', addForm.value);
  showAdd.value = false;
  fetchEvents();
}
function editEvent(row) {
  editForm.value = { ...row };
  showEdit.value = true;
}
async function updateEvent() {
  await request.post('/risk-event/update', editForm.value);
  showEdit.value = false;
  fetchEvents();
}
async function deleteEvent(id) {
  await request.post('/risk-event/delete', { id });
  fetchEvents();
}
fetchEvents();
</script>
