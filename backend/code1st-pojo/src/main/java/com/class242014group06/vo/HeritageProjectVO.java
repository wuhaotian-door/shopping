package com.class242014group06.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 非遗项目返回对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeritageProjectVO implements Serializable {

    private Long id;

    /** 传承人ID */
    private Long inheritorId;

    /** 传承人姓名 */
    private String inheritorName;

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

    private LocalDateTime createTime;
}
