package com.saywell.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import com.saywell.backend.entity.Branch;
import com.saywell.backend.entity.Customer;
import com.saywell.backend.entity.SalesStaff;

@MybatisTest
@DisplayName("MasterRepositoryテスト")
class MasterRepositoryTest {

    @Autowired
    private MasterRepository masterRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO branches (id, name, sort_no) VALUES (10, 'BranchA', 1)");
        jdbcTemplate.update(
                "INSERT INTO sales_staffs (id, branch_id, name) VALUES (100, 10, 'StaffA')");
        jdbcTemplate.update(
                "INSERT INTO customers (id, sales_staff_id, name) VALUES (1000, 100, 'CustomerA')");
    }

    @Test
    void testGetAllBranches() {
        List<Branch> list = masterRepository.getAllBranches();
        assertThat(list).isNotEmpty();
        assertThat(list.get(0).getName()).isEqualTo("BranchA");
    }

    @Test
    void testGetSalesStaffsByBranchId() {
        List<SalesStaff> list = masterRepository.getSalesStaffsByBranchId(10);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getName()).isEqualTo("StaffA");
    }

    @Test
    void testGetCustomersBySalesStaffId() {
        List<Customer> list = masterRepository.getCustomersBySalesStaffId(100);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getName()).isEqualTo("CustomerA");
    }
}
