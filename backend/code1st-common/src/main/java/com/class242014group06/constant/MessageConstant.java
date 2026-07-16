package com.class242014group06.constant;

/**
 * 信息提示常量类
 */
public class MessageConstant {

    public static final String PASSWORD_ERROR = "密码错误";
    public static final String ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String ACCOUNT_LOCKED = "账号被锁定";
    public static final String  ALREADY_EXISTS = "账号已存在";
    public static final String UNKNOWN_ERROR = "未知错误";
    public static final String USER_NOT_LOGIN = "用户未登录";

    public static final String LOGIN_FAILED = "登录失败";
    public static final String UPLOAD_FAILED = "文件上传失败";

    public static final String PASSWORD_EDIT_FAILED = "密码修改失败";

    public static final String REGISTRATION_FAILED = "注册失败";
    public static final String ADMIN_CANNOT_REGISTER = "管理员无法通过注册页面注册";
    public static final String INVALID_ROLE = "无效的角色类型";
    public static final String ACCOUNT_PENDING = "账号正在审核中，请等待管理员审核通过";
    public static final String ACCOUNT_REJECTED = "账号审核未通过，请联系管理员";

    // 订单相关
    public static final String ORDER_NOT_FOUND = "订单不存在";
    public static final String ORDER_STATUS_ERROR = "订单状态异常";
    public static final String ORDER_SUBMIT_SUCCESS = "预约提交成功";

    // 通用操作
    public static final String NOT_FOUND = "不存在";
    public static final String NO_PERMISSION = "无权操作";

    // 时段相关
    public static final String SLOT_NOT_FOUND = "时段不存在";
    public static final String SLOT_NO_PERMISSION = "无权操作此时段";
    public static final String SLOT_TIME_CONFLICT = "时段冲突：此时段与已有时段时间重叠";
    public static final String PROJECT_DATE_OUT_OF_SLOT_RANGE = "项目日期超出了已设置的时段范围，建议先配置相应日期的时段";
    public static final String PROJECT_NO_SLOTS_CONFIGURED = "尚未配置任何时段，建议先设置可预约时段";

    // 项目相关
    public static final String PROJECT_NOT_FOUND = "项目不存在";
    public static final String PROJECT_NO_PERMISSION = "无权操作此项目";

    // 产品相关
    public static final String PRODUCT_NOT_FOUND = "产品不存在";
    public static final String PRODUCT_NO_PERMISSION = "无权操作此产品";
    public static final String PRODUCT_STOCK_INSUFFICIENT = "库存不足";
    public static final String PRODUCT_PERMISSION_DENIED = "无权操作此商品";
    public static final String PRODUCT_ORDER_NOT_FOUND = "商品订单不存在";
    public static final String PRODUCT_ORDER_PERMISSION_DENIED = "无权操作此订单";

    // 文章相关
    public static final String ARTICLE_NOT_FOUND = "文章不存在";
    public static final String ARTICLE_NO_PERMISSION = "无权操作此文章";

}
