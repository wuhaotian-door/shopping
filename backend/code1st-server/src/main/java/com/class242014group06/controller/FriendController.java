package com.class242014group06.controller;

import com.class242014group06.context.BaseContext;
import com.class242014group06.dto.FriendApplyDTO;
import com.class242014group06.result.Result;
import com.class242014group06.service.FriendService;
import com.class242014group06.vo.FriendVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/friend")
@Slf4j
@Api(tags = "好友管理接口")
public class FriendController {

    @Autowired
    private FriendService friendService;

    @PostMapping("/apply")
    @ApiOperation("发送好友申请")
    public Result<String> apply(@RequestBody FriendApplyDTO dto) {
        Long userId = BaseContext.getCurrentId();
        String userRole = BaseContext.getCurrentRole();
        log.info("发送好友申请：from={}({}), to={}({})", userId, userRole, dto.getTargetUserId(), dto.getTargetRole());
        friendService.apply(userId, userRole, dto.getTargetUserId(), dto.getTargetRole());
        return Result.success("好友申请已发送");
    }

    @PutMapping("/{relationId}/accept")
    @ApiOperation("接受好友申请")
    public Result<String> accept(@PathVariable Long relationId) {
        Long userId = BaseContext.getCurrentId();
        log.info("接受好友申请：relationId={}, userId={}", relationId, userId);
        friendService.accept(relationId, userId);
        return Result.success("已接受好友申请");
    }

    @PutMapping("/{relationId}/reject")
    @ApiOperation("拒绝好友申请")
    public Result<String> reject(@PathVariable Long relationId) {
        Long userId = BaseContext.getCurrentId();
        log.info("拒绝好友申请：relationId={}, userId={}", relationId, userId);
        friendService.reject(relationId, userId);
        return Result.success("已拒绝好友申请");
    }

    @GetMapping("/list")
    @ApiOperation("获取好友列表")
    public Result<List<FriendVO>> listFriends() {
        Long userId = BaseContext.getCurrentId();
        log.info("获取好友列表：userId={}", userId);
        return Result.success(friendService.listFriends(userId));
    }

    @GetMapping("/pending")
    @ApiOperation("获取待处理的好友申请")
    public Result<List<FriendVO>> listPending() {
        Long userId = BaseContext.getCurrentId();
        log.info("获取待处理好友申请：userId={}", userId);
        return Result.success(friendService.listPendingRequests(userId));
    }

    @GetMapping("/my-requests")
    @ApiOperation("获取我发出的好友申请")
    public Result<List<FriendVO>> listMyRequests() {
        Long userId = BaseContext.getCurrentId();
        log.info("获取我发出的好友申请：userId={}", userId);
        return Result.success(friendService.listMyRequests(userId));
    }
}
