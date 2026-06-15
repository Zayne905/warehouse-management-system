package com.warehouse.security;

import com.warehouse.model.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static User getCurrentUser() {
        return (User) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static boolean isAdmin() {
        User user = getCurrentUser();
        return user != null && "admin".equals(user.getRole());
    }
}
