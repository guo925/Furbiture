# Furbiture Plan B — 变更记录

## Phase 0: 基础与安全（2026-08-08）

### 0.1 安全漏洞修复
- **Bug A**: `AdminAuthController` 路径从 `/admin/auth` 改为 `/api/admin/auth`，SecurityConfig 添加 `/api/admin/auth/login` 白名单
- **Bug B**: `AdminProductImageController` 和 `AdminProductSpecController` 路径添加 `/api` 前缀
- **Bug C**: 创建 `UpdateUserRequest` DTO 防止用户越权修改 role 字段，修改 `UserController.updateUser()`

### 0.2 数据库迁移
- 新建 `database/migration_v3_planb.sql`
- 新增表：`favorite`（收藏）、`review`（评价）、`notification`（通知）、`operation_log`（操作日志）、`discount`（折扣码）
- `order` 表新增列：`merchant_id`、`refund_amount`、`refund_reason`、`refund_time`

### 0.3 统一设计系统
- 新建 `frontend/src/assets/styles/design-tokens.css`：CSS 变量（品牌色/语义色/背景色/文字色/边框/圆角/阴影/间距/字体）+ 深色模式
- 新建 `frontend/src/composables/useTheme.js`：主题切换 composable（localStorage 持久化 + data-theme 属性切换 + 过渡动画）
- 修改 `frontend/src/main.js`：导入 design-tokens.css

---

## Phase 1: 核心技术修复（2026-08-08）

### 1.1 DTO/VO 规范化
- 新建 `UpdateUserRequest.java`：仅含 email/phone/avatar
- 新建 `ProductDetailVO.java`：聚合商品 + 图片 + 规格 + 评价统计
- 新建 `OrderDetailVO.java`：聚合订单 + 商品明细 + 收货地址
- 新建 `CreateProductRequest.java`：商品创建/更新 DTO（含参数校验）

### 1.2 HTTP 状态码修复
- 修改 `GlobalExceptionHandler.java`：所有异常处理器返回 `ResponseEntity<R<?>>` 并设置正确 HTTP 状态码
  - BusinessException → 401/403/404/400/409（根据 ResultCode 映射）
  - MethodArgumentNotValidException → 400
  - AccessDeniedException → 403
  - IllegalArgumentException → 400
  - Exception → 500
- 修改 `frontend/src/api/request.js`：响应拦截器增加 400/403/404/409 处理，增加业务码检查

### 1.3 N+1 查询和内存聚合优化
- 修改 `ProductServiceImpl.fillCategoryNames()`：批量查询分类名替代逐条查询
- 修改 `OrderItemMapper.java`：新增 `getHotProducts()` SQL GROUP BY 聚合查询
- 修改 `OrderMapper.java`：新增 `selectTotalSalesByStatus()`、`selectTotalSalesByDateAndStatus()`、`countOrdersByDate()`、`countTodayOrders()` SQL 聚合方法
- 修改 `OrderServiceImpl`：getHotProducts/getTotalSales/getTodaySales/getSalesByDate/countTodayOrders/countOrdersByDate 全部改为 SQL 聚合

### 1.4 缓存/JWT/索引修复
- 修改 `MerchantCategoryController`：写操作改用 service 层方法以触发 `@CacheEvict`，移除不再需要的 `CategoryMapper` 注入
- 修改 `JwtAuthenticationFilter`：解析 JWT 后将 userId/username 存入 request attribute
- 修改 `AuthenticationUtil`：优先从 request attribute 读取，避免重复解析 JWT
- 修改 `migration_v2.sql`：使用存储过程安全添加索引，避免 `CREATE INDEX IF NOT EXISTS` 语法错误和重复索引

---

## Phase 2: 用户端体验增强（2026-08-08）

### 2.1 布局统一
- 重写 `ProfileView.vue`：使用 UserLayout 布局，移除重复导航栏/页脚（650行→180行），统一品牌色
- 重写 `OrderDetailView.vue`：使用 UserLayout 布局，新增物流时间线组件

### 2.2 沉浸式商品展示
- 新建 `ProductGallery.vue`：缩略图导航 + 鼠标悬停放大镜 + 缩放预览 + 移动端适配

### 2.4 搜索增强
- 后端：新增 `GET /api/products/suggestions` 搜索建议端点
- 前端：新建 `useSearchHistory.js` composable（localStorage 搜索历史）
- 修改 `UserLayout.vue`：搜索框增加自动补全下拉面板 + 300ms 防抖

### 2.7 评价系统
- 新建 `Review` 实体、`ReviewMapper`、`IReviewService`/`ReviewServiceImpl`
- 新建 `ReviewController`：评价列表、评价统计、创建评价
- 新建 `StarRating.vue`：可交互星级评分组件

### 2.9 商品收藏
- 新建 `Favorite` 实体、`FavoriteMapper`、`IFavoriteService`/`FavoriteServiceImpl`
- 新建 `FavoriteController`：切换收藏、检查状态、收藏列表
- 新建 `FavoritesView.vue`：收藏商品网格 + 购物车 + 取消收藏
- 新建 `favorite.js` API 模块，路由新增 `/favorites`

---

## Phase 3: 商家端体验增强（2026-08-08）

### 3.1 商家路由改造
- 改造为嵌套路由：MerchantLayout 作为父组件 + `<router-view />` 渲染子页面，与管理端一致
- 从 5 个商家视图中移除独立的 `<MerchantLayout>` 包裹和 import
- MerchantLayout 标题改为从 route.meta 动态读取

### 3.2 经营罗盘（数据大屏）
- 重写 `MerchantDashboardController`：新增 4 个端点
  - `GET /dashboard/revenue-trend`：近 7 天营收趋势
  - `GET /dashboard/category-distribution`：品类分布饼图数据
  - `GET /dashboard/order-funnel`：订单漏斗（待付款→已付款→已发货→已完成）
  - `GET /dashboard/top-products`：热销商品 TOP 10
- 优化统计查询，减少内存聚合

### 3.7 批量操作
- 新增 `PUT /api/merchant/products/batch/status`：批量上下架
- 新增 `DELETE /api/merchant/products/batch`：批量删除

### 3.8 多图管理
- 新建 `ImageUploader.vue`：拖拽上传 + 排序 + 删除 + 进度条 + 数量限制

---

## Phase 4: 管理端增强（2026-08-08）

### 4.1 恢复 UsersView
- 从 backup 目录恢复 `UsersView.vue` 到主视图目录
- 已使用正确的 `adminAPI` 路径，开箱即用

### 4.4 商家审核
- 新建 `AdminAuditController`：审核列表 + 通过（自动升级为 MERCHANT 角色）+ 拒绝（含原因）

### 4.6 AOP 操作日志
- 新建 `@OperationLog` 注解：标记需要记录日志的 Controller 方法
- 新建 `OperationLogAspect`：AOP 切面自动记录操作人、操作类型、目标、IP、时间
- 新建 `OperationLog` 实体 + `OperationLogMapper`

### 4.7 退款审核流程
- 新增 `POST /api/orders/{orderNo}/refund`：用户申请退款
- 退款时自动取消订单并恢复库存

---

## Phase 5: 实时通知 + 深色模式 + 打磨（2026-08-08）

### 5.1 WebSocket + STOMP 实时通知
- 添加 `spring-boot-starter-websocket` 依赖
- 新建 `WebSocketConfig.java`：STOMP 消息代理 + SockJS 端点
- 新建 `Notification` 实体 + `NotificationMapper` + `INotificationService` + `NotificationServiceImpl`
- `NotificationServiceImpl.push()` 同时写入数据库和 WebSocket 实时推送
- 新建 `NotificationController`：通知列表 + 未读数量 + 标记已读 + 全部已读
- WebSocket 推送目标：`/user/{userId}/queue/notifications`

### 5.4 深色模式切换
- 在三端布局统一添加深色模式切换按钮（太阳/月亮图标）
- `UserLayout`：顶部导航栏
- `MerchantLayout`：顶部账号栏
- `AdminLayout`：头部右侧
- 使用 `useTheme` composable 实现平滑 CSS transition 切换

### 5.5 其他改进
- 用户端导航栏新增"我的收藏"入口
- 添加 `@EnableAspectJAutoProxy` 支持 AOP 日志
