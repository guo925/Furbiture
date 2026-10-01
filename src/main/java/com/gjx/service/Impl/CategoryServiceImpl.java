package com.gjx.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjx.common.BusinessException;
import com.gjx.common.ResultCode;
import com.gjx.entity.Category;
import com.gjx.mapper.CategoryMapper;
import com.gjx.service.ICategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 分类服务实现类
 * <p>
 * 分类（category 表）是**全平台共享数据**：表里没有 merchant_id，没有任何商家"拥有"某条分类。
 * 因此分类的写操作只对管理员开放，商家端只读（详见 MerchantCategoryController）。
 * 本类负责分类树的构建，以及层次结构的两条不变式：
 * <ol>
 *   <li>不存在父子环 —— 否则整棵子树无法从根遍历到，会从分类树上静默消失；</li>
 *   <li>level 与 parent_id 始终自洽 —— 改父级时子孙的 level 必须一并重算。</li>
 * </ol>
 */
@Slf4j
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService {

    /** 顶级分类的 parent_id 约定值，与建表脚本的 {@code parent_id BIGINT NOT NULL DEFAULT 0} 保持一致 */
    private static final Long ROOT_PARENT_ID = 0L;

    /** 层级递归深度上限，仅作为兜底：正常数据不会触达，存量成环数据会被它截断以免无限递归 */
    private static final int MAX_LEVEL_DEPTH = 64;

    @Override
    @Cacheable(value = "categoryTree", key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<Category> getCategoryTree() {
        // 获取所有分类
        List<Category> allCategories = list();

        // 处理空值情况
        if (allCategories == null || allCategories.isEmpty()) {
            return new ArrayList<>();
        }

        // 按父ID分组。必须先归一 parentId：Collectors.groupingBy 的分类器一旦返回 null，
        // 会直接抛 NPE → 分类树接口 500 → 前台首页整块不可用。
        // 存量数据可能是 NULL（建表脚本现已改为 NOT NULL DEFAULT 0，但老库未必迁移过）。
        Map<Long, List<Category>> categoryMap = allCategories.stream()
                .collect(Collectors.groupingBy(c -> normalizeParentId(c.getParentId())));

        // 构建分类树
        List<Category> rootCategories = new ArrayList<>();
        for (Category category : allCategories) {
            // 用 Long.equals 而不是 == 0：后者会对 null 拆箱，同样 NPE
            if (ROOT_PARENT_ID.equals(normalizeParentId(category.getParentId()))) { // 根分类
                rootCategories.add(category);
                buildCategoryTree(category, categoryMap);
            }
        }

        return rootCategories;
    }

    /**
     * 清空分类树缓存。
     * <p>
     * 分类变更会同时影响分类树与商品列表（商品列表内嵌 categoryName），
     * 因此写操作需要同时失效两个缓存，否则商品列表会残留旧分类名。
     */
    @CacheEvict(value = "categoryTree", key = "'all'")
    public void evictCategoryTreeCache() {
        log.debug("清除分类树缓存");
    }

    /**
     * 覆盖保存方法：归一 parentId 并按父级推导 level，同时清除分类树与商品列表缓存。
     * <p>
     * level 一律由服务端推导，不接受客户端注入；这样「建表默认 level=1」不会让
     * 子分类错误地停留在第 1 层。
     */
    @Override
    @CacheEvict(value = {"categoryTree", "productList"}, allEntries = true)
    public boolean save(Category entity) {
        Long parentId = normalizeParentId(entity.getParentId());
        entity.setParentId(parentId);
        entity.setLevel(deriveLevel(parentId));
        return super.save(entity);
    }

    /**
     * 覆盖更新方法：对「移动分类」做环检测并按新父级重算层级，同时清除分类树与商品列表缓存。
     * <p>
     * <b>为什么必须做环检测</b>：分类树是<b>从根（parent_id = 0）向下递归</b>构建的。
     * 若把某个分类的 parentId 改成它自己或它的某个子孙，这棵子树就再也无法从根遍历到，
     * 它会从分类树上<b>静默消失</b>——不是报错，而是数据看起来"人间蒸发"，
     * 且缓存里也一并消失，排查成本极高。
     * <p>
     * <b>为什么必须重算子孙 level</b>：level 是相对深度，父级一变，整棵子树都要跟着平移；
     * 只改当前节点会让所有子孙的 level 全部漂移。
     * <p>
     * 出错时抛 {@link BusinessException}，由 GlobalExceptionHandler 统一转成
     * {@code R.error(ResultCode.PARAM_ERROR, "不能将分类挂到自身或其子分类下")}。
     */
    @Override
    @CacheEvict(value = {"categoryTree", "productList"}, allEntries = true)
    public boolean updateById(Category entity) {
        if (entity.getId() == null) {
            return false;
        }
        Category existing = getById(entity.getId());
        if (existing == null) {
            // 影响行数为 0，交由调用方判断并返回 404
            return false;
        }

        Long oldParentId = normalizeParentId(existing.getParentId());
        // parentId 未提供视为「不改动父级」：避免只改名称/状态的局部更新把分类误移到顶级
        Long newParentId = entity.getParentId() == null
                ? oldParentId
                : normalizeParentId(entity.getParentId());
        boolean parentChanged = !newParentId.equals(oldParentId);

        if (entity.getParentId() != null) {
            entity.setParentId(newParentId);
        }

        Integer newLevel = existing.getLevel();
        if (parentChanged) {
            if (isSelfOrDescendant(entity.getId(), newParentId)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "不能将分类挂到自身或其子分类下");
            }
            newLevel = deriveLevel(newParentId);
            entity.setLevel(newLevel);
        }

        boolean updated = super.updateById(entity);
        if (updated && parentChanged) {
            // 自身 level 已重算，但子孙的 level 会随之整体漂移 → 递归同步
            recalcDescendantLevels(entity.getId(), newLevel, 1);
        }
        return updated;
    }

    /**
     * 覆盖删除方法，自动清除分类树与商品列表缓存
     */
    @Override
    @CacheEvict(value = {"categoryTree", "productList"}, allEntries = true)
    public boolean removeById(java.io.Serializable id) {
        return super.removeById(id);
    }

    /**
     * 按父级推导层级：顶级为 1，其余为父级 level + 1。
     *
     * @throws BusinessException 父分类不存在时抛 NOT_FOUND，避免写入指向不存在父级的悬空分类
     */
    private Integer deriveLevel(Long parentId) {
        if (ROOT_PARENT_ID.equals(parentId)) {
            return 1;
        }
        Category parent = getById(parentId);
        if (parent == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "父分类不存在");
        }
        Integer parentLevel = parent.getLevel();
        return (parentLevel == null ? 1 : parentLevel) + 1;
    }

    /**
     * 判断 candidateId 是否为 ancestorId 自身、或其后代（沿 parentId 逐级向上追溯）。
     * <p>
     * 终止条件：① 追到顶级（parent_id = 0）或父级记录已不存在；
     * ② 命中 ancestorId（说明 candidate 落在自己的子树里）；
     * ③ visitor 集合发现节点重复 —— 这条是**死循环保护**：存量数据若已成环，
     * 没有它本方法会永远转下去。
     *
     * @param ancestorId  被更新的分类自身 id
     * @param candidateId 拟设置的新父级 id
     * @return true 表示新父级是自己或自己的子孙，必须拒绝
     */
    private boolean isSelfOrDescendant(Long ancestorId, Long candidateId) {
        Long current = candidateId;
        Set<Long> visited = new HashSet<>();
        while (current != null && !ROOT_PARENT_ID.equals(current)) {
            if (!visited.add(current)) {
                // 存量数据已经成环，无法继续向上追溯；按「不是后代」处理，
                // 环本身应由数据修复脚本清理，不在本次写入路径里做无界遍历
                return false;
            }
            if (current.equals(ancestorId)) {
                return true;
            }
            Category parent = getById(current);
            if (parent == null) {
                return false;
            }
            current = normalizeParentId(parent.getParentId());
        }
        return false;
    }

    /**
     * 递归重算指定分类所有子孙的 level。
     * <p>
     * 直接走 {@code super.updateById}：内部重算不再重复触发一次环检测与缓存清理。
     * 深度超过 {@link #MAX_LEVEL_DEPTH} 即停止并告警——只有数据成环才会触达该分支。
     *
     * @param parentId    父分类 id
     * @param parentLevel 父分类已重算好的 level
     * @param depth       当前递归深度（从 1 开始）
     */
    private void recalcDescendantLevels(Long parentId, Integer parentLevel, int depth) {
        if (depth > MAX_LEVEL_DEPTH) {
            log.warn("[分类层级重算] 递归深度超过 {} 层，疑似分类数据成环，已停止 parentId={}", MAX_LEVEL_DEPTH, parentId);
            return;
        }
        List<Category> children = lambdaQuery()
                .select(Category::getId, Category::getParentId, Category::getLevel)
                .eq(Category::getParentId, parentId)
                .list();
        if (children == null || children.isEmpty()) {
            return;
        }
        int childLevel = (parentLevel == null ? 1 : parentLevel) + 1;
        for (Category child : children) {
            Category update = new Category();
            update.setId(child.getId());
            update.setLevel(childLevel);
            super.updateById(update);
            recalcDescendantLevels(child.getId(), childLevel, depth + 1);
        }
    }

    /**
     * 递归构建分类树
     */
    private void buildCategoryTree(Category parent, Map<Long, List<Category>> categoryMap) {
        List<Category> children = categoryMap.get(parent.getId());
        if (children != null && !children.isEmpty()) {
            parent.setChildren(children);
            for (Category child : children) {
                buildCategoryTree(child, categoryMap);
            }
        }
    }

    /**
     * parentId 归一：null 一律视作顶级分类（0）。
     * 存量数据可能为 NULL，避免分组、拆箱比较等场景 NPE。
     */
    private static Long normalizeParentId(Long parentId) {
        return parentId == null ? ROOT_PARENT_ID : parentId;
    }
}
