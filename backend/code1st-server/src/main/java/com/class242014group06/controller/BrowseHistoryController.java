package com.class242014group06.controller;

import com.class242014group06.context.BaseContext;
import com.class242014group06.result.Result;
import com.class242014group06.service.BrowseHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户浏览记录 — 按 user_id + user_role 隔离
 */
@RestController
@RequestMapping("/admin/user")
@Slf4j
@RequiredArgsConstructor
public class BrowseHistoryController {

    private final BrowseHistoryService browseHistoryService;

    @PostMapping("/browse-history")
    public Result<?> record(@RequestBody Map<String, Object> body) {
        Long userId = BaseContext.getCurrentId();
        String userRole = BaseContext.getCurrentRole();
        String targetType = (String) body.get("targetType");
        Long targetId = body.get("targetId") instanceof Number n ? n.longValue() : null;
        String targetTitle = (String) body.get("targetTitle");
        String targetCover = (String) body.get("targetCover");
        String targetPath = (String) body.get("targetPath");

        browseHistoryService.record(userId, userRole, targetType, targetId,
                targetTitle, targetCover, targetPath);
        return Result.success("ok");
    }

    @GetMapping("/browse-history")
    public Result<?> list() {
        Long userId = BaseContext.getCurrentId();
        String userRole = BaseContext.getCurrentRole();
        return Result.success(browseHistoryService.listByUser(userId, userRole));
    }

    @DeleteMapping("/browse-history/{id}")
    public Result<?> delete(@PathVariable Long id) {
        browseHistoryService.deleteById(id);
        return Result.success("ok");
    }

    @DeleteMapping("/browse-history")
    public Result<?> clearAll() {
        Long userId = BaseContext.getCurrentId();
        String userRole = BaseContext.getCurrentRole();
        browseHistoryService.deleteAllByUser(userId, userRole);
        return Result.success("ok");
    }
}
