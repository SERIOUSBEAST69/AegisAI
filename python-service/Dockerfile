# 基础镜像
FROM python:3.10-slim

# 配置国内源（全局生效）
ENV PIP_INDEX_URL=https://mirrors.aliyun.com/pypi/simple/
ENV PIP_TRUSTED_HOST=mirrors.aliyun.com

# 设置工作目录
WORKDIR /app

# 升级 pip
RUN pip install --upgrade pip

# 复制依赖文件并安装（添加 PyTorch CPU 索引）
COPY requirements.txt ./
RUN pip install --no-cache-dir --default-timeout=1000 \
    --extra-index-url https://download.pytorch.org/whl/cpu \
    -r requirements.txt

# 复制应用代码
COPY app.py ./

# ========== 注释掉模型复制（后续有模型再打开） ==========
# COPY models/ ./models/

# 暴露端口
EXPOSE 5000

# 启动命令
CMD ["python", "app.py"]