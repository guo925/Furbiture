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
    public Page<User> adminListUsers(Integer page, Integer size, String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isEmpty()) {
            queryWrapper.like(User::getUsername, username)
                    .or()
                    .like(User::getPhone, username);
        }
        queryWrapper.orderByDesc(User::getCreateTime);
        return page(new Page<>(page, size), queryWrapper);
    }
}
