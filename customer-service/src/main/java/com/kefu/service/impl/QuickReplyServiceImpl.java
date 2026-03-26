package com.kefu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kefu.entity.QuickReply;
import com.kefu.mapper.QuickReplyMapper;
import com.kefu.service.QuickReplyService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuickReplyServiceImpl extends ServiceImpl<QuickReplyMapper, QuickReply> implements QuickReplyService {

    @Override
    public List<QuickReply> getQuickReplies() {
        return this.list(new LambdaQueryWrapper<QuickReply>()
                .orderByAsc(QuickReply::getSortOrder));
    }
}
