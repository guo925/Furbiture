package com.gjx.controller.user;

import com.gjx.common.R;
import com.gjx.service.INotificationService;
import com.gjx.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "通知中心", description = "用户通知相关接口")
public class NotificationController {

    @Autowired
    private INotificationService notificationService;

    @Operation(summary = "获取通知列表")
    @GetMapping
    public R<?> list(@RequestParam(defaultValue = "1") Integer page,
                     @RequestParam(defaultValue = "20") Integer size,
                     HttpServletRequest request) {
        Long userId = AuthenticationUtil.getUserIdFromRequest(request);
        return R.ok(notificationService.listByUser(userId, page, size));
    }

    @Operation(summary = "获取未读数量")
    @GetMapping("/unread-count")
    public R<?> unreadCount(HttpServletRequest request) {
        Long userId = AuthenticationUtil.getUserIdFromRequest(request);
        return R.ok(notificationService.countUnread(userId));
    }

    @Operation(summary = "标记已读")
    @PutMapping("/{id}/read")
    public R<?> markRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return R.ok("已读");
    }

    @Operation(summary = "全部已读")
    @PutMapping("/read-all")
    public R<?> markAllRead(HttpServletRequest request) {
        Long userId = AuthenticationUtil.getUserIdFromRequest(request);
        notificationService.markAllAsRead(userId);
        return R.ok("全部已读");
    }
}
