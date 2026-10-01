package com.gjx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gjx.dto.request.AddressRequest;
import com.gjx.entity.Address;

import java.util.List;

/**
 * 地址服务接口
 */
public interface IAddressService extends IService<Address> {
    /**
     * 根据用户ID获取地址列表
     * @param userId 用户ID
     * @return 地址列表
     */
    List<Address> listByUserId(Long userId);

    /**
     * 新增地址（必要时同时设为默认），保存与置默认在同一事务内完成。
     *
     * <p>之所以要有这个方法：原先由 Controller 先 {@code save} 再 {@code setDefault}，
     * 两次独立事务，第二步失败会留下“地址已存、默认互斥未生效”的脏数据。
     *
     * @param userId  当前登录用户ID（服务端覆写归属，忽略请求体中的 userId）
     * @param request 地址请求 DTO
     */
    void saveForUser(Long userId, AddressRequest request);

    /**
     * 更新地址（必要时同时设为默认），更新与置默认在同一事务内完成。
     *
     * <p>归属（userId）条件下沉到 UPDATE 的 WHERE 中，以影响行数判断是否存在/有权，
     * 避免“先查后判断”的越权竞态。
     *
     * @param userId  当前登录用户ID
     * @param id      地址ID
     * @param request 地址请求 DTO
     */
    void updateForUser(Long userId, Long id, AddressRequest request);

    /**
     * 删除地址
     * @param addressId 地址ID
     * @param userId 用户ID
     */
    void delete(Long addressId, Long userId);

    /**
     * 设置默认地址
     * @param addressId 地址ID
     * @param userId 用户ID
     */
    void setDefault(Long addressId, Long userId);
}
