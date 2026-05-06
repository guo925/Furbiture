package com.gjx.service;

import com.baomidou.mybatisplus.extension.service.IService;
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