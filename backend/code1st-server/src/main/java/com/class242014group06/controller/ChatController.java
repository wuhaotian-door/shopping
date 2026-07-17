package com.class242014group06.controller;

import com.class242014group06.context.BaseContext;
import com.class242014group06.dto.ChatSendDTO;
import com.class242014group06.result.Result;
import com.class242014group06.service.ChatService;
import com.class242014group06.vo.ChatMessageVO;
import com.class242014group06.vo.ConversationVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/chat")
@Slf4j
@Api(tags = "聊天消息接口")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/conversations")
    @ApiOperation("获取会话列表")
    public Result<List<ConversationVO>> listConversations() {
        Long userId = BaseContext.getCurrentId();
        String role = BaseContext.getCurrentRole();
        log.info("获取会话列表：userId={}, role={}", userId, role);
        return Result.success(chatService.listConversations(userId, role));
    }

    @GetMapping("/messages/{conversationId}")
    @ApiOperation("获取会话消息")
    public Result<List<ChatMessageVO>> getMessages(@PathVariable Long conversationId) {
        Long userId = BaseContext.getCurrentId();
        String role = BaseContext.getCurrentRole();
        log.info("获取消息：conversationId={}, userId={}", conversationId, userId);
        return Result.success(chatService.getMessages(conversationId, userId, role));
    }

    @PostMapping("/send")
    @ApiOperation("发送消息")
    public Result<ChatMessageVO> send(@RequestBody ChatSendDTO dto) {
        Long userId = BaseContext.getCurrentId();
        String role = BaseContext.getCurrentRole();
        log.info("发送消息：senderId={}, role={}, dto={}", userId, role, dto);
        return Result.success(chatService.sendMessage(userId, role, dto));
    }

    @GetMapping("/search")
    @ApiOperation("按用户名搜索用户")
    public Result<Object> searchUser(@RequestParam String username) {
        Long userId = BaseContext.getCurrentId();
        String role = BaseContext.getCurrentRole();
        log.info("搜索用户：searcherId={}, username={}", userId, username);
        Object user = chatService.searchUserByUsername(userId, role, username);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }
}
