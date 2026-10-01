package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjx.entity.Category;
import com.gjx.entity.Product;
import com.gjx.mapper.ProductMapper;
import com.gjx.service.ICategoryService;
import com.gjx.service.IProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService {

    private final ICategoryService categoryService;

    /** 缓存 key 中关键词的最大长度：用户可控的 keyword 必须截断，否则可被刷爆 Redis。 */
    private static final int KEYWORD_MAX_LENGTH = 32;

    // ==================== 库存/销量变更：刻意不失效列表缓存 ====================
    // 库存与销量是高频写、值域分散的字段：每下一单都会按商品逐条调用 decreaseStock。
    // 若在这里挂 @CacheEvict(allEntries = true)，每下一单就会清空整个 productList，
    // 列表缓存几乎永远命中不了（等于没有缓存）；而且大量 key 在同一时刻集体失效、又同时重建，
    // 请求洪峰会直接打到数据库，形成缓存雪崩窗口。
    // 商品列表卡片本就不要求实时库存，交给 TTL 自然过期即可；
    // 确实需要立即失效的低频场景（如取消订单回补库存），由调用方显式调用 evictProductListCache()。

    @Override
    public boolean decreaseStock(Long productId, Integer quantity) {
        return baseMapper.decreaseStock(productId, quantity) > 0;
    }

    @Override
    public boolean increaseStock(Long productId, Integer quantity) {
        return baseMapper.increaseStock(productId, quantity) > 0;
    }

    /**
     * 显式失效商品列表缓存。
     * <p>
     * 这是库存/销量等高频变更场景下的“按需失效”入口：调用方在一批变更结束后调用一次，
     * 而不是在每次单条扣减时都触发（那会导致缓存恒冷 + 雪崩）。
     */
    @Override
    @CacheEvict(value = "productList", allEntries = true)
    public void evictProductListCache() {
        log.debug("清除商品列表缓存");
    }

    // ==================== 写操作统一失效商品列表缓存 ====================
    // 商品列表缓存了名称、价格、库存、销量与分类名，任一字段变化都必须让缓存失效，
    // 否则前端会在 TTL 内看到过期数据。

    @Override
    @CacheEvict(value = "productList", allEntries = true)
    public boolean save(Product entity) {
        return super.save(entity);
    }

    @Override
    @CacheEvict(value = "productList", allEntries = true)
    public boolean updateById(Product entity) {
        return super.updateById(entity);
    }

    @Override
    @CacheEvict(value = "productList", allEntries = true)
    public boolean removeById(Serializable id) {
        return super.removeById(id);
    }

    @Override
    @CacheEvict(value = "productList", allEntries = true)
    public boolean removeByIds(java.util.Collection<?> list) {
        return super.removeByIds(list);
    }

    @Override
    @CacheEvict(value = "productList", allEntries = true)
    public boolean updateBatchById(java.util.Collection<Product> entityList) {
        return super.updateBatchById(entityList);
    }

    /**
     * 规范化搜索关键词，作为缓存 key 的组成部分。
     * <p>
     * 处理顺序：{@code null} → 空串；去除首尾空白；统一小写；截断到 {@value #KEYWORD_MAX_LENGTH} 字符。
     * 不做规范化的话，攻击者可以刷随机关键词把 Redis 写满（缓存污染型 DoS），
     * 且“大小写/首尾空白”不同但语义相同的关键词会各占一条缓存，白白浪费内存。
     * <p>
     * 必须是 {@code public static}：{@link Cacheable} 的 key 表达式通过 SpEL 调用
     * {@code T(com.gjx.service.Impl.ProductServiceImpl).normalizeKeyword(#keyword)}。
     * <b>规范化发生在 key 求值阶段，而不是方法体内</b>——这正是它有效的关键：
     * 方法体内对形参再赋值，SpEL 拿到的仍是原始入参，key 依旧用旧值；
     * 只有让 key 表达式本身去调用这个辅助方法，才能保证 key 一定由规范化后的值构成。
     */
    public static String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return "";
        }
        String normalized = keyword.trim().toLowerCase();
        return normalized.length() > KEYWORD_MAX_LENGTH
                ? normalized.substring(0, KEYWORD_MAX_LENGTH)
                : normalized;
    }

    @Override
    @Cacheable(value = "productList",
            key = "'list_' + #categoryId + '_' + T(com.gjx.service.Impl.ProductServiceImpl).normalizeKeyword(#keyword) + '_' + #sortBy + '_' + #page + '_' + #size",
            unless = "#result == null || #result.records.isEmpty()")
    public Page<Product> listProducts(Long categoryId, String keyword, String sortBy, Integer page, Integer size) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(Product::getStatus, 1);

        if (categoryId != null) {
            queryWrapper.eq(Product::getCategoryId, categoryId);
        }

        // 与缓存 key 使用同一份规范化结果，保证“命中哪条缓存”和“查出的数据”一一对应
        String normalizedKeyword = normalizeKeyword(keyword);
        if (!normalizedKeyword.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Product::getName, normalizedKeyword)
                    .or().like(Product::getBrand, normalizedKeyword)
                    .or().like(Product::getDescription, normalizedKeyword));
        }

        // 排序逻辑
        if ("price_asc".equals(sortBy)) {
            queryWrapper.orderByAsc(Product::getPrice);
        } else if ("price_desc".equals(sortBy)) {
            queryWrapper.orderByDesc(Product::getPrice);
        } else if ("sales_desc".equals(sortBy)) {
            queryWrapper.orderByDesc(Product::getSales)
                    .orderByDesc(Product::getCreateTime);
        } else {
            queryWrapper.orderByDesc(Product::getCreateTime);
        }

        Page<Product> result = page(new Page<>(page, size), queryWrapper);

        fillCategoryNames(result.getRecords());

        return result;
    }

    /**
     * 关键词搜索（名称 / 品牌 / 描述，三列任一命中）。
     *
     * <p><b>关于关键词规范化：</b>本方法此前直接把原始 {@code keyword} 丢进 LIKE，
     * 而 {@link #listProducts} 会先 trim + 转小写——同一份搜索语义两套行为，
     * 最直观的表现是搜「" 沙发 "」（前后带空格）在列表页能搜到、在搜索页搜不到。
     * 现已统一走 {@link #normalizeKeyword}。
     *
     * <p><b>关于 {@code LIKE '%kw%'} 的全表扫描：</b>
     * 前置通配符使索引失效，三列 OR 更是必然全表扫。这里**有意保留**，原因有二：
     * <ol>
     *   <li>换成 {@code FULLTEXT ... WITH PARSER ngram} 虽然能走索引，但会**改变搜索语义**：
     *       ngram 是分词匹配而非子串匹配，且默认 {@code ngram_token_size=2}
     *       ——**单个汉字查询会直接返回空**（实测：「床」在 LIKE 下能搜到「实木双人床」，
     *       ngram 下返回 0 条）。用一个真实的功能回退换取当前数据量下测不出的性能收益，不划算。</li>
     *   <li>商品表当前为十位数量级，全表扫的代价可忽略。</li>
     * </ol>
     * 数据量达到万级、且业务能接受分词语义时再迁移。迁移步骤见
     * {@code database/README.md}「搜索的扩展路径」。
     */
    @Override
    @Cacheable(value = "productList",
            key = "'search_' + T(com.gjx.service.Impl.ProductServiceImpl).normalizeKeyword(#keyword) + '_' + #page + '_' + #size",
            unless = "#result == null || #result.records.isEmpty()")
    public Page<Product> searchProducts(String keyword, Integer page, Integer size) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(Product::getStatus, 1);

        String normalizedKeyword = normalizeKeyword(keyword);
        if (!normalizedKeyword.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Product::getName, normalizedKeyword)
                    .or().like(Product::getBrand, normalizedKeyword)
                    .or().like(Product::getDescription, normalizedKeyword));
        }

        queryWrapper.orderByDesc(Product::getSales)
                .orderByDesc(Product::getCreateTime);

        Page<Product> result = page(new Page<>(page, size), queryWrapper);

        fillCategoryNames(result.getRecords());

        return result;
    }

    @Override
    public Page<Product> adminListProducts(Integer page, Integer size, String name, Integer status) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();

        if (name != null && !name.isEmpty()) {
            queryWrapper.like(Product::getName, name);
        }

        if (status != null) {
            queryWrapper.eq(Product::getStatus, status);
        }

        queryWrapper.orderByDesc(Product::getCreateTime);

        Page<Product> result = page(new Page<>(page, size), queryWrapper);

        fillCategoryNames(result.getRecords());

        return result;
    }

    /**
     * 批量填充商品分类名称
     * <p>
     * 优化：先收集所有 categoryId，一次批量查询，再映射回各商品，
     * 避免每个商品单独查询一次数据库（N+1 问题）。
     */
    private void fillCategoryNames(java.util.List<Product> products) {
        if (products == null || products.isEmpty()) {
            return;
        }

        // 收集所有非空 categoryId
        java.util.Set<Long> categoryIds = products.stream()
                .map(Product::getCategoryId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());

        if (categoryIds.isEmpty()) {
            return;
        }

        // 一次批量查询所有分类
        java.util.Map<Long, String> nameMap = categoryService.listByIds(new java.util.ArrayList<>(categoryIds))
                .stream()
                .collect(java.util.stream.Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        // 映射分类名到商品
        for (Product product : products) {
            if (product.getCategoryId() != null) {
                product.setCategoryName(nameMap.get(product.getCategoryId()));
            }
        }
    }
}
