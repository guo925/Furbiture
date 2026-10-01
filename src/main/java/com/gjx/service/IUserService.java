package com.gjx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gjx.entity.User;

/**
 * 用户服务接口
 */
public interface IUserService extends IService<User> {
    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户
     */
    User findByUsername(String username);

    /**
     * 获取用户列表（管理员端）
     * @param page 页码
     * @param size 每页大小
     * @param username 用户名（同时匹配用户名与手机号），可选
     * @param role 角色过滤，可选。用于「选商家」这类场景（传 MERCHANT 只出商家）
     * @return 用户列表
     */
    Page<User> adminListUsers(Integer page, Integer size, String username, String role);
}