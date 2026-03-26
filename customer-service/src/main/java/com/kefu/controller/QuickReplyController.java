package com.kefu.controller;

import com.kefu.dto.Result;
import com.kefu.entity.QuickReply;
import com.kefu.service.QuickReplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quick-reply")
public class QuickReplyController {

    @Autowired
    private QuickReplyService quickReplyService;

    @GetMapping
    public Result<List<QuickReply>> getQuickReplies() {
        List<QuickReply> list = quickReplyService.getQuickReplies();
        return Result.success(list);
    }

    @PostMapping
    public Result<?> addQuickReply(@RequestBody QuickReply quickReply) {
        quickReplyService.save(quickReply);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<?> updateQuickReply(@PathVariable Long id, @RequestBody QuickReply quickReply) {
        quickReply.setId(id);
        quickReplyService.updateById(quickReply);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteQuickReply(@PathVariable Long id) {
        quickReplyService.removeById(id);
        return Result.success();
    }
}
