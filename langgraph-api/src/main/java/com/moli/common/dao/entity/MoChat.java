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
 * 会话
 * </p>
 *
 * @author system
 * @since 2026-06-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MoChat implements Serializable {

    /**
     * 会话ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 应用ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long appId;

    /**
     * 会话名称
     */
    private String chatName;

    /**
     * 模型名称,仅记录首次对话时
     */
    private String modelName;

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
