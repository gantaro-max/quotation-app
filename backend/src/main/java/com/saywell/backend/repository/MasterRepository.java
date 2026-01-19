package com.saywell.backend.repository;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.saywell.backend.entity.Branch;
import com.saywell.backend.entity.Customer;
import com.saywell.backend.entity.SalesStaff;

@Mapper
public interface MasterRepository {

    List<Branch> getAllBranches();

    List<SalesStaff> getSalesStaffsByBranchId(Integer branchId);

    List<Customer> getCustomersBySalesStaffId(Integer salesStaffId);

}
