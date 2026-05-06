package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjx.common.BusinessException;
import com.gjx.entity.Address;
import com.gjx.mapper.AddressMapper;
import com.gjx.service.IAddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 地址服务实现类
 */
@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements IAddressService {

    @Override
    public List<Address> listByUserId(Long userId) {
        LambdaQueryWrapper<Address> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Address::getUserId, userId);
        queryWrapper.orderByDesc(Address::getIsDefault);
        queryWrapper.orderByDesc(Address::getCreateTime);
        return list(queryWrapper);
    }

    @Override
    @Transactional
    public void delete(Long addressId, Long userId) {
        // 验证地址是否存在且属于当前用户
        Address address = getById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        removeById(addressId);
    }

    @Override
    @Transactional
    public void setDefault(Long addressId, Long userId) {
        // 验证地址是否存在且属于当前用户
        Address address = getById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        
        // 将该用户的所有地址设为非默认
        LambdaQueryWrapper<Address> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(Address::getUserId, userId);
        Address nonDefault = new Address();
        nonDefault.setIsDefault(0);
        update(nonDefault, updateWrapper);
        
        // 将指定地址设为默认
        Address defaultAddress = new Address();
        defaultAddress.setId(addressId);
        defaultAddress.setIsDefault(1);
        updateById(defaultAddress);
    }
}