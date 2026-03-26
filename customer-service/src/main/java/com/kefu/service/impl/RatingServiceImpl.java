package com.kefu.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kefu.entity.Rating;
import com.kefu.entity.Conversation;
import com.kefu.enums.ConversationStatus;
import com.kefu.mapper.RatingMapper;
import com.kefu.service.ConversationService;
import com.kefu.service.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RatingServiceImpl extends ServiceImpl<RatingMapper, Rating> implements RatingService {

    @Autowired
    private ConversationService conversationService;

    @Override
    public Rating submitRating(Long conversationId, Integer rating, String comment) {
        Rating ratingEntity = new Rating();
        ratingEntity.setConversationId(conversationId);
        ratingEntity.setRating(rating);
        ratingEntity.setComment(comment);
        ratingEntity.setCreatedAt(LocalDateTime.now());
        this.save(ratingEntity);

        // 更新会话状态为已评价
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setStatus(ConversationStatus.RATED);
        conversationService.updateById(conversation);

        return ratingEntity;
    }
}
