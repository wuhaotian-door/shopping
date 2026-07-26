package com.class242014group06.service.impl;

import com.class242014group06.entity.BrowseHistory;
import com.class242014group06.mapper.BrowseHistoryMapper;
import com.class242014group06.service.BrowseHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BrowseHistoryServiceImpl implements BrowseHistoryService {

    private final BrowseHistoryMapper browseHistoryMapper;

    @Override
    public void record(Long userId, String userRole, String targetType, Long targetId,
                       String targetTitle, String targetCover, String targetPath) {
        BrowseHistory h = BrowseHistory.builder()
                .userId(userId)
                .userRole(userRole)
                .targetType(targetType)
                .targetId(targetId)
                .targetTitle(targetTitle)
                .targetCover(targetCover)
                .targetPath(targetPath)
                .createTime(LocalDateTime.now())
                .build();
        browseHistoryMapper.insert(h);
        log.debug("浏览记录: uid={}_{} → {}/{}", userRole, userId, targetType, targetId);
    }

    @Override
    public List<BrowseHistory> listByUser(Long userId, String userRole) {
        return browseHistoryMapper.listByUser(userId, userRole);
    }

    @Override
    public void deleteById(Long id) {
        browseHistoryMapper.deleteById(id);
    }

    @Override
    public void deleteAllByUser(Long userId, String userRole) {
        int count = browseHistoryMapper.deleteAllByUser(userId, userRole);
        log.info("清空浏览记录: uid={}_{}, {}条", userRole, userId, count);
    }
}
