<template>
  <el-dialog :model-value="visible" :title="title" width="600px" @close="handleClose">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
      <el-form-item label="模型名称" prop="modelName"><el-input v-model="form.modelName" /></el-form-item>
      <el-form-item label="模型代码" prop="modelCode"><el-input v-model="form.modelCode" /></el-form-item>
      <el-form-item label="供应商" prop="provider">
        <el-select v-model="form.provider" placeholder="选择供应商">
          <el-option v-for="p in providers" :key="p" :label="p" :value="p" />
        </el-select>
      </el-form-item>
      <el-form-item label="API 地址" prop="apiUrl"><el-input v-model="form.apiUrl" /></el-form-item>
      <el-form-item label="API 密钥" prop="apiKey">
        <el-input v-model="form.apiKey" show-password placeholder="输入明文，保存时自动加密" />
      </el-form-item>
      <el-form-item label="模型类型" prop="modelType">
        <el-select v-model="form.modelType">
          <el-option label="对话" value="chat" />
          <el-option label="向量" value="embedding" />
          <el-option label="图片" value="image" />
        </el-select>
      </el-form-item>
      <el-form-item label="风险等级" prop="riskLevel">
        <el-select v-model="form.riskLevel">
          <el-option label="低" value="low" />
          <el-option label="中" value="medium" />
          <el-option label="高" value="high" />
        </el-select>
      </el-form-item>
      <el-form-item label="每日限额" prop="callLimit">
        <el-input-number v-model="form.callLimit" :min="0" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="form.status">
          <el-option label="启用" value="enabled" />
          <el-option label="停用" value="disabled" />
        </el-select>
      </el-form-item>
      <el-form-item label="描述">
        <el-input type="textarea" v-model="form.description" rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, watch, ref } from 'vue';

const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '新增模型' },
  loading: { type: Boolean, default: false },
  model: { type: Object, default: () => ({}) }
});
const emits = defineEmits(['submit', 'close']);

const formRef = ref();
const form = reactive({
  id: null,
  modelName: '',
  modelCode: '',
  provider: '',
  apiUrl: '',
  apiKey: '',
  modelType: 'chat',
  riskLevel: 'low',
  callLimit: 0,
  status: 'enabled',
  description: ''
});
const providers = ['deepseek', 'qwen', 'wenxin', 'hunyuan', 'xinghuo', 'doubao', 'zhipu', 'modelwhale', 'kimi', 'gaoding'];

const rules = {
  modelName: [{ required: true, message: '必填', trigger: 'blur' }],
  modelCode: [{ required: true, message: '必填', trigger: 'blur' }],
  provider: [{ required: true, message: '必选', trigger: 'change' }],
  apiUrl: [{ required: true, message: '必填', trigger: 'blur' }],
  modelType: [{ required: true, message: '必选', trigger: 'change' }],
  status: [{ required: true, message: '必选', trigger: 'change' }]
};

watch(() => props.model, (val) => {
  Object.assign(form, {
    id: val.id || null,
    modelName: val.modelName || '',
    modelCode: val.modelCode || '',
    provider: val.provider || '',
    apiUrl: val.apiUrl || '',
    apiKey: '',
    modelType: val.modelType || 'chat',
    riskLevel: val.riskLevel || 'low',
    callLimit: val.callLimit ?? 0,
    status: val.status || 'enabled',
    description: val.description || ''
  });
}, { immediate: true });

function handleSubmit() {
  formRef.value.validate().then(() => emits('submit', { ...form })).catch(() => {});
}
function handleClose() {
  emits('close');
}
</script>
