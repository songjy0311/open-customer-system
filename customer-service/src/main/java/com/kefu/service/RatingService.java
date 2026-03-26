package com.kefu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kefu.entity.Rating;

public interface RatingService extends IService<Rating> {
    /**
     * 提交评价
     */
    Rating submitRating(Long conversationId, Integer rating, String comment);
}
