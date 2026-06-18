create table mo_chat
(
    id          bigint      not null primary key comment '会话ID',
    app_id      bigint      not null comment '应用ID',
    chat_name   varchar(64) not null comment '会话名称',
    model_name  varchar(64) null comment '模型名称,仅记录首次对话时',
    create_by   varchar(32) comment '创建人',
    create_time datetime comment '创建时间',
    update_by   varchar(32) comment '创建人',
    update_time datetime comment '创建时间'
) comment '会话';

create table mo_message_history
(
    id           bigint      not null primary key comment 'ID',
    chat_id      bigint      not null comment '会话ID',
    app_id       bigint      not null comment '应用ID',
    model_name   varchar(64) null comment '模型名称',
    message_type varchar(16) not null comment '消息类型：user, ai, system',
    message      mediumtext comment '消息',
    think_message  mediumtext comment '思考消息',
    sort         bigint      not null comment '排序，雪花算法',
    create_by    varchar(32) comment '创建人',
    create_time  datetime comment '创建时间',
    update_by    varchar(32) comment '创建人',
    update_time  datetime comment '创建时间'
) comment '消息历史';