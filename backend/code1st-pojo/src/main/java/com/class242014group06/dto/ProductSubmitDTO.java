package com.class242014group06.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 鍟嗗搧鏂板/淇敼璇锋眰浣?
 */
@Data
public class ProductSubmitDTO implements Serializable {

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
}
