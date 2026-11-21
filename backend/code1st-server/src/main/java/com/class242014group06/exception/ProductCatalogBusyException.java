package com.class242014group06.exception;

public class ProductCatalogBusyException extends BaseException {
    public ProductCatalogBusyException() {
        super("商城繁忙，请稍后重试");
    }
}
