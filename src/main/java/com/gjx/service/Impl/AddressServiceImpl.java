package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjx.common.BusinessException;
import com.gjx.common.ResultCode;
import com.gjx.dto.request.AddressRequest;
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

    /**
     * 是否默认地址：1 默认
     */
    private static final int IS_DEFAULT = 1;

    /**
     * 是否默认地址：0 非默认
     */
    private static final int NOT_DEFAULT = 0;

    @Override
    public List<Address> listByUserId(Long userId) {
        LambdaQueryWrapper<Address> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Address::getUserId, userId);
        queryWrapper.orderByDesc(Address::getIsDefault);
        queryWrapper.orderByDesc(Address::getCreateTime);
        return list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long addressId, Long userId) {
        // 验证地址是否存在且属于当前用户
        Address address = getById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        removeById(addressId);
    }

    /**
     * 新增地址（含“设为默认”），保存与置默认共用一个事务。
     *
     * <p><b>为什么这两步必须在一个事务里：</b>
     * “保存地址”与“把该用户的其它地址清成非默认、再把本条置为默认”是两次独立的写操作。
     * 如果它们各自提交（原先 Controller 里 {@code save(...)} 与 {@code setDefault(...)}
     * 是两次独立事务），一旦置默认那一步失败，就会出现「地址已经落库、但默认地址互斥没有生效」
     * 的中间态——用户最终会留下两条默认地址。放进同一个事务后，任一步抛异常都会整体回滚，
     * 要么都成功、要么都不生效。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveForUser(Long userId, AddressRequest request) {
        Address address = toEntity(request);
        // 归属由服务端强制覆写，忽略请求体中可能夹带的 id / userId
        address.setUserId(userId);
        save(address);
        if (isDefaultRequested(request)) {
            applyDefault(address.getId(), userId);
        }
    }

    /**
     * 更新地址（含“设为默认”），更新与置默认共用一个事务。
     *
     * <p>归属（userId）条件下沉到 UPDATE 的 WHERE，用影响行数判断成败（A 类写法），
     * 而不是“先 getById 再在 Java 里比对 user_id 再 update”——后者在条件可变时存在竞态。
     * 置默认与更新同理必须在同一事务内，理由见 {@link #saveForUser}。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateForUser(Long userId, Long id, AddressRequest request) {
        boolean requestedDefault = isDefaultRequested(request);
        LambdaUpdateWrapper<Address> updateWrapper = new LambdaUpdateWrapper<Address>()
                .eq(Address::getId, id)
                .eq(Address::getUserId, userId)                 // 归属进 WHERE
                .set(Address::getName, request.getName())
                .set(Address::getPhone, request.getPhone())
                .set(Address::getProvince, request.getProvince())
                .set(Address::getCity, request.getCity())
                .set(Address::getDistrict, request.getDistrict())
                .set(Address::getDetailAddress, request.getDetailAddress())
                .set(Address::getIsDefault, requestedDefault ? IS_DEFAULT : NOT_DEFAULT);
        // 影响行数为 0 = 地址不存在或不属于当前用户，两种情况对外返回同一条消息
        if (!update(updateWrapper)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "地址不存在");
        }
        if (requestedDefault) {
            applyDefault(id, userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long addressId, Long userId) {
        // A 类：归属条件下沉进查询条件，用计数判断是否存在，避免先查后判断的越权窗口
        boolean owned = count(new LambdaQueryWrapper<Address>()
                .eq(Address::getId, addressId)
                .eq(Address::getUserId, userId)) > 0;
        if (!owned) {
            throw new BusinessException(ResultCode.NOT_FOUND, "地址不存在");
        }
        applyDefault(addressId, userId);
    }

    /**
     * 把指定地址置为默认，并保证同一用户下默认地址唯一（互斥）。
     *
     * <p>保持原有的“先全清 0、再置 1”语义；本次不引入数据库唯一约束（不在范围内）。
     * <p><b>不要在本方法上加 {@code @Transactional}</b>：它由同类内部的
     * {@link #saveForUser}/{@link #updateForUser}/{@link #setDefault} 调用（自调用不会走代理，
     * 注解也不会生效）。三步写操作由调用方已开启的事务统一兜住，正是我们想要的效果。
     */
    private void applyDefault(Long addressId, Long userId) {
        // 1) 先把该用户的所有地址清成非默认
        update(new LambdaUpdateWrapper<Address>()
                .eq(Address::getUserId, userId)
                .set(Address::getIsDefault, NOT_DEFAULT));
        // 2) 再把目标地址置为默认（归属条件再兜一层，防止越权改到他人地址）
        update(new LambdaUpdateWrapper<Address>()
                .eq(Address::getId, addressId)
                .eq(Address::getUserId, userId)
                .set(Address::getIsDefault, IS_DEFAULT));
    }

    /**
     * 请求是否要求把该地址设为默认
     */
    private boolean isDefaultRequested(AddressRequest request) {
        return request.getIsDefault() != null && request.getIsDefault() == IS_DEFAULT;
    }

    /**
     * DTO → 实体映射，只映射允许客户端提交的业务字段
     */
    private Address toEntity(AddressRequest request) {
        Address address = new Address();
        address.setName(request.getName());
        address.setPhone(request.getPhone());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setDetailAddress(request.getDetailAddress());
        address.setIsDefault(isDefaultRequested(request) ? IS_DEFAULT : NOT_DEFAULT);
        return address;
    }
}
