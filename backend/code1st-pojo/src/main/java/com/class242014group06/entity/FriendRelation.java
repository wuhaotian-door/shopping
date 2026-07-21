package com.class242014group06.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRelation implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String userRole;
    private Long friendId;
    private String friendRole;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
