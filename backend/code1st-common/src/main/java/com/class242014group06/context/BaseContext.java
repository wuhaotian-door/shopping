package com.class242014group06.context;

public class BaseContext {

    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /** 当前用户角色（USER / INHERITOR / ADMIN） */
    public static ThreadLocal<String> currentRole = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void setCurrentRole(String role) {
        currentRole.set(role);
    }

    public static String getCurrentRole() {
        return currentRole.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
        currentRole.remove();
    }

}
