package com.class242014group06.service.impl;

import com.class242014group06.entity.Admin;
import com.class242014group06.entity.FriendRelation;
import com.class242014group06.entity.Inheritor;
import com.class242014group06.entity.User;
import com.class242014group06.mapper.AdminMapper;
import com.class242014group06.mapper.FriendRelationMapper;
import com.class242014group06.mapper.InheritorMapper;
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
    @Autowired
    private InheritorMapper inheritorMapper;
    @Autowired
    private AdminMapper adminMapper;

    /** 按角色+ID 精确查找用户 */
    private FriendVO resolveUser(Long id, String role) {
        if ("INHERITOR".equals(role)) {
            Inheritor inh = inheritorMapper.getById(id);
            if (inh != null) return FriendVO.builder().userId(inh.getId()).userName(inh.getUsername()).name(inh.getName()).role("INHERITOR").build();
        } else if ("ADMIN".equals(role)) {
            Admin admin = adminMapper.getById(id);
            if (admin != null) return FriendVO.builder().userId(admin.getId()).userName(admin.getUsername()).name(admin.getName()).role("ADMIN").build();
        } else {
            User user = userMapper.getById(id);
            if (user != null) return FriendVO.builder().userId(user.getId()).userName(user.getUsername()).name(user.getName()).role("USER").build();
        }
        return null;
    }

    @Override
    @Transactional
    public void apply(Long fromUserId, String fromRole, Long toUserId, String toRole) {
        if (fromUserId.equals(toUserId) && fromRole.equals(toRole)) {
            throw new RuntimeException("不能添加自己为好友");
        }
        FriendRelation existing = friendRelationMapper.findRelation(fromUserId, toUserId);
        if (existing != null) {
            if (existing.getStatus() == 1) throw new RuntimeException("已经是好友了");
            if (existing.getStatus() == 0) throw new RuntimeException("已发送过好友申请，请等待对方处理");
            existing.setStatus(0);
            friendRelationMapper.updateStatus(existing.getId(), 0);
            return;
        }
        FriendRelation relation = FriendRelation.builder()
                .userId(fromUserId).userRole(fromRole)
                .friendId(toUserId).friendRole(toRole)
                .status(0).build();
        friendRelationMapper.insert(relation);
    }

    @Override
    @Transactional
    public void accept(Long relationId, Long currentUserId) {
        FriendRelation relation = friendRelationMapper.getById(relationId);
        if (relation == null) throw new RuntimeException("好友申请不存在");
        if (!relation.getFriendId().equals(currentUserId)) throw new RuntimeException("无权处理此申请");
        if (relation.getStatus() != 0) throw new RuntimeException("该申请已处理");
        friendRelationMapper.updateStatus(relationId, 1);
    }

    @Override
    @Transactional
    public void reject(Long relationId, Long currentUserId) {
        FriendRelation relation = friendRelationMapper.getById(relationId);
        if (relation == null) throw new RuntimeException("好友申请不存在");
        if (!relation.getFriendId().equals(currentUserId)) throw new RuntimeException("无权处理此申请");
        friendRelationMapper.updateStatus(relationId, 2);
    }

    @Override
    public List<FriendVO> listFriends(Long userId) {
        List<FriendRelation> relations = friendRelationMapper.listFriends(userId);
        List<FriendVO> result = new ArrayList<>();
        for (FriendRelation r : relations) {
            Long friendId = r.getUserId().equals(userId) ? r.getFriendId() : r.getUserId();
            String role = r.getUserId().equals(userId) ? r.getFriendRole() : r.getUserRole();
            FriendVO vo = resolveUser(friendId, role);
            if (vo != null) {
                vo.setRelationId(r.getId());
                vo.setStatus(r.getStatus());
                vo.setCreatedAt(r.getCreatedAt());
                result.add(vo);
            }
        }
        return result;
    }

    @Override
    public List<FriendVO> listPendingRequests(Long userId) {
        List<FriendRelation> relations = friendRelationMapper.listPendingRequests(userId);
        List<FriendVO> result = new ArrayList<>();
        for (FriendRelation r : relations) {
            FriendVO vo = resolveUser(r.getUserId(), r.getUserRole());
            if (vo != null) {
                vo.setRelationId(r.getId());
                vo.setStatus(r.getStatus());
                vo.setCreatedAt(r.getCreatedAt());
                result.add(vo);
            }
        }
        return result;
    }

    @Override
    public List<FriendVO> listMyRequests(Long userId) {
        List<FriendRelation> relations = friendRelationMapper.listMyRequests(userId);
        List<FriendVO> result = new ArrayList<>();
        for (FriendRelation r : relations) {
            FriendVO vo = resolveUser(r.getFriendId(), r.getFriendRole());
            if (vo != null) {
                vo.setRelationId(r.getId());
                vo.setStatus(r.getStatus());
                vo.setCreatedAt(r.getCreatedAt());
                result.add(vo);
            }
        }
        return result;
    }
}
