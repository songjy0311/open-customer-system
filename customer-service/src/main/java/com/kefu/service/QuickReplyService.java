package com.kefu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kefu.entity.QuickReply;

import java.util.List;

public interface QuickReplyService extends IService<QuickReply> {
    List<QuickReply> getQuickReplies();
}
