import request from './request';
import { isMockSession } from '../utils/auth';
import { shouldUseApiFallback } from './fallback';

// ── Mock数据 ──────────────────────────────────────────────────────────────────

const MOCK_STATS = {
  totalClients: 8,
  highRiskClients: 2,
  totalShadowAi: 14,
  recentReports: 23,
  riskDistribution: { none: 3, low: 2, medium: 1, high: 2 },
  _mock: true,
};

const MOCK_CLIENTS = [
  {
    id: 1,
    clientId: 'demo-client-001',
    hostname: 'DESKTOP-ZHANGSAN',
    osUsername: 'zhangsan',
    osType: 'Windows',
    clientVersion: '1.0.0',
    shadowAiCount: 4,
    riskLevel: 'high',
    scanTime: '2026-03-14T08:30:00',
    discoveredServices: JSON.stringify([
      { name: 'ChatGPT', domain: 'chat.openai.com', category: 'chat', source: 'browser_history', riskLevel: 'high', lastSeen: '2026-03-14T08:25:00' },
      { name: 'Claude', domain: 'claude.ai', category: 'chat', source: 'browser_history', riskLevel: 'high', lastSeen: '2026-03-14T07:50:00' },
      { name: 'Midjourney', domain: 'midjourney.com', category: 'image', source: 'browser_history', riskLevel: 'medium', lastSeen: '2026-03-13T16:20:00' },
      { name: 'Perplexity', domain: 'perplexity.ai', category: 'search', source: 'network', riskLevel: 'medium', lastSeen: '2026-03-14T08:10:00' },
    ]),
  },
  {
    id: 2,
    clientId: 'demo-client-002',
    hostname: 'LAPTOP-LISI',
    osUsername: 'lisi',
    osType: 'Windows',
    clientVersion: '1.0.0',
    shadowAiCount: 3,
    riskLevel: 'medium',
    scanTime: '2026-03-14T09:00:00',
    discoveredServices: JSON.stringify([
      { name: 'Kimi', domain: 'kimi.moonshot.cn', category: 'chat', source: 'browser_history', riskLevel: 'medium', lastSeen: '2026-03-14T08:55:00' },
      { name: 'Doubao', domain: 'doubao.com', category: 'chat', source: 'browser_history', riskLevel: 'medium', lastSeen: '2026-03-14T09:00:00' },
      { name: 'Ollama', domain: 'localhost:11434', category: 'local_llm', source: 'process', riskLevel: 'medium', lastSeen: '2026-03-14T09:00:00' },
    ]),
  },
  {
    id: 3,
    clientId: 'demo-client-003',
    hostname: 'MAC-WANGWU',
    osUsername: 'wangwu',
    osType: 'macOS',
    clientVersion: '1.0.0',
    shadowAiCount: 1,
    riskLevel: 'low',
    scanTime: '2026-03-14T07:00:00',
    discoveredServices: JSON.stringify([
      { name: 'Gemini', domain: 'gemini.google.com', category: 'chat', source: 'browser_history', riskLevel: 'low', lastSeen: '2026-03-13T20:00:00' },
    ]),
  },
  {
    id: 4,
    clientId: 'demo-client-004',
    hostname: 'DESKTOP-ZHAOLIU',
    osUsername: 'zhaoliu',
    osType: 'Windows',
    clientVersion: '1.0.0',
    shadowAiCount: 0,
    riskLevel: 'none',
    scanTime: '2026-03-14T09:10:00',
    discoveredServices: JSON.stringify([]),
  },
];

// ── API ───────────────────────────────────────────────────────────────────────

export const shadowAiApi = {
  /**
   * 获取影子AI治理统计摘要（供工作台和视图使用）。
   */
  async getStats() {
    if (isMockSession()) {
      return { ...MOCK_STATS, _mock: true };
    }
    try {
      return await request.get('/client/stats');
    } catch (error) {
      if (shouldUseApiFallback(error)) {
        return { ...MOCK_STATS, _mock: true };
      }
      throw error;
    }
  },

  /**
   * 获取所有客户端最新扫描报告列表。
   */
  async getClients() {
    if (isMockSession()) {
      return MOCK_CLIENTS.map(c => ({ ...c, _mock: true }));
    }
    try {
      return await request.get('/client/list');
    } catch (error) {
      if (shouldUseApiFallback(error)) {
        return MOCK_CLIENTS.map(c => ({ ...c, _mock: true }));
      }
      throw error;
    }
  },

  /**
   * 获取指定客户端的历史扫描记录。
   */
  async getHistory(clientId) {
    if (isMockSession()) {
      return MOCK_CLIENTS.filter(c => c.clientId === clientId).map(c => ({ ...c, _mock: true }));
    }
    try {
      return await request.get('/client/history', { params: { clientId } });
    } catch (error) {
      if (shouldUseApiFallback(error)) {
        return [];
      }
      throw error;
    }
  },
};
