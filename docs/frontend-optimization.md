# 前端界面优化 — 剩余工作清单

> **本文档用途**：交接给新会话继续执行用。已完成的部分不必重做，剩余任务已标注精确坐标（file:line），
> 新会话无需重新调研整个前端即可开工。
>
> **生成时间**：2026-09-12 ｜ **基线提交**：`ca5ea26`
> **最近更新**：2026-09-14 ｜ 批次 A 完成，批次 B/C 未开始

---

## 一、已完成（不要重做）

| 提交 | 内容 |
|---|---|
| `b55d33e` | 后端生产上线加固（越权/并发/密钥等 9 项 + 可运维 7 项） |
| `ca5ea26` | 前端界面优化（可见缺陷 3 项 + 品牌统一 + 体验细节 + 清理 3043 行死代码） |
| `de769fe` | 新增本文档 |
| *（未提交）* | **批次 A：管理端响应式**，详见下方 |

### 前端已完成的具体项

- **3 个可见缺陷**：首页「主题市场」空白（`HomeView` 误调 `productAPI.getCategories`）、分页英文（缺 `locale: zhCn`）、管理端表格列被固定列遮挡
- **品牌统一**：新增 `frontend/src/assets/styles/element-theme.css`，把 Element Plus 的 `--el-*` 变量全部映射到项目令牌；三端品牌色收敛为橙色系
- **深色模式接线**：`frontend/src/composables/useTheme.js` 的 `applyTheme` 现在同时设置 `data-theme`（项目令牌）与 `html.dark`（EP 组件）
- **购物车加固**：三个破坏性操作补确认弹窗 + loading + 失败回滚
- **商家端 loading**：`MerchantProducts` / `MerchantOrders` / `MerchantCategories` 补加载态，消除"闪一下暂无商品"
- **死代码清理**：删除 16 个文件 3043 行

### 批次 A：管理端响应式（2026-09-14 完成，未提交）

**新引入的公共设施**：`frontend/src/composables/useBreakpoint.js`

```js
export const BREAKPOINTS = Object.freeze({ mobile: 640, narrow: 1024 })
// 返回 { isNarrow, isMobile, isTablet（计算属性 = isNarrow && !isMobile）}
```

用 `matchMedia` 而非 `window.resize` 事件 —— **`resize` 每像素都触发，`matchMedia` 只在跨阈值时触发一次**。断点值在此收敛，批次 B/C 应复用，不要再新增临时值。

**逐文件改动**：

| 文件 | 改了什么 |
|---|---|
| `AdminLayout.vue` | 侧边栏 `v-if="!isNarrow"`；窄屏改用 `el-drawer`（`direction="ltr"`、220px）+ 顶栏汉堡按钮；菜单 5 个硬编码 `el-menu-item` 收敛为 `navItems` 数组；面包屑首项与「系统运行中」标签在窄屏隐藏 |
| `DashboardView.vue` | 4 个统计卡 `:span="6"` → `:xs="24" :sm="12" :lg="6"`；图表列加 `:xs/:md`；新增 `ResizeObserver` 驱动 `chart.resize()`；**`grid.top` 由 `'10%'` 改为固定 `36`**（见下方坑位） |
| `ProductsView.vue` | 弹窗 `:width="isMobile ? '92%' : '640px'"`；工具栏堆叠、分页居中换行 |
| `OrdersView.vue` | 抽屉 `:size="isMobile ? '88%' : '480px'"`；同上媒体查询 |
| `CategoriesView.vue` | 弹窗 `:width="isMobile ? '92%' : '500px'"`；同上 |
| `UsersView.vue` | 搜索框去掉内联 `style`；表格列补 `min-width`（120/130/180）避免省略号收缩；两个弹窗响应式 |

**横向滚动兜底**：Element Plus 的真实滚动容器是内层 `.el-scrollbar__wrap`，不是 `el-table` 本身。探针要查那个元素才准。已实测 768px 下每列（含 `fixed="right"` 操作列）都可滚到。

**移除 4 处 `max-width: 1400px`**（`DashboardView` / `ProductsView` / `OrdersView` / `CategoriesView` 的 `.admin-page`）。
**决策依据**：用户明确要求「放开，大屏铺满」。原上限在 1920px 屏留白 300px、2560px 屏留白 940px。**不要改成更大的值**（如 1800px）——在 2560px 屏上仍留白，等于没解决。

> 注：删除后 `.admin-page` 在三个模板里仍作为根容器标记存在，但已无任何 CSS 规则。这是有意的（保留语义标记，避免无收益的模板改动）。

**批次 A 的验证状态**：`npm run build` 通过；768px 下表格横向滚动实测可达；`grid.top` 修复经元素截图确认。**未做**：全 6 页逐页 768px 人工验收。

---

## 二、必须遵守的约束

1. **不要推翻前台（用户端）的设计** —— `UserLayout`、`ProductCard`、`api/request.js` 是全项目质量最高的一段，应作为"标准答案"向外扩散，而不是被重写。
2. **颜色一律用令牌** —— 品牌色是 `var(--color-primary)`（`#ff5000`），渐变是 `var(--color-primary-gradient)`。**不要再写 `#ff5000` 字面量**。
3. **Element Plus 主题已接通** —— 需要 EP 组件变色时改 `frontend/src/assets/styles/element-theme.css`，**不要在组件里覆写**。
4. **改任何硬编码色前先确认它对应哪个令牌** —— 大部分硬编码值就是令牌本身的值（见第三节）。
5. **本项目 `.claude/` 有提交门禁** —— 提交用 `/git-save`，禁止 `git commit`（会被 `pre-commit` hook 拦截）。
6. **断点统一用 `useBreakpoint.js` 的常量** —— 不要再写 `560px`/`980px`/`900px` 这类一次性值（全项目原有 12 个不同断点）。

---

## 三、剩余任务

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

1. ~~**统一断点**~~ —— **已在批次 A 完成**：断点定义收敛到 `frontend/src/composables/useBreakpoint.js`（`mobile: 640` / `narrow: 1024`）。
   **仍需做**：把各文件里遗留的 `@media (max-width: 560px/980px/900px/760px/720px/920px/860px/820px/768px/600px/1160px)` 迁移到这两个值。**注意 `@media` 里的 px 无法直接引用 JS 常量**，要么保留字面量但只用 640/1024 两个值，要么改用 CSS 自定义属性 + `@custom-media`（需 PostCSS 插件，当前未装）。
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

> ⚠️ 批次 A 给 `DashboardView` 加了行号漂移（新增 `ResizeObserver` 等约 20 行），上表行号**需重新核对**。

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

**只验证前端构建**（不需要起后端，秒级）：
```bash
npm --prefix /home/gyz/IdeaProjects/Furbiture/frontend run build
```

**浏览器验证**：Playwright 的会话在多次调用间**容易丢失**（页面变 `about:blank`）。
**务必用 `browser_run_code_unsafe` 把「导航 + 操作 + 截图」写在同一个脚本里**，一次原子调用完成，避免跨调用丢会话。

**量测视口与滚动**（比看截图可靠）：直接 `page.evaluate` 读 `scrollWidth`/`clientWidth` 与 `getComputedStyle`。
查 Element Plus 表格横向滚动时**必须查内层 `.el-scrollbar__wrap`**，`el-table` 本身不滚。

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

## 六、前置操作（状态）

```bash
cd /home/gyz/IdeaProjects/Furbiture && git rm -r --cached frontend/dist
```

**✅ 已执行（2026-09-14）**，但**尚未提交**：当前暂存区有 57 条 `D frontend/dist/...`。

`.gitignore:35` 已修为 `frontend/dist/`（原先的 `/dist/` 锚定仓库根目录，匹配不到前端产物）。
`git ls-files frontend/dist` 现返回 0 —— 跟踪关系已解除。**只改了索引，磁盘文件未删**；回滚用 `git reset HEAD frontend/dist`。

> 下一次 `/git-save` 会把这批删除一并提交。

---

## 七、建议执行顺序

1. ~~**批次 A（响应式）**~~ —— ✅ 2026-09-14 完成
2. **批次 B 第 2 项（修 `App.vue` 字体）** —— 一行改动，且是 C 的前置。**先做这个**
3. **提交当前进度** —— 批次 A + dist 解跟踪已经攒了不少改动，跑一次 `/git-save` 固化
4. **批次 C（深色模式）** —— 依赖 B 的令牌清理；ECharts 主题建议与批次 B 的 `#fff` 清理合并做
5. **批次 B 第 1 项（392 处硬编码色）** —— 工作量最大、收益最慢，放最后

每完成一批跑一次 `/git-save` 固化（含 Java 改动时会自动触发 tester + quality-engineer 质检）。

---

## 八、附：踩过的坑（避免重复）

### 本次会话（2026-09-14）新增

| 坑 | 说明 |
|---|---|
| **ECharts `grid.top` 不能用百分比** | `DashboardView` 原本写 `grid.top: '10%'`。百分比**随容器高度等比缩放**，窄屏时图表高度 240px → 只剩 24px，不足以容纳 `yAxis.name`（"销售额 (元)"），轴名被画到画布外裁掉。**改用固定像素 `36`**。凡是给轴标签留空间的值都应用固定像素 |
| **`scoped` 样式命中不到 Element Plus 的 DOM** | `el-drawer` / `el-dialog` 的 DOM 由 EP 渲染在组件作用域外，`<style scoped>` 的选择器打不中。需另开一个**非 scoped** 的 `<style>` 块，且选择器一律以自定义类名起头（如 `.admin-nav-drawer .el-drawer__body`）避免污染全局 |
| **`matchMedia` 优于 `resize` 事件** | `resize` 每像素触发一次回调，`matchMedia('(max-width: Npx)')` 的 `change` 只在跨越阈值时触发一次。断点检测用后者 |

### 历史（2026-09-12）

| 坑 | 说明 |
|---|---|
| `pkill -f "spring-boot:run"` 会自杀 | 模式文本出现在执行它的 shell 自身命令行里，导致命令被自己杀掉（exit 144）。改用端口终止：`fuser -k 9090/tcp` |
| `mvn -o spring-boot:run` 会失败 | Actuator 的传递依赖 `HdrHistogram` 未缓存；去掉 `-o` 联网拉一次即可 |
| 管道后的 `$?` 不是命令的退出码 | `cmd 2>&1 \| head -5 && echo OK` 取的是 `head` 的退出码（恒为 0），结论必错。用 `${PIPESTATUS[0]}` 或先落变量 |
| `Set.of(...).contains(null)` 抛 NPE | 不可变集合不允许 null 查询。判空要写在前面：`v == null \|\| !set.contains(v)` |
| Jackson 反序列化 JSON 数组 | `Map<String,Object>` 里的数组实际是 `ArrayList<Integer>`，强转 `List<Long>` 会在访问元素时抛 `ClassCastException`。用 `((Number) item).longValue()` |
| Redis 缓存加白名单要放行 `Page` | `productList` 缓存的是 `Page<Product>`，其类型是 `com.baomidou.mybatisplus...Page`，不在 `com.gjx.` 前缀内，漏放会让缓存读取抛 `InvalidTypeIdException` |
