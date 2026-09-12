# ============================================================
# 后端镜像：多阶段构建（构建阶段带 Maven，运行阶段只留 JRE）
# ============================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先单独拷贝 pom.xml 并预下载依赖，利用 Docker 层缓存：
# 只要 pom.xml 未变，后续改代码就不会重新下载依赖
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 安装 curl 供容器健康检查使用
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# 以非 root 用户运行，降低容器被攻破后的影响面
RUN groupadd -r app && useradd -r -g app app \
    && mkdir -p /data/furniture-uploads \
    && chown -R app:app /app /data/furniture-uploads

COPY --from=build /build/target/*.jar app.jar
USER app

EXPOSE 9090

# 健康检查直接复用 actuator 的 health 端点，容器编排可据此判断是否就绪
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:9090/actuator/health || exit 1

# 生产 profile 通过环境变量注入；MaxRAMPercentage 让 JVM 按容器内存限制自动设置堆大小
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
