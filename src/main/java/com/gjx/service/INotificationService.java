package com.gjx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gjx.entity.Notification;

public interface INotificationService extends IService<Notification> {

    /** 推送通知并保存到数据库 */
    void push(Long userId, String type, String title, String content, String link);

    /** 获取用户通知分页 */
    Page<Notification> listByUser(Long userId, Integer page, Integer size);

    /** 标记已读 */
    void markAsRead(Long notificationId);

    /** 全部已读 */
    void markAllAsRead(Long userId);

    /** 未读数量 */
    int countUnread(Long userId);
}
