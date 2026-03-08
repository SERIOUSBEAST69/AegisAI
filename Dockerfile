# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# 复制 pom.xml 并下载依赖（利用缓存）
COPY backend/pom.xml ./backend/pom.xml
RUN mvn -f backend/pom.xml dependency:go-offline

# 复制全部源代码
COPY backend ./backend

# 打包（跳过测试）
RUN mvn -f backend/pom.xml clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre
WORKDIR /app

# 使用通配符复制任意 JAR 文件，避免版本号硬编码
COPY --from=builder /app/backend/target/*.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]