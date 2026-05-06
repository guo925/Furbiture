package com.gjx.controller.merchant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjx.common.R;
import com.gjx.common.ResultCode;
import com.gjx.entity.Category;
import com.gjx.entity.Order;
import com.gjx.entity.OrderItem;
import com.gjx.entity.Product;
import com.gjx.entity.User;
import com.gjx.mapper.CategoryMapper;
import com.gjx.mapper.OrderItemMapper;
import com.gjx.mapper.OrderMapper;
import com.gjx.mapper.ProductMapper;
import com.gjx.service.IProductService;
import com.gjx.service.IUserService;
import com.gjx.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家管理控制器
 */
@RestController
@RequestMapping("/api/merchant")
@Tag(name = "商家管理", description = "商家管理相关接口")
public class MerchantController {

    @Autowired
    private IProductService productService;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private com.gjx.service.ICategoryService categoryService;

    @Autowired
    private IUserService userService;

    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * 获取商家仪表盘统计
     */
    @Operation(summary = "获取商家统计", description = "获取商家的商品数量、订单数量、销售额等统计信息")
    @GetMapping("/dashboard")
    public R<?> getDashboard(HttpServletRequest request) {
        Long merchantId = getMerchantIdFromRequest(request);

        // 获取商品数量
        LambdaQueryWrapper<Product> productQuery = new LambdaQueryWrapper<>();
        productQuery.eq(Product::getMerchantId, merchantId);
        long productCount = productService.count(productQuery);

        // 获取商家商品ID列表
        List<Product> merchantProducts = productService.list(productQuery);
        List<Long> productIds = merchantProducts.stream().map(Product::getId).toList();

        // 获取包含商家商品的订单ID
        List<Long> orderIds = getMerchantOrderIds(productIds);

        // 获取待处理订单数量（status: 1=已付款，待发货）
        long pendingOrderCount = 0;
        if (!orderIds.isEmpty()) {
            LambdaQueryWrapper<Order> orderQuery = new LambdaQueryWrapper<>();
            orderQuery.in(Order::getId, orderIds);
            orderQuery.eq(Order::getStatus, 1);
            pendingOrderCount = orderMapper.selectCount(orderQuery);
        }

        // 计算销售额（已完成订单中商家商品的总金额）
        BigDecimal totalSales = BigDecimal.ZERO;
        if (!orderIds.isEmpty()) {
            LambdaQueryWrapper<Order> completedOrderQuery = new LambdaQueryWrapper<>();
            completedOrderQuery.in(Order::getId, orderIds);
            completedOrderQuery.eq(Order::getStatus, 3); // 已完成
            List<Order> completedOrders = orderMapper.selectList(completedOrderQuery);
            
            // 计算商家商品在订单中的销售额
            for (Order order : completedOrders) {
                LambdaQueryWrapper<OrderItem> itemQuery = new LambdaQueryWrapper<>();
                itemQuery.eq(OrderItem::getOrderId, order.getId());
                itemQuery.in(OrderItem::getProductId, productIds);
                List<OrderItem> items = orderItemMapper.selectList(itemQuery);
                for (OrderItem item : items) {
                    totalSales = totalSales.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }
        }

        // 获取今日订单数量
        long todayOrderCount = 0;
        if (!orderIds.isEmpty()) {
            LambdaQueryWrapper<Order> todayOrderQuery = new LambdaQueryWrapper<>();
            todayOrderQuery.in(Order::getId, orderIds);
            todayOrderQuery.apply("DATE(create_time) = CURDATE()");
            todayOrderCount = orderMapper.selectCount(todayOrderQuery);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("productCount", productCount);
        stats.put("pendingOrderCount", pendingOrderCount);
        stats.put("totalSales", totalSales);
        stats.put("todayOrderCount", todayOrderCount);

        return R.ok(stats);
    }

    /**
     * 获取商家订单ID列表
     */
    private List<Long> getMerchantOrderIds(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        LambdaQueryWrapper<OrderItem> orderItemQuery = new LambdaQueryWrapper<>();
        orderItemQuery.in(OrderItem::getProductId, productIds);
        List<OrderItem> orderItems = orderItemMapper.selectList(orderItemQuery);
        return orderItems.stream()
                .map(OrderItem::getOrderId)
                .distinct()
                .toList();
    }

    /**
     * 获取商家商品列表
     */
    @Operation(summary = "获取商家商品列表", description = "获取当前商家的商品列表")
    @GetMapping("/products")
    public R<?> getProducts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        Long merchantId = getMerchantIdFromRequest(request);

        Page<Product> productPage = new Page<>(page, size);
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getMerchantId, merchantId);
        queryWrapper.orderByDesc(Product::getCreateTime);

        Page<Product> result = productService.page(productPage, queryWrapper);

        return R.ok(result);
    }

    /**
     * 添加商品
     */
    @Operation(summary = "添加商品", description = "商家添加新商品")
    @PostMapping("/products")
    public R<?> addProduct(@RequestBody Product product, HttpServletRequest request) {
        Long merchantId = getMerchantIdFromRequest(request);

        product.setMerchantId(merchantId);
        product.setSales(0);
        product.setStatus(1); // 默认上架

        boolean success = productService.save(product);

        if (success) {
            return R.ok("添加成功");
        } else {
            return R.error(ResultCode.ERROR, "添加失败");
        }
    }

    /**
     * 更新商品
     */
    @Operation(summary = "更新商品", description = "商家更新自己的商品")
    @PutMapping("/products/{id}")
    public R<?> updateProduct(@PathVariable Long id, @RequestBody Product product, HttpServletRequest request) {
        Long merchantId = getMerchantIdFromRequest(request);

        // 检查商品是否属于当前商家
        Product existingProduct = productService.getById(id);
        if (existingProduct == null || !existingProduct.getMerchantId().equals(merchantId)) {
            return R.error(ResultCode.FORBIDDEN, "无权操作此商品");
        }

        product.setId(id);
        product.setMerchantId(merchantId);
        product.setSales(existingProduct.getSales()); // 保持销量不变

        boolean success = productService.updateById(product);

        if (success) {
            return R.ok("更新成功");
        } else {
            return R.error(ResultCode.ERROR, "更新失败");
        }
    }

    /**
     * 删除商品
     */
    @Operation(summary = "删除商品", description = "商家删除自己的商品")
    @DeleteMapping("/products/{id}")
    public R<?> deleteProduct(@PathVariable Long id, HttpServletRequest request) {
        Long merchantId = getMerchantIdFromRequest(request);

        // 检查商品是否属于当前商家
        Product existingProduct = productService.getById(id);
        if (existingProduct == null || !existingProduct.getMerchantId().equals(merchantId)) {
            return R.error(ResultCode.FORBIDDEN, "无权操作此商品");
        }

        boolean success = productService.removeById(id);

        if (success) {
            return R.ok("删除成功");
        } else {
            return R.error(ResultCode.ERROR, "删除失败");
        }
    }

    /**
     * 获取商家订单列表
     */
    @Operation(summary = "获取商家订单列表", description = "获取商家相关的订单列表")
    @GetMapping("/orders")
    public R<?> getOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        Long merchantId = getMerchantIdFromRequest(request);

        // 获取商家商品ID列表
        LambdaQueryWrapper<Product> productQuery = new LambdaQueryWrapper<>();
        productQuery.eq(Product::getMerchantId, merchantId);
        List<Product> merchantProducts = productService.list(productQuery);
        List<Long> productIds = merchantProducts.stream().map(Product::getId).toList();

        if (productIds.isEmpty()) {
            return R.ok(new Page<>(page, size));
        }

        // 获取包含商家商品的订单ID
        LambdaQueryWrapper<OrderItem> orderItemQuery = new LambdaQueryWrapper<>();
        orderItemQuery.in(OrderItem::getProductId, productIds);
        List<OrderItem> orderItems = orderItemMapper.selectList(orderItemQuery);
        List<Long> orderIds = orderItems.stream()
                .map(OrderItem::getOrderId)
                .distinct()
                .toList();

        if (orderIds.isEmpty()) {
            return R.ok(new Page<>(page, size));
        }

        // 分页查询订单
        Page<Order> orderPage = new Page<>(page, size);
        LambdaQueryWrapper<Order> orderQuery = new LambdaQueryWrapper<>();
        orderQuery.in(Order::getId, orderIds);
        if (status != null) {
            orderQuery.eq(Order::getStatus, status);
        }
        orderQuery.orderByDesc(Order::getCreateTime);

        Page<Order> result = orderMapper.selectPage(orderPage, orderQuery);

        return R.ok(result);
    }

    /**
     * 获取商家订单详情
     */
    @Operation(summary = "获取商家订单详情", description = "获取商家订单的详细信息")
    @GetMapping("/orders/{orderNo}")
    public R<?> getOrderDetail(@PathVariable String orderNo, HttpServletRequest request) {
        Long merchantId = getMerchantIdFromRequest(request);

        // 获取订单
        LambdaQueryWrapper<Order> orderQuery = new LambdaQueryWrapper<>();
        orderQuery.eq(Order::getOrderNo, orderNo);
        Order order = orderMapper.selectOne(orderQuery);

        if (order == null) {
            return R.error(ResultCode.NOT_FOUND, "订单不存在");
        }

        // 获取订单项
        LambdaQueryWrapper<OrderItem> orderItemQuery = new LambdaQueryWrapper<>();
        orderItemQuery.eq(OrderItem::getOrderId, order.getId());
        List<OrderItem> orderItems = orderItemMapper.selectList(orderItemQuery);

        // 过滤出商家自己的订单项
        LambdaQueryWrapper<Product> productQuery = new LambdaQueryWrapper<>();
        productQuery.eq(Product::getMerchantId, merchantId);
        List<Product> merchantProducts = productService.list(productQuery);
        List<Long> merchantProductIds = merchantProducts.stream().map(Product::getId).toList();

        List<OrderItem> merchantOrderItems = orderItems.stream()
                .filter(item -> merchantProductIds.contains(item.getProductId()))
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("orderItems", merchantOrderItems);

        return R.ok(result);
    }

    /**
     * 更新订单状态
     */
    @Operation(summary = "更新订单状态", description = "商家更新订单状态（如发货）")
    @PutMapping("/orders/{orderNo}/status")
    public R<?> updateOrderStatus(
            @PathVariable String orderNo,
            @RequestBody Map<String, Integer> statusUpdate,
            HttpServletRequest request) {
        Long merchantId = getMerchantIdFromRequest(request);

        Integer status = statusUpdate.get("status");

        // 获取订单
        LambdaQueryWrapper<Order> orderQuery = new LambdaQueryWrapper<>();
        orderQuery.eq(Order::getOrderNo, orderNo);
        Order order = orderMapper.selectOne(orderQuery);

        if (order == null) {
            return R.error(ResultCode.NOT_FOUND, "订单不存在");
        }

        // 更新订单状态
        order.setStatus(status);
        orderMapper.updateById(order);

        return R.ok("更新成功");
    }

    /**
     * 获取分类列表（分页）
     */
    @Operation(summary = "获取分类列表", description = "获取商品分类列表（分页）")
    @GetMapping("/categories")
    public R<?> getCategories(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {
        Page<Category> categoryPage;
        
        if (name != null && !name.isEmpty()) {
            categoryPage = categoryService.lambdaQuery()
                    .like(Category::getName, name)
                    .page(new Page<>(page, size));
        } else {
            categoryPage = categoryService.page(new Page<>(page, size));
        }
        
        // 设置父分类名称
        for (Category category : categoryPage.getRecords()) {
            if (category.getParentId() != null && category.getParentId() > 0) {
                Category parent = categoryService.getById(category.getParentId());
                if (parent != null) {
                    category.setParentName(parent.getName());
                }
            }
        }
        
        return R.ok(categoryPage);
    }

    /**
     * 添加分类
     */
    @Operation(summary = "添加分类", description = "商家添加商品分类")
    @PostMapping("/categories")
    public R<?> addCategory(@RequestBody Category category) {
        // 设置层级
        if (category.getParentId() == null || category.getParentId() == 0) {
            category.setLevel(1);
            category.setParentId(0L);
        } else {
            Category parent = categoryMapper.selectById(category.getParentId());
            if (parent != null) {
                category.setLevel(parent.getLevel() + 1);
            } else {
                return R.error(ResultCode.NOT_FOUND, "父分类不存在");
            }
        }
        
        // 设置默认排序
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        
        boolean success = categoryMapper.insert(category) > 0;
        
        if (success) {
            return R.ok("添加成功");
        } else {
            return R.error(ResultCode.ERROR, "添加失败");
        }
    }

    /**
     * 更新分类
     */
    @Operation(summary = "更新分类", description = "商家更新商品分类")
    @PutMapping("/categories/{id}")
    public R<?> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        Category existingCategory = categoryMapper.selectById(id);
        if (existingCategory == null) {
            return R.error(ResultCode.NOT_FOUND, "分类不存在");
        }
        
        category.setId(id);
        
        // 如果修改了父分类，更新层级
        if (category.getParentId() != null && !category.getParentId().equals(existingCategory.getParentId())) {
            if (category.getParentId() == 0) {
                category.setLevel(1);
            } else {
                Category parent = categoryMapper.selectById(category.getParentId());
                if (parent != null) {
                    category.setLevel(parent.getLevel() + 1);
                } else {
                    return R.error(ResultCode.NOT_FOUND, "父分类不存在");
                }
            }
        }
        
        boolean success = categoryMapper.updateById(category) > 0;
        
        if (success) {
            return R.ok("更新成功");
        } else {
            return R.error(ResultCode.ERROR, "更新失败");
        }
    }

    /**
     * 删除分类
     */
    @Operation(summary = "删除分类", description = "商家删除商品分类")
    @DeleteMapping("/categories/{id}")
    public R<?> deleteCategory(@PathVariable Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            return R.error(ResultCode.NOT_FOUND, "分类不存在");
        }
        
        // 检查是否有子分类
        LambdaQueryWrapper<Category> childQuery = new LambdaQueryWrapper<>();
        childQuery.eq(Category::getParentId, id);
        List<Category> children = categoryMapper.selectList(childQuery);
        if (!children.isEmpty()) {
            return R.error(ResultCode.ERROR, "请先删除子分类");
        }
        
        // 检查是否有商品使用此分类
        LambdaQueryWrapper<Product> productQuery = new LambdaQueryWrapper<>();
        productQuery.eq(Product::getCategoryId, id);
        long productCount = productMapper.selectCount(productQuery);
        if (productCount > 0) {
            return R.error(ResultCode.ERROR, "该分类下存在商品，无法删除");
        }
        
        boolean success = categoryMapper.deleteById(id) > 0;
        
        if (success) {
            return R.ok("删除成功");
        } else {
            return R.error(ResultCode.ERROR, "删除失败");
        }
    }

    /**
     * 构建分类树
     */
    private List<Category> buildCategoryTree(List<Category> categories) {
        List<Category> tree = new java.util.ArrayList<>();
        java.util.Map<Long, Category> categoryMap = new java.util.HashMap<>();
        
        // 将所有分类放入map，方便查找
        for (Category category : categories) {
            category.setChildren(new java.util.ArrayList<>());
            categoryMap.put(category.getId(), category);
        }
        
        // 构建树形结构
        for (Category category : categories) {
            if (category.getParentId() == null || category.getParentId() == 0) {
                tree.add(category);
            } else {
                Category parent = categoryMap.get(category.getParentId());
                if (parent != null) {
                    parent.getChildren().add(category);
                }
            }
        }
        
        return tree;
    }

    /**
     * 获取商家信息
     */
    @Operation(summary = "获取商家信息", description = "获取当前商家的信息")
    @GetMapping("/info")
    public R<?> getMerchantInfo(HttpServletRequest request) {
        String username = AuthenticationUtil.getUsernameFromRequest(request);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        User user = userService.findByUsername(username);

        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("phone", user.getPhone());
        info.put("email", user.getEmail());
        info.put("name", user.getName());

        return R.ok(info);
    }

    /**
     * 更新商家信息
     */
    @Operation(summary = "更新商家信息", description = "更新当前商家的基本信息")
    @PutMapping("/info")
    public R<?> updateMerchantInfo(@RequestBody Map<String, String> infoData, HttpServletRequest request) {
        String username = AuthenticationUtil.getUsernameFromRequest(request);
        User user = userService.findByUsername(username);

        if (infoData.containsKey("email")) {
            user.setEmail(infoData.get("email"));
        }
        if (infoData.containsKey("phone")) {
            user.setPhone(infoData.get("phone"));
        }
        if (infoData.containsKey("name")) {
            user.setName(infoData.get("name"));
        }

        userService.updateById(user);
        return R.ok("更新成功");
    }

    /**
     * 修改商家密码
     */
    @Operation(summary = "修改密码", description = "修改当前商家的密码")
    @PostMapping("/info/password")
    public R<?> changePassword(@RequestBody Map<String, String> passwordData, HttpServletRequest request) {
        String username = AuthenticationUtil.getUsernameFromRequest(request);
        User user = userService.findByUsername(username);

        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");

        if (!user.getPassword().equals(oldPassword)) {
            return R.error(ResultCode.FORBIDDEN, "原密码错误");
        }

        user.setPassword(newPassword);
        userService.updateById(user);

        return R.ok("密码修改成功");
    }

    /**
     * 从请求中获取商家ID
     */
    private Long getMerchantIdFromRequest(HttpServletRequest request) {
        String username = AuthenticationUtil.getUsernameFromRequest(request);
        User user = userService.findByUsername(username);
        return user.getId();
    }
}