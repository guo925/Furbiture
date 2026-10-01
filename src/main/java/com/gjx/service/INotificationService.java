package com.gjx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gjx.entity.Notification;
import com.gjx.enums.NotificationTypeEnum;

public interface INotificationService extends IService<Notification> {

    /**
     * 推送通知并保存到数据库。
     *
     * <p>业务代码应优先用下面的枚举重载：{@code type} 若任由调用方拼字符串，
     * {@code notification.type} 这个索引列很快就会退化成自由文本。
     */
    void push(Long userId, String type, String title, String content, String link);

    /** 按枚举类型推送，标题取 {@link NotificationTypeEnum#getDefaultTitle()} */
    void push(Long userId, NotificationTypeEnum type, String content, String link);

    /** 获取用户通知分页 */
    Page<Notification> listByUser(Long userId, Integer page, Integer size);

    /** 标记已读（必须同时校验归属：notificationId 属于 userId） */
    void markAsRead(Long notificationId, Long userId);

    /** 全部已读 */
    void markAllAsRead(Long userId);

    /** 未读数量 */
    int countUnread(Long userId);
}
