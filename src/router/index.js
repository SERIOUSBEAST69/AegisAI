import { createRouter, createWebHistory } from 'vue-router';

import Home from '../views/Home.vue';
import DataAsset from '../views/DataAsset.vue';
import AuditLog from '../views/AuditLog.vue';
import AiModelManage from '../views/AiModelManage.vue';
import UserManage from '../views/UserManage.vue';
import RoleManage from '../views/RoleManage.vue';
import PermissionManage from '../views/PermissionManage.vue';
import ApprovalManage from '../views/ApprovalManage.vue';
import PolicyManage from '../views/PolicyManage.vue';
import RiskEventManage from '../views/RiskEventManage.vue';
import ModelCost from '../views/ModelCost.vue';
import SensitiveScan from '../views/SensitiveScan.vue';
import Alerts from '../views/Alerts.vue';
import DataShare from '../views/DataShare.vue';
import SubjectRequest from '../views/SubjectRequest.vue';
import DesensePreview from '../views/DesensePreview.vue';
import GlobalSearch from '../views/GlobalSearch.vue';
import Login from '../views/Login.vue';
import ModelList from '../views/ai/ModelList.vue';
import ModelMonitor from '../views/ai/ModelMonitor.vue';

const routes = [
  { path: '/login', name: 'Login', component: Login, meta: { public: true } },
  { path: '/', name: 'Home', component: Home },
  { path: '/data-asset', name: 'DataAsset', component: DataAsset },
  { path: '/audit-log', name: 'AuditLog', component: AuditLog },
  { path: '/ai-model-manage', name: 'AiModelManage', component: AiModelManage },
  { path: '/model-cost', name: 'ModelCost', component: ModelCost },
  { path: '/user-manage', name: 'UserManage', component: UserManage },
  { path: '/role-manage', name: 'RoleManage', component: RoleManage },
  { path: '/permission-manage', name: 'PermissionManage', component: PermissionManage },
  { path: '/approval-manage', name: 'ApprovalManage', component: ApprovalManage },
  { path: '/policy-manage', name: 'PolicyManage', component: PolicyManage },
  { path: '/risk-event-manage', name: 'RiskEventManage', component: RiskEventManage },
  { path: '/sensitive-scan', name: 'SensitiveScan', component: SensitiveScan },
  { path: '/alerts', name: 'Alerts', component: Alerts },
  { path: '/data-share', name: 'DataShare', component: DataShare },
  { path: '/subject-request', name: 'SubjectRequest', component: SubjectRequest },
  { path: '/desense-preview', name: 'DesensePreview', component: DesensePreview },
  { path: '/global-search', name: 'GlobalSearch', component: GlobalSearch },
  { path: '/ai/models', name: 'ModelList', component: ModelList },
  { path: '/ai/monitor', name: 'ModelMonitor', component: ModelMonitor }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// 简单登录守卫：无 token 则跳转登录
router.beforeEach((to, from, next) => {
  if (to.meta.public) return next();
  const token = localStorage.getItem('token');
  if (!token) return next('/login');
  next();
});

export default router;
