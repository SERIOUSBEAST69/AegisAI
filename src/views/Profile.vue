<template>
  <el-card class="card-glass">
    <div class="card-header">个人资料</div>
    <el-form :model="userInfo" label-width="120px">
      <el-form-item label="用户名">
        <el-input v-model="userInfo.username" disabled />
      </el-form-item>
      <el-form-item label="真实姓名">
        <el-input v-model="userInfo.realName" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="userInfo.email" />
      </el-form-item>
      <el-form-item label="电话">
        <el-input v-model="userInfo.phone" />
      </el-form-item>
      <el-form-item label="部门">
        <el-input v-model="userInfo.department" />
      </el-form-item>
      <el-form-item label="角色">
        <el-input v-model="userInfo.role" disabled />
      </el-form-item>
      <el-form-item label="最后登录时间">
        <el-input v-model="userInfo.lastLogin" disabled />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="saveProfile">保存修改</el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';

const userInfo = ref({
  username: 'admin',
  realName: '管理员',
  email: 'admin@aegisai.com',
  phone: '13800138000',
  department: '技术部',
  role: '超级管理员',
  lastLogin: '2026-03-08 10:00:00'
});

const saveProfile = () => {
  // 模拟保存操作
  ElMessage.success('个人资料保存成功');
};

const resetForm = () => {
  userInfo.value = {
    username: 'admin',
    realName: '管理员',
    email: 'admin@aegisai.com',
    phone: '13800138000',
    department: '技术部',
    role: '超级管理员',
    lastLogin: '2026-03-08 10:00:00'
  };
  ElMessage.info('表单已重置');
};
</script>

<style scoped>
.card-header {
  font-weight: 600;
  margin-bottom: 16px;
  color: var(--color-text);
  font-size: 18px;
}
</style>