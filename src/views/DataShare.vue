<template>
  <div class="page-grid">
    <el-card class="card-glass">
      <div class="card-header">数据资产共享审批</div>
      <el-form :inline="true" @submit.prevent ref="applyFormRef" :model="form" :rules="rules">
        <el-form-item label="资产ID" prop="assetId"><el-input v-model="form.assetId" /></el-form-item>
        <el-form-item label="协作人ID，逗号分隔"><el-input v-model="form.collaborators" /></el-form-item>
        <el-form-item label="理由" prop="reason"><el-input v-model="form.reason" style="width:240px" /></el-form-item>
        <el-button type="primary" :loading="saving" @click="apply">提交申请</el-button>
      </el-form>
      <el-table :data="list" style="margin-top:12px" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="assetId" label="资产ID" />
        <el-table-column prop="applicantId" label="申请人" />
        <el-table-column prop="collaborators" label="协作人" />
        <el-table-column prop="reason" label="理由" />
        <el-table-column label="状态" width="90">
          <template #default="scope">{{ formatShareStatus(scope.row.status) }}</template>
        </el-table-column>
        <!-- 通过后显示令牌 -->
        <el-table-column label="访问令牌" min-width="220">
          <template #default="scope">
            <template v-if="scope.row.status === 'approved' && scope.row.shareToken">
              <div class="token-cell">
                <el-tooltip :content="scope.row.shareToken" placement="top">
                  <code class="token-code">{{ truncateToken(scope.row.shareToken) }}</code>
                </el-tooltip>
                <el-button
                  size="small"
                  type="primary"
                  link
                  @click="copyToken(scope.row.shareToken)"
                >复制</el-button>
              </div>
              <div v-if="scope.row.expireTime" class="token-expire">
                有效至：{{ scope.row.expireTime }}
              </div>
            </template>
            <span v-else class="token-none">—</span>
          </template>
        </el-table-column>
        <el-table-column label="审批" width="200">
          <template #default="scope">
            <el-button size="small" type="success" :loading="actionId===scope.row.id && actionType==='approve'" @click="approve(scope.row.id, 'approved')">通过</el-button>
            <el-button size="small" type="danger" :loading="actionId===scope.row.id && actionType==='reject'" @click="approve(scope.row.id, 'rejected')">拒绝</el-button>
            <el-button size="small" type="warning" :loading="actionId===scope.row.id && actionType==='delete'" @click="remove(scope.row.id)" style="margin-left:6px">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-divider />

      <div class="access-panel">
        <div class="access-title">共享令牌访问验证</div>
        <p class="access-subtitle">输入已签发的共享令牌，验证后端 data-share access 能力是否可用。</p>
        <div class="access-row">
          <el-input v-model="accessToken" placeholder="粘贴 shareToken" clearable />
          <el-button type="primary" :loading="accessLoading" @click="accessByToken">验证访问</el-button>
        </div>
        <el-alert
          v-if="accessResult"
          type="success"
          :closable="false"
          show-icon
          style="margin-top: 10px"
        >
          <template #title>资产 {{ accessResult.assetId }} · {{ accessResult.name }}</template>
          <div>类型：{{ accessResult.type }} | 敏感等级：{{ accessResult.sensitivityLevel }}</div>
          <div>脱敏位置：{{ accessResult.location }}</div>
        </el-alert>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../api/request';

const form = ref({ assetId: '', collaborators: '', reason: '' });
const list = ref([]);
const loading = ref(false);
const saving = ref(false);
const actionId = ref(null);
const actionType = ref('');
const accessToken = ref('');
const accessLoading = ref(false);
const accessResult = ref(null);
const applyFormRef = ref();
const rules = {
  assetId: [{ required: true, message: '资产ID不能为空', trigger: 'blur' }],
  reason: [{ required: true, message: '理由不能为空', trigger: 'blur' }]
};

async function load() {
  loading.value = true;
  try {
    list.value = await request.get('/data-share/list');
  } catch (err) {
    ElMessage.error(err?.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

async function apply() {
  if (!applyFormRef.value) return;
  applyFormRef.value.validate(async valid => {
    if (!valid) return;
    saving.value = true;
    try {
      await request.post('/data-share/apply', form.value);
      ElMessage.success('提交成功');
      load();
    } catch (err) {
      ElMessage.error(err?.message || '提交失败');
    } finally {
      saving.value = false;
    }
  });
}

async function approve(id, status) {
  actionId.value = id; actionType.value = status === 'approved' ? 'approve' : 'reject';
  try {
    await request.post('/data-share/approve', { id, status });
    ElMessage.success(status === 'approved' ? '已通过，系统已生成访问令牌' : '已拒绝');
    load();
  } catch (err) {
    ElMessage.error(err?.message || '处理失败');
  } finally {
    actionId.value = null; actionType.value = '';
  }
}

async function remove(id) {
  try {
    await ElMessageBox.confirm('确认删除该申请吗？', '提示', { type: 'warning' });
    actionId.value = id; actionType.value = 'delete';
    await request.post('/data-share/delete', { id });
    ElMessage.success('删除成功');
    load();
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(err?.message || '删除失败');
  } finally {
    actionId.value = null; actionType.value = '';
  }
}

function formatShareStatus(status) {
  if (status === 'approved') return '已通过';
  if (status === 'rejected') return '已拒绝';
  return '待审批';
}

function truncateToken(token) {
  if (!token) return '';
  if (token.length <= 16) return token;
  return token.substring(0, 8) + '...' + token.substring(token.length - 4);
}

async function copyToken(token) {
  try {
    await navigator.clipboard.writeText(token);
    ElMessage.success('令牌已复制到剪贴板');
  } catch {
    ElMessage.info('令牌：' + token);
  }
}

async function accessByToken() {
  if (!accessToken.value.trim()) {
    ElMessage.warning('请输入共享令牌');
    return;
  }
  accessLoading.value = true;
  accessResult.value = null;
  try {
    accessResult.value = await request.get('/data-share/access', {
      params: { token: accessToken.value.trim() }
    });
    ElMessage.success('访问验证成功');
  } catch (err) {
    ElMessage.error(err?.message || '访问验证失败');
  } finally {
    accessLoading.value = false;
  }
}

load();
</script>

<style scoped>
.page-grid { display: grid; gap: 16px; }
.card-header { font-weight: 600; margin-bottom: 12px; }

.token-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.token-code {
  font-family: monospace;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.06);
  color: var(--color-primary-light);
  cursor: default;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.token-expire {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-top: 3px;
}

.token-none {
  color: var(--color-text-muted);
  font-size: 13px;
}

.access-panel {
  margin-top: 8px;
}

.access-title {
  font-weight: 700;
  color: var(--color-text);
}

.access-subtitle {
  margin: 6px 0 10px;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.access-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}
</style>

