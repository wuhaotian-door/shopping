package com.class242014group06.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 非遗项目实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeritageProject implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 传承人ID */
    private Long inheritorId;

    /** 项目标题 */
    private String title;

    /** 项目分类 */
    private String category;

    /** 项目描述 */
    private String description;

    /** 封面图 */
    private String coverUrl;

    /** 活动地点 */
    private String location;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 最大参与人数 */
    private Integer maxPeople;

    /** 当前报名人数 */
    private Integer currentPeople;

    /** 状态：0-下架 1-上架 */
    private Integer status;

    /** 项目类型：0-公益(非盈利), 1-非公益(盈利) */
    private Integer projectType;

    /** 价格（非公益项目使用） */
    private BigDecimal price;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;
}
