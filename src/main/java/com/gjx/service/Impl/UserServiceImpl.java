package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjx.entity.User;
import com.gjx.mapper.UserMapper;
import com.gjx.service.IUserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Override
    public User findByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public Page<User> adminListUsers(Integer page, Integer size, String username, String role) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isEmpty()) {
            // 整块必须包在 and(...) 里。写成 username LIKE ? OR phone LIKE ? 时，
            // 与下面按角色过滤的 role = ? 组合会变成 A OR (B AND C)——
            // 只命中用户名的记录会**无视角色过滤**混进来（列商家时列出普通买家）。
            queryWrapper.and(wrapper -> wrapper
                    .like(User::getUsername, username)
                    .or()
                    .like(User::getPhone, username));
        }
        if (role != null && !role.isBlank()) {
            queryWrapper.eq(User::getRole, role);
        }
        queryWrapper.orderByDesc(User::getCreateTime);
        return page(new Page<>(page, size), queryWrapper);
    }
}
