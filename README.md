
# AegisAI 前后端一体化启动说明

## 一、数据库初始化
1. 启动 MySQL 8.0，创建数据库：
	```sql
	CREATE DATABASE aegisai DEFAULT CHARACTER SET utf8mb4;
	```
2. 执行 backend/src/main/resources/db.sql 初始化表结构。

## 二、后端启动
1. 配置 Redis、Elasticsearch 并启动。
2. 修改 backend/src/main/resources/application.yml 数据库连接为 aegisai。
3. 进入 backend 目录，执行：
	```bash
	mvn clean package -DskipTests
	java -jar target/AegisAI-backend-0.1.0.jar
	```
4. Swagger 文档访问：http://localhost:8080/swagger-ui.html

## 三、前端启动
1. 进入前端根目录（含 package.json 的目录）。
2. 安装依赖：
	```bash
	npm install
	```
3. 启动开发服务器：
	```bash
	npm run dev
	```
4. 访问 http://localhost:5173

默认账号：`admin` / `admin123`（首次启动后端且表为空时自动创建）。

## 四、功能入口
- 首页：/
- 全局搜索：/global-search
- 数据资产管理：/data-asset（含敏感扫描、共享审批）
- AI 模型与调用成本：/ai-model-manage 、/model-cost
- 审计日志与报告：/audit-log、/audit-report
- 告警闭环与风险：/alerts、/risk-event-manage
- 脱敏预览：/desense-preview；主体权利工单：/subject-request

## 五、常见问题
- 依赖下载慢/失败：请配置 ~/.m2/settings.xml 使用阿里云镜像。
- 数据库、Redis、ES 未启动会导致后端报错。

---
如需扩展更多功能，请参考 controller/service/entity/mapper 目录结构补充业务代码。
