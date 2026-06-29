package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.warehouse.mapper.UserMapper;
import com.warehouse.model.entity.User;
import com.warehouse.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public User login(String username, String password) {
        User user = userMapper.selectOne(
                new QueryWrapper<User>().eq("username", username));
        if (user == null) {
            return null;
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return null;
        }
        return user;
    }

    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    public List<User> list() {
        List<User> users = userMapper.selectList(
                new QueryWrapper<User>().orderByAsc("username"));
        // 清除密码字段，不暴露给前端
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    @Transactional
    public User save(User user) {
        if (user.getId() != null) {
            // 更新用户：密码非空则重新加密，密码为空保留原密码
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            } else {
                User existing = userMapper.selectById(user.getId());
                if (existing != null) {
                    user.setPassword(existing.getPassword());
                }
            }
            userMapper.updateById(user);
        } else {
            // 新建用户：密码必填
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                throw new RuntimeException("新建用户密码不能为空");
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userMapper.insert(user);
        }
        // 返回前清除密码
        user.setPassword(null);
        return user;
    }

    @Transactional
    public void delete(Long id) {
        // 不允许删除自己
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId != null && currentUserId.equals(id)) {
            throw new RuntimeException("不能删除自己的账号");
        }
        userMapper.deleteById(id);
    }
}
