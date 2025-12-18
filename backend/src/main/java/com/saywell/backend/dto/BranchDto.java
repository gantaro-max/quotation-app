package com.saywell.backend.dto;

import lombok.Data;

@Data
public class BranchDto {
    private Integer id;
    private String name;
    private String address; // 印刷用
    private String phone; // 印刷用
}
