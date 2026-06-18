package com.moli.common.dao.manager.impl;

import com.moli.common.dao.entity.MoMessageHistory;
import com.moli.common.dao.mapper.MoMessageHistoryMapper;
import com.moli.common.dao.manager.MoMessageHistoryManager;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 消息历史 服务实现类
 * </p>
 *
 * @author system
 * @since 2026-06-18
 */
@Service
public class MoMessageHistoryManagerImpl extends ServiceImpl<MoMessageHistoryMapper, MoMessageHistory> implements MoMessageHistoryManager {

}
