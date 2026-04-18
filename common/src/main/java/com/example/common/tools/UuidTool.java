package com.example.common.tools;

import java.util.UUID;

public class UuidTool {
    public static String getUUID32(){
        return UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }
}
