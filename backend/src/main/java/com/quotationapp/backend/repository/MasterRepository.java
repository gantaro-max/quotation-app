package com.quotationapp.backend.repository;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.quotationapp.backend.entity.Branch;
import com.quotationapp.backend.entity.Customer;
import com.quotationapp.backend.entity.SalesStaff;

@Mapper
public interface MasterRepository {

    List<Branch> getAllBranches();

    List<SalesStaff> getSalesStaffsByBranchId(Integer branchId);

    List<Customer> getCustomersBySalesStaffId(Integer salesStaffId);

}
