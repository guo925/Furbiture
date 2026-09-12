# 前端界面优化 — 剩余工作清单

> **本文档用途**：交接给新会话继续执行用。已完成的部分不必重做，剩余任务已标注精确坐标（file:line），
> 新会话无需重新调研整个前端即可开工。
>
> **生成时间**：2026-09-12 ｜ **基线提交**：`ca5ea26`

---

## 一、已完成（不要重做）

| 提交 | 内容 |
|---|---|
| `b55d33e` | 后端生产上线加固（越权/并发/密钥等 9 项 + 可运维 7 项） |
| `ca5ea26` | 前端界面优化（可见缺陷 3 项 + 品牌统一 + 体验细节 + 清理 3043 行死代码） |

### 前端已完成的具体项

- **3 个可见缺陷**：首页「主题市场」空白（`HomeView` 误调 `productAPI.getCategories`）、分页英文（缺 `locale: zhCn`）、管理端表格列被固定列遮挡
- **品牌统一**：新增 `frontend/src/assets/styles/element-theme.css`，把 Element Plus 的 `--el-*` 变量全部映射到项目令牌；三端品牌色收敛为橙色系
- **深色模式接线**：`frontend/src/composables/useTheme.js` 的 `applyTheme` 现在同时设置 `data-theme`（项目令牌）与 `html.dark`（EP 组件）
- **购物车加固**：三个破坏性操作补确认弹窗 + loading + 失败回滚
- **商家端 loading**：`MerchantProducts` / `MerchantOrders` / `MerchantCategories` 补加载态，消除"闪一下暂无商品"
- **死代码清理**：删除 16 个文件 3043 行

---

## 二、必须遵守的约束

1. **不要推翻前台（用户端）的设计** —— `UserLayout`、`ProductCard`、`api/request.js` 是全项目质量最高的一段，应作为"标准答案"向外扩散，而不是被重写。
2. **颜色一律用令牌** —— 品牌色是 `var(--color-primary)`（`#ff5000`），渐变是 `var(--color-primary-gradient)`。**不要再写 `#ff5000` 字面量**。
3. **Element Plus 主题已接通** —— 需要 EP 组件变色时改 `frontend/src/assets/styles/element-theme.css`，**不要在组件里覆写**。
4. **改任何硬编码色前先确认它对应哪个令牌** —— 大部分硬编码值就是令牌本身的值（见第三节）。
5. **本项目 `.claude/` 有提交门禁** —— 提交用 `/git-save`，禁止 `git commit`（会被 `pre-commit` hook 拦截）。

---

## 三、剩余任务

### 批次 A：管理端响应式（优先级最高）

**问题**：管理端 **6 个视图全部 0 处 `@media`**，而侧边栏固定 220px + `height:100vh; overflow:hidden`，窄屏直接挤压内容区不可用。

| 文件 | 现状 |
|---|---|
| `frontend/src/views/admin/AdminLayout.vue` | 0 处 @media；侧边栏 `width:220px` 固定，折叠仅切换 64/220px，无移动端抽屉 |
| `frontend/src/views/admin/DashboardView.vue` | 0 处；统计卡栅格无断点 |
| `frontend/src/views/admin/ProductsView.vue` | 0 处 |
| `frontend/src/views/admin/OrdersView.vue` | 0 处；列宽 `200/170/220` + `fixed="right"` |
| `frontend/src/views/admin/UsersView.vue` | 0 处 |
| `frontend/src/views/admin/CategoriesView.vue` | 0 处；树形表格 |

**建议做法**：
1. 先定**统一断点**（见批次 B 第 2 项），管理端建议用 `1024px` 作为侧边栏转抽屉的临界点
2. `AdminLayout` 加 `<1024px` 时侧边栏转为 `el-drawer`（`el-menu` 已支持 `router` 模式，复用即可）
3. 表格页统一加横向滚动兜底，避免列被 `fixed` 列遮挡

**验收**：浏览器缩到 768px 宽，管理端 5 个页面均可用（侧边栏可开合、表格可横向滚动、无内容被裁切）。

### 批次 B：硬编码色清理（工作量最大）

**现状**：硬编码 hex **392 处**，`var(--...)` 引用 **117 处**，令牌采用率约 23%。

**最高频硬编码值恰好就是令牌本身**：

| 硬编码值 | 出现次数 | 对应令牌 |
|---|---|---|
| `#fff` | 74 | `--color-bg-secondary` |
| `#ff5000` | 42 | `--color-primary` |
| `#333` | 19 | `--color-text-primary` |
| `#666` | 12 | `--color-text-secondary` |
| `#eee` | 11 | `--color-border-light` |
| `#999` | 11 | `--color-text-muted` |
| `#f0f0f0` | 7 | `--color-border-light` |
| `#fff3ed` | 5 | `--color-primary-light` |

**按文件集中度排序（建议从高到低处理）**：

```
frontend/src/views/merchant/MerchantDashboard.vue   59 处   ← 最多
frontend/src/views/admin/DashboardView.vue          27 处
frontend/src/components/UserLayout.vue              16 处
frontend/src/views/user/ProductDetailView.vue       12 处
frontend/src/views/admin/ProductsView.vue           12 处
frontend/src/views/admin/OrdersView.vue             12 处
frontend/src/views/user/OrdersView.vue              11 处
frontend/src/views/admin/AdminLayout.vue            10 处
frontend/src/views/user/HomeView.vue                 9 处
frontend/src/views/user/CheckoutView.vue             9 处
frontend/src/components/MerchantLayout.vue           9 处
```

> 注：`design-tokens.css` 自身有 36 处 hex，那是**令牌定义**，不要动。

**顺带修两件事**：

1. **统一断点**：全项目有 **12 个不同断点值**（`560px`×6、`980px`×3、`900px`×3、`760px`×2、`720px`×2，另有 `920/860/820/768/600/1160px`）。
   建议收敛为三档：**移动 `<640px`**、**平板 `640–1024px`**、**桌面 `>1024px`**。
2. **修 `App.vue` 字体覆盖**：`frontend/src/App.vue:20` 写了 `font-family: Arial, sans-serif;`，把 `frontend/src/assets/styles/design-tokens.css:71` 的 `--font-family` 全局覆盖了，导致该令牌从未生效。改为 `font-family: var(--font-family);`。

**验收**：`grep -rc '#[0-9a-fA-F]\{6\}' frontend/src` 的计数显著下降；切换深色模式时无"白底残留"。

### 批次 C：深色模式收尾

**问题**：`data-theme='dark'` 令牌已就绪、EP 暗色也已接通，但大量**写死的白色背景**让深色模式呈"深色底 + 白色块"的斑驳状态。

**写死白底最集中的文件**：

```
frontend/src/views/user/ProductDetailView.vue     5 处
frontend/src/views/user/CheckoutView.vue          4 处
frontend/src/views/merchant/MerchantDashboard.vue 4 处
frontend/src/components/UserLayout.vue            4 处
frontend/src/views/user/OrdersView.vue            3 处
frontend/src/views/user/HomeView.vue              3 处
frontend/src/views/user/CartView.vue              3 处
frontend/src/views/user/AddressView.vue           3 处
```

**ECharts 图表在深色下仍是白底**（当前 4 个图表实例都写死了颜色）：

| 文件:行 | 内容 |
|---|---|
| `frontend/src/views/admin/DashboardView.vue:142` | `backgroundColor: '#fff'` |
| `frontend/src/views/admin/DashboardView.vue:144` | `textStyle: { color: '#2d3748' }` |
| `frontend/src/views/merchant/MerchantDashboard.vue:333` | `backgroundColor: '#fff'` |
| `frontend/src/views/merchant/MerchantDashboard.vue:335` | `textStyle: { color: '#17202b' }` |
| `frontend/src/views/merchant/MerchantDashboard.vue:375` | `legend: { bottom: 0, textStyle: { color: '#667085' } }` |

**建议做法**：抽一个工具函数（如 `frontend/src/utils/chartTheme.js`）根据 `useTheme().isDark` 返回主题色对象，图表 `setOption` 时注入，并在主题切换时 `dispose()` + 重建（现有代码已在重绘前 `dispose()`，沿用该纪律）。

**验收**：切到深色模式，逐页检查无白色块残留；两个仪表板的图表背景随主题变化。

---

## 四、验证方法

本次会话用的验证手段，可直接复用：

**启动服务**：
```bash
cd /home/gyz/IdeaProjects/Furbiture && mvn spring-boot:run &   # 后端 9090
cd frontend && npx vite --host &                               # 前端 3003
```

**测试账号**（密码均 `123456`）：`admin` / `user1` / `merchant1`(id=3) / `merchant2`(id=4)

**浏览器验证**：Playwright 的会话在多次调用间**容易丢失**（页面变 `about:blank`）。
**务必用 `browser_run_code_unsafe` 把「导航 + 操作 + 截图」写在同一个脚本里**，一次原子调用完成，避免跨调用丢会话。

**颜色是否生效**：直接读计算样式，比看截图更可靠——
```js
page.evaluate(() => getComputedStyle(document.documentElement).getPropertyValue('--el-color-primary'))
```

**注意**：截图会落到仓库根目录污染 git，**用完记得移走**（`mv *.png /tmp/`）。

---

## 五、已知的环境问题（不是 bug，别去修）

| 现象 | 原因 |
|---|---|
| 商品图片全部 500 | 开发环境上传目录是 `/tmp/furniture-uploads/`，**重启后被系统清空**，而数据库里的 `main_image` 路径仍指向旧文件 |
| `R.error(String)` 返回 code 500 | 已知缺陷，客户端错误应为 400/404。已在 `.claude/CLAUDE.md` 坑位清单中记录 |
| 管理端/商家端仪表板数据对不上 | 销售额口径不同（管理端只算 `PAID`，商家端算 `PAID+DELIVERED+COMPLETED`），**是设计差异不是 bug** |

---

## 六、前置操作（需要人工执行）

```bash
cd /home/gyz/IdeaProjects/Furbiture && git rm -r --cached frontend/dist
```

`frontend/dist` 的 57 个构建产物仍在版本控制中（`.gitignore` 已修为 `frontend/dist/`，但跟踪关系未解除）。
该命令被本会话的安全门禁拦截多次，需手动执行。**只改索引，不删磁盘文件**；回滚用 `git reset HEAD frontend/dist`。

---

## 七、建议执行顺序

1. **批次 A（响应式）** —— 用户感知最直接：窄屏现在完全不可用
2. **批次 B 第 2 项（修 `App.vue` 字体 + 统一断点）** —— 一行改动，且是 A 和 C 的前置
3. **批次 C（深色模式）** —— 依赖 B 的令牌清理
4. **批次 B 第 1 项（392 处硬编码色）** —— 工作量最大、收益最慢，放最后

每完成一批跑一次 `/git-save` 固化（含 Java 改动时会自动触发 tester + quality-engineer 质检）。

---

## 八、附：本次会话踩过的坑（避免重复）

| 坑 | 说明 |
|---|---|
| `pkill -f "spring-boot:run"` 会自杀 | 模式文本出现在执行它的 shell 自身命令行里，导致命令被自己杀掉（exit 144）。改用端口终止：`fuser -k 9090/tcp` |
| `mvn -o spring-boot:run` 会失败 | Actuator 的传递依赖 `HdrHistogram` 未缓存；去掉 `-o` 联网拉一次即可 |
| 管道后的 `$?` 不是命令的退出码 | `cmd 2>&1 \| head -5 && echo OK` 取的是 `head` 的退出码（恒为 0），结论必错。用 `${PIPESTATUS[0]}` 或先落变量 |
| `Set.of(...).contains(null)` 抛 NPE | 不可变集合不允许 null 查询。判空要写在前面：`v == null \|\| !set.contains(v)` |
| Jackson 反序列化 JSON 数组 | `Map<String,Object>` 里的数组实际是 `ArrayList<Integer>`，强转 `List<Long>` 会在访问元素时抛 `ClassCastException`。用 `((Number) item).longValue()` |
| Redis 缓存加白名单要放行 `Page` | `productList` 缓存的是 `Page<Product>`，其类型是 `com.baomidou.mybatisplus...Page`，不在 `com.gjx.` 前缀内，漏放会让缓存读取抛 `InvalidTypeIdException` |
