package com.gjx.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String target;
    private Long targetId;
    private String ip;
    private String detail;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
