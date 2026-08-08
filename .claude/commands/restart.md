---
name: restart
description: 重启 Furbiture 前后端服务
---

# 重启 Furbiture 项目

停止并重新启动前后端服务。

## 步骤

### 1. 停止旧进程

```bash
fuser -k 9090/tcp 2>/dev/null
pkill -f "vite" 2>/dev/null
sleep 2
echo "旧进程已停止"
```

### 2. 编译后端（如代码有变更）

```bash
cd /home/gyz/IdeaProjects/Furbiture && mvn compile -q 2>&1 | tail -3
```

### 3. 启动后端

```bash
cd /home/gyz/IdeaProjects/Furbiture && mvn spring-boot:run -q > /tmp/backend.log 2>&1 &
```

### 4. 等待后端就绪（最多 25 秒）

```bash
for i in $(seq 1 25); do
  if curl -s 'http://localhost:9090/api/categories/tree' > /dev/null 2>&1; then
    echo "后端就绪 (${i}s)"
    break
  fi
  sleep 1
done
```

### 5. 启动前端

```bash
cd /home/gyz/IdeaProjects/Furbiture/frontend && npx vite --host > /tmp/frontend.log 2>&1 &
sleep 4
```

### 6. 验证

```bash
echo "=== 后端 9090 ===" && curl -s 'http://localhost:9090/api/categories/tree' | python3 -c "import sys,json;print(f'code={json.load(sys.stdin)[\"code\"]}')" 2>/dev/null
echo "=== 前端 ===" && FRONTEND_PORT=$(grep -oP 'localhost:\K\d+' /tmp/frontend.log 2>/dev/null | head -1) && echo "访问 http://localhost:${FRONTEND_PORT:-3004}"
```
