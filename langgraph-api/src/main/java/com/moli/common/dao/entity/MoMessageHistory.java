package com.moli.common.dao.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 消息历史
 * </p>
 *
 * @author system
 * @since 2026-06-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MoMessageHistory implements Serializable {

    /**
     * ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 会话ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long chatId;

    /**
     * 应用ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long appId;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 消息类型：user, ai, system
     */
    private String messageType;

    /**
     * 消息
     */
    private String message;

    /**
     * 思考消息
     */
    private String thinkMessage;

    /**
     * 排序，雪花算法
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long sort;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


}
