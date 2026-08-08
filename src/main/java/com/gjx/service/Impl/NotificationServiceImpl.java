package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjx.entity.Notification;
import com.gjx.mapper.NotificationMapper;
import com.gjx.service.INotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements INotificationService {

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void push(Long userId, String type, String title, String content, String link) {
        Notification notif = new Notification();
        notif.setUserId(userId != null ? userId : 0L);
        notif.setType(type);
        notif.setTitle(title);
        notif.setContent(content);
        notif.setLink(link);
        notif.setIsRead(0);
        save(notif);

        // WebSocket 实时推送
        if (messagingTemplate != null && userId != null && userId > 0) {
            try {
                messagingTemplate.convertAndSendToUser(
                        userId.toString(), "/queue/notifications", notif);
            } catch (Exception e) {
                log.warn("WebSocket 推送失败: {}", e.getMessage());
            }
        }
    }

    @Override
    public Page<Notification> listByUser(Long userId, Integer page, Integer size) {
        return page(new Page<>(page, size),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreateTime));
    }

    @Override
    public void markAsRead(Long notificationId) {
        Notification n = getById(notificationId);
        if (n != null && n.getIsRead() == 0) {
            n.setIsRead(1);
            updateById(n);
        }
    }

    @Override
    public void markAllAsRead(Long userId) {
        Notification update = new Notification();
        update.setIsRead(1);
        baseMapper.update(update, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
    }

    @Override
    public int countUnread(Long userId) {
        return (int) count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
    }
}
