package com.quotationapp.backend.entity;

import lombok.Data;

@Data
public class Customer {
    private Integer id;
    private Integer branchId;
    private Integer salesStaffId;
    private String name;
}
