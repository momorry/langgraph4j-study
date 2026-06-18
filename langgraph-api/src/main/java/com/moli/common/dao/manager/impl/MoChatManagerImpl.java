package com.moli.common.dao.manager.impl;

import com.moli.common.dao.entity.MoChat;
import com.moli.common.dao.mapper.MoChatMapper;
import com.moli.common.dao.manager.MoChatManager;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 会话 服务实现类
 * </p>
 *
 * @author system
 * @since 2026-06-18
 */
@Service
public class MoChatManagerImpl extends ServiceImpl<MoChatMapper, MoChat> implements MoChatManager {

}
