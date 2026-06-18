package com.moli.common.dao;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.Data;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 通用字段填充器
 *
 * @author moli
 * @since 2023/02/10
 */
@Component
public class CommonFieldFillHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Opt opt = currentOperator();
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);

        this.strictInsertFill(metaObject, "createBy", String.class, opt.getOpr());
        this.strictUpdateFill(metaObject, "updateBy", String.class, opt.getOpr());
        this.strictInsertFill(metaObject, "userId", Integer.class, opt.getOprId());

    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Opt opt = currentOperator();
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateBy", String.class, opt.getOpr());
    }

    private Opt currentOperator() {
        try {

            return new Opt(0, "admin");
        } catch (Exception e) {
        }
        return new Opt(0, "admin");
    }

    @Data
    public class Opt {
        private Integer oprId;

        private String opr;

        Opt() {
        }

        Opt(Integer oprId, String opr) {
            this.oprId = oprId;
            this.opr = opr;
        }
    }

}
