package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjx.common.BusinessException;
import com.gjx.common.ResultCode;
import com.gjx.entity.Notification;
import com.gjx.enums.NotificationTypeEnum;
import com.gjx.mapper.NotificationMapper;
import com.gjx.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements INotificationService {

    /**
     * WebSocket 推送模板。
     * <p>
     * 原先是以 {@code required = false} 声明的可选字段注入（WebSocket 未启用时允许缺失）。
     * 为改成构造器注入又不丢失「可选」语义，这里用 {@link ObjectProvider} 承载：
     * 依赖缺失时 {@code getIfAvailable()} 返回 null，行为与原 {@code required = false} 完全一致，
     * 同时字段本身是 {@code final}、可被构造器注入，测试可直接构造。
     */
    private final ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider;

    /**
     * 推送一条通知。
     *
     * <p><b>为什么 WebSocket 推送要延到事务提交之后：</b>本方法几乎总是被订单/审核等
     * {@code @Transactional} 的 Service 方法调用。若在这里直接发送，消息会**先于事务提交**
     * 到达浏览器——一旦外层事务随后回滚（比如发货后又触发了库存校验失败），
     * 用户已经收到了「订单已发货」的实时提醒，而订单状态其实没有变。
     * 数据库那一行会随事务回滚消失，但发出去的消息收不回来，形成"幽灵通知"。
     * 因此这里在存在活动事务时把发送注册为 {@code afterCommit} 回调，
     * 让"落库"与"推送"的可见时机保持一致。
     *
     * <p>无事务上下文时（如定时任务直接调用）立即发送。
     */
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

        // 收件人无效时只落库不推送（系统通知的 userId=0 走这条路径）
        if (userId == null || userId <= 0) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendOverWebSocket(userId, notif);
                }
            });
        } else {
            sendOverWebSocket(userId, notif);
        }
    }

    /**
     * 按枚举类型推送，标题取该类型的默认标题。
     *
     * @see NotificationTypeEnum#getDefaultTitle()
     */
    @Override
    public void push(Long userId, NotificationTypeEnum type, String content, String link) {
        push(userId, type.name(), type.getDefaultTitle(), content, link);
    }

    /**
     * WebSocket 实时推送（模板可能缺失，{@code getIfAvailable} 返回 null 即跳过）。
     *
     * <p>推送失败只记警告、不上抛：通知已落库，用户下次进通知中心仍能看到，
     * 实时通道故障不应让已成功的下单/发货操作回滚。
     */
    private void sendOverWebSocket(Long userId, Notification notif) {
        SimpMessagingTemplate messagingTemplate = messagingTemplateProvider.getIfAvailable();
        if (messagingTemplate == null) {
            return;
        }
        try {
            messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", notif);
        } catch (Exception e) {
            log.warn("WebSocket 推送失败: userId={}, type={}, reason={}",
                    userId, notif.getType(), e.getMessage());
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
    public void markAsRead(Long notificationId, Long userId) {
        // A 类归属校验：把 userId 下沉进 WHERE，用影响行数判断成败。
        // 避免"先 getById 再在 Java 里比对 userId"之间存在的竞态窗口，
        // 同时天然阻止用户标记他人通知（IDOR）。
        boolean updated = update(new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, userId)
                .set(Notification::getIsRead, 1));
        if (!updated) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通知不存在");
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
