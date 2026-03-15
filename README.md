
# AegisAI 前后端一体化启动说明

## 一、快速启动（本地开发 — H2 内存数据库）

本项目支持**无需安装 MySQL / Redis / Elasticsearch** 的本地开发模式，后端自动使用 H2 内存数据库并在缺少外部服务时优雅降级。

### 1. 启动后端（Spring Boot）
```bash
cd backend
mvn clean package -DskipTests
java -jar target/AegisAI-backend-0.1.0.jar
```
- API 文档：http://localhost:8080/swagger-ui.html
- H2 控制台：http://localhost:8080/h2-console（JDBC URL: `jdbc:h2:mem:aegisai`）

### 2. 启动前端（Vue + Vite）
```bash
npm install      # 项目根目录
npm run dev
```
- 访问：http://localhost:5173
- 默认账号：`admin` / `admin123`

### 3. 启动 Python AI 推理服务
```bash
cd python-service
pip install -r requirements.txt

# 推荐：开发环境设置 BERT_MOCK=true 跳过 BERT 模型下载（~1.3 GB），使用轻量 ML 分类器
BERT_MOCK=true python app.py
```
- 首次启动时若无异常检测模型文件，会自动运行 `gen_behavior_data.py` 和 `train_anomaly.py` 完成训练（约需 30–120 秒）。
- 如果自动训练失败，手动执行：
  ```bash
  python gen_behavior_data.py
  python train_anomaly.py
  BERT_MOCK=true python app.py
  ```
- 服务地址：http://localhost:5000
- 健康检查：http://localhost:5000/health

> **注意**：不设置 `BERT_MOCK=true` 时，服务将尝试从 HuggingFace 下载 BERT 模型（约 1.3 GB）。若网络不可用，服务仍会启动并自动降级为纯 ML 分类器，但会有警告日志。

### 4. 启动 Electron 客户端（可选）
```bash
cd electron
npm install
npm start
```
- 客户端默认连接 `http://localhost:5173`（Vue 开发服务器）。
- **首次使用必须先登录**：客户端加载 Vue 前端后，如未登录（或存在已过期的 mock 会话），会自动跳转到登录页面。请使用账号 `admin` / `admin123` 登录。
- 打包后需手动配置服务端地址（托盘菜单 → 服务器设置）。

---

## 二、威胁监控模拟器
```bash
cd python-service
python openclaw_simulator.py --count 1200 --url http://localhost:8080
```
> 上报接口 `/api/security/events/report` 无需登录 token，可直接调用。

---

## 三、完整生产部署（Docker Compose）
```bash
cp backend/src/main/resources/docker-compose.yml .
docker compose up -d
```
Docker Compose 会自动启动：MySQL、Redis、Elasticsearch、RabbitMQ、Spring Boot 后端、Python 推理服务。

---

## 四、环境变量（覆盖默认值）

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `AI_INFERENCE_URL` | `http://localhost:5000` | Python 推理服务地址 |
| `BERT_MOCK` | `false` | 设为 `true` 跳过 BERT 模型下载，使用轻量 ML 分类器 |
| `ELASTICSEARCH_URIS` | `http://localhost:9200` | Elasticsearch 地址 |
| `REDIS_HOST` | `localhost` | Redis 主机 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ 主机 |
| `CROSS_SITE_ALLOWED_ORIGINS` | `http://localhost:5173,...` | 允许的前端来源 |

Docker Compose 生产部署时，将上述变量设置为对应容器名（如 `AI_INFERENCE_URL=http://ai-inference:5000`）。

---

## 五、常见问题

**Q: 后端日志报 Redis 连接失败**
- 本地开发可忽略此警告，限流功能降级为放行。
- 生产环境请安装 Redis 并设置 `REDIS_HOST`。

**Q: 后端日志报 Elasticsearch 连接失败**
- 本地开发可忽略，审计日志 ES 全文检索不可用，但数据库层仍可正常查询。

**Q: AI 风险评级 / 异常检测显示"推理服务不可用"**
- 确认 Python 推理服务已启动（`python app.py`）且监听 `http://localhost:5000`。

**Q: Electron 客户端显示 `{"code":40100,"msg":"未登录或令牌失效"}`**
- 确认 Vue 开发服务器已启动在 `http://localhost:5173`。
- 或者在 Electron 客户端设置中修改服务端地址为正确的 URL。

---

如需扩展更多功能，请参考 `controller/service/entity/mapper` 目录结构补充业务代码。

