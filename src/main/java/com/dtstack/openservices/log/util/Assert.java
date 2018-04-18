package com.dtstack.openservices.log.util;

public class Assert {

    public static void assertTrue(boolean assertFalse){
        if(!assertFalse) throw new IllegalArgumentException("非法参数");
    }

    public static void assertTrue(boolean assertFalse,String message){
        if(!assertFalse) throw new IllegalArgumentException(message);
    }

}
