package com.class242014group06.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 鍟嗗搧鏌ヨ杩斿洖瑙嗗浘瀵硅薄
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVO implements Serializable {

    private Long id;

    /** 鍟嗗搧鍚嶇О */
    private String name;

    /** 鍟嗗搧鎻忚堪 */
    private String description;

    /** 鍟嗗搧浠锋牸 */
    private BigDecimal price;

    /** 搴撳瓨鏁伴噺 */
    private Integer stock;

    /** 鍟嗗搧鍥剧墖URL */
    private String imageUrl;

    /** 3D模型URL（glb/gltf格式），可选 */
    private String modelUrl;

    /** 鍟嗗搧鍒嗙被 */
    private String category;

    /** 浼犳壙浜篒D */
    private Long inheritorId;

    /** 浼犳壙浜哄鍚?*/
    private String inheritorName;

    /** 鐘舵€侊細1-涓婃灦 0-涓嬫灦 */
    private Integer status;

    /** 鍒涘缓鏃堕棿 */
    private LocalDateTime createTime;

    /** 缁煎悎璇勫垎锛堝钩鍧囧€硷級 */
    private Double avgRating;

    /** 璇勪环鏁伴噺 */
    private Long reviewCount;
}
