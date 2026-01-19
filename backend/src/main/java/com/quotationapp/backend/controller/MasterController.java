package com.quotationapp.backend.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.quotationapp.backend.entity.Branch;
import com.quotationapp.backend.entity.Customer;
import com.quotationapp.backend.entity.SalesStaff;
import com.quotationapp.backend.repository.MasterRepository;

@RestController
@RequestMapping("/api/master")
public class MasterController {

    @Autowired
    private MasterRepository masterRepository;

    // 営業所一覧を取得
    @GetMapping("/branches")
    public List<Branch> getBranches() {
        return masterRepository.getAllBranches();
    }

    // 特定の営業所に紐づく担当者を取得
    @GetMapping("/staffs")
    public List<SalesStaff> getStaffs(@RequestParam("branchId") Integer branchId) {
        return masterRepository.getSalesStaffsByBranchId(branchId);
    }

    // 特定の営業所に紐づく顧客を取得
    @GetMapping("/customers")
    public List<Customer> getCustomers(@RequestParam("salesStaffId") Integer salesStaffId) {
        return masterRepository.getCustomersBySalesStaffId(salesStaffId);
    }
}
