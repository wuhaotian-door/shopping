package com.class242014group06.service.impl;

import com.class242014group06.entity.FriendRelation;
import com.class242014group06.entity.User;
import com.class242014group06.mapper.FriendRelationMapper;
import com.class242014group06.mapper.UserMapper;
import com.class242014group06.service.FriendService;
import com.class242014group06.vo.FriendVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FriendServiceImpl implements FriendService {

    @Autowired
    private FriendRelationMapper friendRelationMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public void apply(Long fromUserId, Long toUserId) {
        if (fromUserId.equals(toUserId)) {
            throw new RuntimeException("不能添加自己为好友");
        }
        // 检查是否已存在关系
        FriendRelation existing = friendRelationMapper.findRelation(fromUserId, toUserId);
        if (existing != null) {
            if (existing.getStatus() == 1) {
                throw new RuntimeException("已经是好友了");
            }
            if (existing.getStatus() == 0) {
                throw new RuntimeException("已发送过好友申请，请等待对方处理");
            }
            // 之前被拒绝过，重新申请
            existing.setStatus(0);
            friendRelationMapper.updateStatus(existing.getId(), 0);
            return;
        }

        FriendRelation relation = FriendRelation.builder()
                .userId(fromUserId).friendId(toUserId).status(0).build();
        friendRelationMapper.insert(relation);
    }

    @Override
    @Transactional
    public void accept(Long relationId, Long currentUserId) {
        FriendRelation relation = friendRelationMapper.getById(relationId);
        if (relation == null) {
            throw new RuntimeException("好友申请不存在");
        }
        if (!relation.getFriendId().equals(currentUserId)) {
            throw new RuntimeException("无权处理此申请");
        }
        if (relation.getStatus() != 0) {
            throw new RuntimeException("该申请已处理");
        }
        friendRelationMapper.updateStatus(relationId, 1);
    }

    @Override
    @Transactional
    public void reject(Long relationId, Long currentUserId) {
        FriendRelation relation = friendRelationMapper.getById(relationId);
        if (relation == null) {
            throw new RuntimeException("好友申请不存在");
        }
        if (!relation.getFriendId().equals(currentUserId)) {
            throw new RuntimeException("无权处理此申请");
        }
        friendRelationMapper.updateStatus(relationId, 2);
    }

    @Override
    public List<FriendVO> listFriends(Long userId) {
        List<FriendRelation> relations = friendRelationMapper.listFriends(userId);
        List<FriendVO> result = new ArrayList<>();
        for (FriendRelation r : relations) {
            Long friendId = r.getUserId().equals(userId) ? r.getFriendId() : r.getUserId();
            User friend = userMapper.getById(friendId);
            if (friend != null) {
                result.add(FriendVO.builder()
                        .relationId(r.getId())
                        .userId(friend.getId()).userName(friend.getUsername())
                        .name(friend.getName()).role("USER")
                        .status(r.getStatus()).createdAt(r.getCreatedAt()).build());
            }
        }
        return result;
    }

    @Override
    public List<FriendVO> listPendingRequests(Long userId) {
        List<FriendRelation> relations = friendRelationMapper.listPendingRequests(userId);
        List<FriendVO> result = new ArrayList<>();
        for (FriendRelation r : relations) {
            User fromUser = userMapper.getById(r.getUserId());
            if (fromUser != null) {
                result.add(FriendVO.builder()
                        .relationId(r.getId())
                        .userId(fromUser.getId()).userName(fromUser.getUsername())
                        .name(fromUser.getName()).role("USER")
                        .status(r.getStatus()).createdAt(r.getCreatedAt()).build());
            }
        }
        return result;
    }
}
