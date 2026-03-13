<template>
  <div class="page-grid">
    <el-card class="card-glass">
      <div class="card-header">脱敏规则与效果预览</div>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="样例数据"><el-input v-model="sample" style="width:260px" /></el-form-item>
        <el-form-item label="掩码符"><el-input v-model="mask" style="width:100px" /></el-form-item>
        <el-button type="primary" @click="preview">预览</el-button>
        <el-button @click="loadRules">加载规则</el-button>
      </el-form>
      <el-alert v-if="result" type="success" show-icon :closable="false" style="margin-top:10px;">
        <template #title>脱敏结果</template>
        <div>原文：{{ result.raw }}</div>
        <div>脱敏：{{ result.masked }}</div>
      </el-alert>
      <el-table :data="rules" style="margin-top:16px" v-loading="loading">
        <el-table-column prop="name" label="规则名" />
        <el-table-column prop="pattern" label="匹配" />
        <el-table-column prop="mask" label="掩码" />
        <el-table-column prop="example" label="示例" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import request from '../api/request';

const sample = ref('13800138000');
const mask = ref('*');
const result = ref(null);
const rules = ref([]);
const loading = ref(false);

async function preview() {
  result.value = await request.post('/desense/preview', { sample: sample.value, mask: mask.value });
}

async function loadRules() {
  loading.value = true;
  rules.value = await request.get('/desense/rules');
  loading.value = false;
}

loadRules();
</script>

<style scoped>
.page-grid { display: grid; gap: 16px; }
.card-header { font-weight: 600; margin-bottom: 12px; }
</style>
