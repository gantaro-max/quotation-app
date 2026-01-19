package com.quotationapp.backend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.quotationapp.backend.entity.Branch;
import com.quotationapp.backend.entity.Customer;
import com.quotationapp.backend.entity.SalesStaff;
import com.quotationapp.backend.repository.MasterRepository;

@WebMvcTest(MasterController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("MasterControllerテスト")
class MasterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MasterRepository masterRepository;

    @Test
    @DisplayName("GET /api/master/branches: 営業所一覧")
    void testGetBranches() throws Exception {
        Branch b = new Branch();
        b.setId(10);
        b.setName("東京本社");
        when(masterRepository.getAllBranches()).thenReturn(List.of(b));

        mockMvc.perform(get("/api/master/branches")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("東京本社"));
    }

    @Test
    @DisplayName("GET /api/master/staffs: 担当者一覧")
    void testGetStaffs() throws Exception {
        SalesStaff s = new SalesStaff();
        s.setId(100);
        s.setName("営業太郎");
        when(masterRepository.getSalesStaffsByBranchId(10)).thenReturn(List.of(s));

        mockMvc.perform(get("/api/master/staffs").param("branchId", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("営業太郎"));
    }

    @Test
    @DisplayName("GET /api/master/customers: 顧客一覧")
    void testGetCustomers() throws Exception {
        Customer c = new Customer();
        c.setId(1000);
        c.setName("テスト病院");
        when(masterRepository.getCustomersBySalesStaffId(100)).thenReturn(List.of(c));

        mockMvc.perform(get("/api/master/customers").param("salesStaffId", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("テスト病院"));
    }
}
