package com.kefu.controller;

import com.kefu.dto.Result;
import com.kefu.entity.Rating;
import com.kefu.service.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rating")
public class RatingController {

    @Autowired
    private RatingService ratingService;

    @PostMapping
    public Result<?> submitRating(@RequestParam Long conversationId,
                                   @RequestParam Integer rating,
                                   @RequestParam(required = false) String comment) {
        Rating result = ratingService.submitRating(conversationId, rating, comment);
        return Result.success(result);
    }
}
