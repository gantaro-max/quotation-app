package com.saywell.backend.entity;

import lombok.Data;

@Data
public class User {
    private Integer id;
    private Integer branchId;
    private String name;
    private String password;
}
