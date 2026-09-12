# ============================================================
# 前端镜像：Node 构建出静态产物，交给 Nginx 托管并反向代理后端
# ============================================================

# ---------- 构建阶段 ----------
FROM node:20-alpine AS build
WORKDIR /build

# 先拷贝依赖清单，依赖未变时复用缓存层
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---------- 运行阶段 ----------
FROM nginx:1.27-alpine

RUN rm -f /etc/nginx/conf.d/default.conf
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost/ >/dev/null || exit 1
