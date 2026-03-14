<template>
  <div class="shadow-ai-page">

    <!-- 页头 -->
    <div class="page-header scene-block">
      <div class="page-header-copy">
        <div class="page-eyebrow">SHADOW AI DISCOVERY</div>
        <h1 class="page-title">影子AI发现与治理</h1>
        <p class="page-subtitle">
          实时监控终端设备上员工私自使用的AI服务，自动识别 ChatGPT、Midjourney、Claude
          等未受管控的"影子AI"，防止核心数据与隐私泄露。
        </p>
      </div>
      <div class="page-header-actions">
        <el-tag v-if="isMock" type="warning" size="large">演示数据</el-tag>
        <el-button type="primary" :loading="loading" @click="refresh">
          <el-icon><Refresh /></el-icon>
          刷新扫描结果
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid scene-block">
      <article class="stat-tile card-glass">
        <div class="stat-tile-icon clients">
          <el-icon><Monitor /></el-icon>
        </div>
        <div class="stat-tile-body">
          <span class="stat-tile-label">在线客户端</span>
          <strong class="stat-tile-value">{{ stats.totalClients }}</strong>
          <span class="stat-tile-hint">已部署客户端总数</span>
        </div>
      </article>

      <article class="stat-tile card-glass">
        <div class="stat-tile-icon shadow">
          <el-icon><Warning /></el-icon>
        </div>
        <div class="stat-tile-body">
          <span class="stat-tile-label">发现影子AI</span>
          <strong class="stat-tile-value">{{ stats.totalShadowAi }}</strong>
          <span class="stat-tile-hint">未经授权的AI服务</span>
        </div>
      </article>

      <article class="stat-tile card-glass">
        <div class="stat-tile-icon danger">
          <el-icon><AlarmClock /></el-icon>
        </div>
        <div class="stat-tile-body">
          <span class="stat-tile-label">高风险终端</span>
          <strong class="stat-tile-value danger-text">{{ stats.highRiskClients }}</strong>
          <span class="stat-tile-hint">需优先处置的设备</span>
        </div>
      </article>

      <article class="stat-tile card-glass">
        <div class="stat-tile-icon reports">
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-tile-body">
          <span class="stat-tile-label">近7日报告</span>
          <strong class="stat-tile-value">{{ stats.recentReports }}</strong>
          <span class="stat-tile-hint">客户端上报次数</span>
        </div>
      </article>
    </div>

    <!-- 风险分布 + 客户端列表 -->
    <div class="main-grid scene-block">

      <!-- 左侧：风险等级分布 -->
      <el-card class="risk-dist-card card-glass">
        <div class="panel-head">
          <div class="card-header">风险分布</div>
          <p class="panel-subtitle">各终端影子AI风险等级占比。</p>
        </div>
        <div class="risk-dist-list">
          <div
            v-for="item in riskDistItems"
            :key="item.level"
            class="risk-dist-item"
          >
            <div class="risk-dist-row">
              <span :class="['risk-badge', item.level]">{{ item.label }}</span>
              <strong>{{ item.count }}</strong>
            </div>
            <div class="risk-dist-bar">
              <i
                :class="['risk-dist-fill', item.level]"
                :style="{ width: `${item.pct}%` }"
              ></i>
            </div>
          </div>
        </div>

        <div class="download-section">
          <div class="card-header" style="margin-top: 28px;">客户端下载</div>
          <p class="panel-subtitle">
            部署 Aegis 轻量客户端到员工电脑，实现持续监控。
          </p>
          <div class="download-btns">
            <el-button type="primary" plain size="small">
              <el-icon><Download /></el-icon>
              Windows 安装包
            </el-button>
            <el-button type="primary" plain size="small">
              <el-icon><Download /></el-icon>
              macOS DMG
            </el-button>
            <el-button plain size="small">
              <el-icon><Download /></el-icon>
              Linux DEB/RPM
            </el-button>
          </div>
          <div class="deploy-tip">
            下载后双击安装，客户端将自动连接到本平台并开始扫描。
            部署文档见
            <a
              href="https://github.com/SERIOUSBEAST69/AegisAI/blob/main/electron/README.md"
              target="_blank"
            >electron/README.md</a>。
          </div>
        </div>
      </el-card>

      <!-- 右侧：客户端设备列表 -->
      <el-card class="clients-card card-glass">
        <div class="panel-head">
          <div>
            <div class="card-header">终端设备清单</div>
            <p class="panel-subtitle">每台设备的最新扫描结果与发现的影子AI服务。</p>
          </div>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索主机名或用户"
            size="small"
            style="width: 200px;"
            clearable
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>

        <div v-if="loading" class="loading-state">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>正在加载扫描数据…</span>
        </div>

        <div v-else-if="filteredClients.length === 0" class="empty-state">
          <el-icon><Monitor /></el-icon>
          <span>暂无客户端数据。请先部署客户端。</span>
        </div>

        <div v-else class="client-list">
          <article
            v-for="client in filteredClients"
            :key="client.clientId"
            :class="['client-card', 'card-glass', client.riskLevel]"
            @click="selectClient(client)"
          >
            <div class="client-card-head">
              <div class="client-info">
                <div class="client-hostname">
                  <el-icon><Monitor /></el-icon>
                  {{ client.hostname }}
                </div>
                <div class="client-meta">
                  {{ client.osUsername }} · {{ client.osType }} · v{{ client.clientVersion }}
                </div>
              </div>
              <div class="client-risk">
                <span :class="['risk-badge', client.riskLevel]">
                  {{ riskLabel(client.riskLevel) }}
                </span>
                <span class="client-ai-count">
                  {{ client.shadowAiCount }} 个影子AI
                </span>
              </div>
            </div>

            <div class="client-services" v-if="parseServices(client).length > 0">
              <span
                v-for="svc in parseServices(client).slice(0, 6)"
                :key="svc.domain"
                :class="['service-chip', svc.riskLevel]"
                :title="svc.domain + ' · 发现来源: ' + svc.source"
              >
                {{ svc.name }}
              </span>
              <span v-if="parseServices(client).length > 6" class="service-chip more">
                +{{ parseServices(client).length - 6 }}
              </span>
            </div>

            <div class="client-footer">
              <span class="client-scan-time">
                最近扫描：{{ formatTime(client.scanTime) }}
              </span>
            </div>
          </article>
        </div>
      </el-card>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="`设备详情 · ${selectedClient?.hostname}`"
      direction="rtl"
      size="520px"
    >
      <div v-if="selectedClient" class="drawer-content">
        <div class="drawer-meta">
          <div class="drawer-meta-row">
            <span>主机名</span><strong>{{ selectedClient.hostname }}</strong>
          </div>
          <div class="drawer-meta-row">
            <span>用户</span><strong>{{ selectedClient.osUsername }}</strong>
          </div>
          <div class="drawer-meta-row">
            <span>系统</span><strong>{{ selectedClient.osType }}</strong>
          </div>
          <div class="drawer-meta-row">
            <span>客户端版本</span><strong>v{{ selectedClient.clientVersion }}</strong>
          </div>
          <div class="drawer-meta-row">
            <span>风险等级</span>
            <span :class="['risk-badge', selectedClient.riskLevel]">
              {{ riskLabel(selectedClient.riskLevel) }}
            </span>
          </div>
          <div class="drawer-meta-row">
            <span>最近扫描</span><strong>{{ formatTime(selectedClient.scanTime) }}</strong>
          </div>
        </div>

        <div class="drawer-section-title">发现的影子AI服务</div>

        <div v-if="parseServices(selectedClient).length === 0" class="drawer-empty">
          未发现未授权AI服务 ✓
        </div>

        <div
          v-for="svc in parseServices(selectedClient)"
          :key="svc.domain"
          :class="['drawer-service-item', svc.riskLevel]"
        >
          <div class="drawer-service-head">
            <span :class="['risk-badge', svc.riskLevel]">{{ riskLabel(svc.riskLevel) }}</span>
            <strong>{{ svc.name }}</strong>
            <el-tag size="small" type="info">{{ categoryLabel(svc.category) }}</el-tag>
          </div>
          <div class="drawer-service-meta">
            <span class="meta-item">🌐 {{ svc.domain }}</span>
            <span class="meta-item">📡 {{ sourceLabel(svc.source) }}</span>
            <span class="meta-item" v-if="svc.lastSeen">🕐 {{ formatTime(svc.lastSeen) }}</span>
          </div>
        </div>
      </div>
    </el-drawer>

  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  Refresh, Monitor, Warning, AlarmClock, Document,
  Download, Search, Loading,
} from '@element-plus/icons-vue';
import { shadowAiApi } from '../api/shadowAi';

// ── 状态 ──────────────────────────────────────────────────────────────────────
const loading     = ref(false);
const isMock      = ref(false);
const stats       = ref({ totalClients: 0, highRiskClients: 0, totalShadowAi: 0, recentReports: 0, riskDistribution: {} });
const clients     = ref([]);
const searchKeyword  = ref('');
const drawerVisible  = ref(false);
const selectedClient = ref(null);

// ── 计算属性 ──────────────────────────────────────────────────────────────────
const filteredClients = computed(() => {
  const kw = searchKeyword.value.toLowerCase();
  if (!kw) return clients.value;
  return clients.value.filter(c =>
    (c.hostname || '').toLowerCase().includes(kw) ||
    (c.osUsername || '').toLowerCase().includes(kw)
  );
});

const riskDistItems = computed(() => {
  const dist = stats.value.riskDistribution || {};
  const total = Object.values(dist).reduce((a, b) => a + b, 0) || 1;
  const order = ['high', 'medium', 'low', 'none'];
  return order.map(level => ({
    level,
    label: riskLabel(level),
    count: dist[level] || 0,
    pct: Math.round(((dist[level] || 0) / total) * 100),
  }));
});

// ── 工具函数 ──────────────────────────────────────────────────────────────────
function parseServices(client) {
  if (!client?.discoveredServices) return [];
  try {
    return JSON.parse(client.discoveredServices);
  } catch {
    return [];
  }
}

function riskLabel(level) {
  const map = { high: '高风险', medium: '中风险', low: '低风险', none: '安全' };
  return map[level] || level || '未知';
}

function categoryLabel(cat) {
  const map = {
    chat: '对话AI',
    image: '图像AI',
    search: 'AI搜索',
    code: '代码AI',
    local_llm: '本地LLM',
    embedding: '向量模型',
    other: '其他',
  };
  return map[cat] || cat || '未知';
}

function sourceLabel(src) {
  const map = {
    browser_history: '浏览器历史',
    network: '网络连接',
    process: '运行进程',
    dns: 'DNS记录',
  };
  return map[src] || src || '未知';
}

function formatTime(t) {
  if (!t) return '—';
  const d = new Date(t);
  if (isNaN(d)) return t;
  return d.toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-');
}

// ── 数据加载 ──────────────────────────────────────────────────────────────────
async function refresh() {
  loading.value = true;
  try {
    const [s, c] = await Promise.all([
      shadowAiApi.getStats(),
      shadowAiApi.getClients(),
    ]);
    stats.value   = s;
    clients.value = c;
    isMock.value  = !!(s?._mock || c?.[0]?._mock);
  } catch (err) {
    ElMessage.error('加载失败：' + (err.message || '网络异常'));
  } finally {
    loading.value = false;
  }
}

function selectClient(client) {
  selectedClient.value = client;
  drawerVisible.value  = true;
}

onMounted(refresh);
</script>

<style scoped>
.shadow-ai-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ── 页头 ─────────────────────────────────────────────────────────────────── */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
  padding: 28px 32px;
  border-radius: var(--radius-lg);
  background: rgba(8, 16, 28, 0.6);
  border: 1px solid rgba(169, 196, 255, 0.1);
}

.page-eyebrow {
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.24em;
  color: #64acff;
  text-transform: uppercase;
  margin-bottom: 6px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 8px;
}

.page-subtitle {
  color: var(--color-text-muted);
  font-size: 14px;
  line-height: 1.6;
  margin: 0;
  max-width: 600px;
}

.page-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  padding-top: 4px;
}

/* ── 统计卡片 ─────────────────────────────────────────────────────────────── */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 1100px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) {
  .stats-grid { grid-template-columns: 1fr; }
}

.stat-tile {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  border-radius: var(--radius-lg);
}

.stat-tile-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.stat-tile-icon.clients  { background: rgba(34, 116, 255, 0.18); color: #64acff; }
.stat-tile-icon.shadow   { background: rgba(255, 198, 0, 0.18);  color: #ffc600; }
.stat-tile-icon.danger   { background: rgba(255, 77, 77, 0.18);  color: #ff6b6b; }
.stat-tile-icon.reports  { background: rgba(27, 217, 180, 0.18); color: #1bd9b4; }

.stat-tile-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.stat-tile-label {
  font-size: 12px;
  color: var(--color-text-muted);
  font-weight: 600;
  letter-spacing: 0.04em;
}

.stat-tile-value {
  font-size: 30px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.1;
}

.stat-tile-value.danger-text { color: #ff6b6b; }

.stat-tile-hint {
  font-size: 11px;
  color: var(--color-text-muted);
}

/* ── 主网格 ───────────────────────────────────────────────────────────────── */
.main-grid {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 20px;
  align-items: start;
}

@media (max-width: 1000px) {
  .main-grid { grid-template-columns: 1fr; }
}

.risk-dist-card, .clients-card {
  background: rgba(8, 16, 28, 0.5) !important;
  border: 1px solid rgba(169, 196, 255, 0.1) !important;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.card-header {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text);
}

.panel-subtitle {
  font-size: 12px;
  color: var(--color-text-muted);
  margin: 4px 0 0;
  line-height: 1.5;
}

/* ── 风险分布 ─────────────────────────────────────────────────────────────── */
.risk-dist-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.risk-dist-item {}

.risk-dist-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.risk-dist-bar {
  height: 6px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 3px;
  overflow: hidden;
}

.risk-dist-fill {
  display: block;
  height: 100%;
  border-radius: 3px;
  transition: width 0.6s ease;
}

.risk-dist-fill.high   { background: #ff6b6b; }
.risk-dist-fill.medium { background: #ffc600; }
.risk-dist-fill.low    { background: #64acff; }
.risk-dist-fill.none   { background: #1bd9b4; }

/* ── 下载区域 ─────────────────────────────────────────────────────────────── */
.download-btns {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
}

.deploy-tip {
  margin-top: 10px;
  font-size: 11px;
  color: var(--color-text-muted);
  line-height: 1.6;
}

.deploy-tip a {
  color: #64acff;
  text-decoration: none;
}

/* ── 客户端列表 ───────────────────────────────────────────────────────────── */
.client-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 640px;
  overflow-y: auto;
  padding-right: 4px;
}

.client-card {
  padding: 16px 20px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  border: 1px solid rgba(169, 196, 255, 0.08);
}

.client-card:hover {
  border-color: rgba(100, 172, 255, 0.3);
  transform: translateY(-1px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.client-card.high   { border-left: 3px solid #ff6b6b; }
.client-card.medium { border-left: 3px solid #ffc600; }
.client-card.low    { border-left: 3px solid #64acff; }
.client-card.none   { border-left: 3px solid #1bd9b4; }

.client-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.client-hostname {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}

.client-meta {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-top: 2px;
}

.client-risk {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0;
}

.client-ai-count {
  font-size: 11px;
  color: var(--color-text-muted);
}

.client-services {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.client-footer {
  font-size: 11px;
  color: var(--color-text-muted);
}

/* ── 风险标签 ─────────────────────────────────────────────────────────────── */
.risk-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.risk-badge.high   { background: rgba(255, 77, 77, 0.18);  color: #ff8f8f; border: 1px solid rgba(255, 77, 77, 0.3); }
.risk-badge.medium { background: rgba(255, 198, 0, 0.18);  color: #ffd557; border: 1px solid rgba(255, 198, 0, 0.3); }
.risk-badge.low    { background: rgba(34, 116, 255, 0.18); color: #7ab8ff; border: 1px solid rgba(34, 116, 255, 0.3); }
.risk-badge.none   { background: rgba(27, 217, 180, 0.18); color: #1bd9b4; border: 1px solid rgba(27, 217, 180, 0.3); }

/* ── 服务标签 ─────────────────────────────────────────────────────────────── */
.service-chip {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.service-chip.high   { background: rgba(255, 77, 77, 0.14);  color: #ff8f8f; }
.service-chip.medium { background: rgba(255, 198, 0, 0.14);  color: #ffd557; }
.service-chip.low    { background: rgba(34, 116, 255, 0.14); color: #7ab8ff; }
.service-chip.more   { background: rgba(255, 255, 255, 0.06); color: var(--color-text-muted); }

/* ── 加载/空状态 ──────────────────────────────────────────────────────────── */
.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 48px 24px;
  color: var(--color-text-muted);
  font-size: 14px;
}

.loading-state .el-icon { font-size: 28px; }
.empty-state   .el-icon { font-size: 36px; }

/* ── 详情抽屉 ─────────────────────────────────────────────────────────────── */
.drawer-content {
  padding: 0 4px;
}

.drawer-meta {
  background: rgba(255, 255, 255, 0.03);
  border-radius: var(--radius-md);
  padding: 16px 20px;
  margin-bottom: 24px;
}

.drawer-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  font-size: 13px;
}

.drawer-meta-row:last-child { border-bottom: none; }
.drawer-meta-row span { color: var(--color-text-muted); }
.drawer-meta-row strong { color: var(--color-text); }

.drawer-section-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 12px;
}

.drawer-empty {
  padding: 24px;
  text-align: center;
  color: #1bd9b4;
  font-size: 14px;
  background: rgba(27, 217, 180, 0.06);
  border-radius: var(--radius-md);
}

.drawer-service-item {
  padding: 14px 16px;
  border-radius: var(--radius-md);
  margin-bottom: 10px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(169, 196, 255, 0.08);
}

.drawer-service-item.high   { border-left: 3px solid #ff6b6b; }
.drawer-service-item.medium { border-left: 3px solid #ffc600; }
.drawer-service-item.low    { border-left: 3px solid #64acff; }

.drawer-service-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.drawer-service-head strong {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.drawer-service-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.meta-item {
  font-size: 12px;
  color: var(--color-text-muted);
}

.scan-time { font-size: 11px; }
</style>
