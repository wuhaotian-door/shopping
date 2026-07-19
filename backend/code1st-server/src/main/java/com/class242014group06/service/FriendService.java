package com.class242014group06.service;

import com.class242014group06.vo.FriendVO;

import java.util.List;

public interface FriendService {

    /** 发送好友申请 */
    void apply(Long fromUserId, Long toUserId);

    /** 接受好友申请 */
    void accept(Long relationId, Long currentUserId);

    /** 拒绝好友申请 */
    void reject(Long relationId, Long currentUserId);

    /** 获取好友列表 */
    List<FriendVO> listFriends(Long userId);

    /** 获取待处理的好友申请列表 */
    List<FriendVO> listPendingRequests(Long userId);
}
