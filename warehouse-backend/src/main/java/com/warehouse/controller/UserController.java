package com.warehouse.controller;

import com.warehouse.model.dto.Result;
import com.warehouse.model.entity.User;
import com.warehouse.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/user/list")
    public Result<List<User>> list() {
        return Result.ok(userService.list());
    }

    @PostMapping("/user/save")
    public Result<User> save(@RequestBody User user) {
        return Result.ok(userService.save(user));
    }

    @PostMapping("/user/delete")
    public Result<?> delete(@RequestBody Map<String, Long> body) {
        userService.delete(body.get("id"));
        return Result.ok(null);
    }
}
