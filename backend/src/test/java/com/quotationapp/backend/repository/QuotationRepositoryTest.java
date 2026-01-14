package com.quotationapp.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import com.quotationapp.backend.dto.QuotationDto;
import com.quotationapp.backend.entity.Quotation;
import com.quotationapp.backend.entity.QuotationItem;

@MybatisTest
@DisplayName("QuotationRepositoryのテスト")
class QuotationRepositoryTest {

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // マスタデータの準備
        jdbcTemplate.update(
                "INSERT INTO users (id, name, email, password_hash) VALUES (1, 'User1', 'u1@ex.com', 'pw')");
        jdbcTemplate.update("INSERT INTO branches (id, name) VALUES (10, 'Branch1')");
        jdbcTemplate.update("INSERT INTO sales_staffs (id, name) VALUES (100, 'Staff1')");
        jdbcTemplate.update("INSERT INTO customers (id, name) VALUES (1000, 'Customer1')");
    }

    @Test
    @DisplayName("insert & findById: 保存と取得ができること")
    void testInsertAndFindById() {
        Quotation q = new Quotation();
        q.setEstimateNo("Q-TEST-001");
        q.setVersion(1);
        q.setCustomerName("テスト顧客");
        q.setCreatedByUserId(1);
        q.setCreatedAt(LocalDateTime.now());
        q.setUpdatedAt(LocalDateTime.now());

        // 保存
        quotationRepository.insert(q);

        // IDが自動採番されているか
        assertThat(q.getId()).isNotNull();

        // 取得
        Quotation fetched = quotationRepository.findById(q.getId());
        assertThat(fetched).isNotNull();
        assertThat(fetched.getEstimateNo()).isEqualTo("Q-TEST-001");
    }

    @Test
    @DisplayName("findDtoById: マスタ情報と明細がJOINされて取得できること")
    void testFindDtoById() {
        // データ準備: ヘッダー
        jdbcTemplate
                .update("""
                            INSERT INTO quotations (id, estimate_no, created_by_user_id, sales_branch_id, customer_id, customer_name, created_at)
                            VALUES (1, 'Q-DTO-TEST', 1, 10, 1000, 'Customer1', NOW())
                        """);
        // データ準備: 明細
        jdbcTemplate
                .update("""
                            INSERT INTO quotation_items (quotation_id, row_order, item_name, unit_price, quantity)
                            VALUES (1, 1, '商品A', 1000, 2)
                        """);

        // 実行
        Optional<QuotationDto> result = quotationRepository.findDtoById(1L);

        // 検証
        assertThat(result).isPresent();
        QuotationDto dto = result.get();

        // 基本情報
        assertThat(dto.getEstimateNo()).isEqualTo("Q-DTO-TEST");

        // JOIN情報 (ここが重要)
        assertThat(dto.getCreatedByUser()).isNotNull();
        assertThat(dto.getCreatedByUser().getName()).isEqualTo("User1"); // User結合確認

        assertThat(dto.getSalesBranch()).isNotNull();
        assertThat(dto.getSalesBranch().getName()).isEqualTo("Branch1"); // Branch結合確認

        // 明細情報
        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getItems().get(0).getItemName()).isEqualTo("商品A");
    }

    @Test
    @DisplayName("search: 条件検索ができること")
    void testSearch() {
        jdbcTemplate.update(
                "INSERT INTO quotations (estimate_no, customer_name, created_at) VALUES ('Q1', 'Alpha Corp', NOW())");
        jdbcTemplate.update(
                "INSERT INTO quotations (estimate_no, customer_name, created_at) VALUES ('Q2', 'Beta Inc', NOW())");

        // 部分一致検索
        // 引数を5つに修正: customerName, customerCode, salesBranchName, projectName, estimateNo
        List<QuotationDto> results = quotationRepository.search("Alpha", null, null, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCustomerName()).isEqualTo("Alpha Corp");
    }

    @Test
    @DisplayName("明細の一括保存と削除")
    void testItemsOperations() {
        Long quotationId = 99L;

        QuotationItem item1 = new QuotationItem();
        item1.setQuotationId(quotationId);
        item1.setRowOrder(1);
        item1.setItemName("Item1");

        QuotationItem item2 = new QuotationItem();
        item2.setQuotationId(quotationId);
        item2.setRowOrder(2);
        item2.setItemName("Item2");

        // 一括保存
        quotationRepository.insertItems(List.of(item1, item2));

        // 確認
        List<QuotationItem> items = quotationRepository.findItemsByQuotationId(quotationId);
        assertThat(items).hasSize(2);

        // 削除
        quotationRepository.deleteItemsByQuotationId(quotationId);

        // 確認
        items = quotationRepository.findItemsByQuotationId(quotationId);
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("値引き額(discountAmount)が正しく保存・取得できること (0とNULLの区別)")
    void testDiscountAmount() {
        // ケース1: 値引きあり
        Quotation q1 = new Quotation();
        q1.setEstimateNo("Q-DISCOUNT-1");
        q1.setCustomerName("C1");
        q1.setCreatedByUserId(1);
        q1.setDiscountAmount(new BigDecimal("5000.00"));
        quotationRepository.insert(q1);

        Quotation fetched1 = quotationRepository.findById(q1.getId());
        // BigDecimal比較は isEqualByComparingTo が安全
        assertThat(fetched1.getDiscountAmount()).isEqualByComparingTo("5000");

        // ケース2: 値引きなし（NULL）
        Quotation q2 = new Quotation();
        q2.setEstimateNo("Q-DISCOUNT-2");
        q2.setCustomerName("C2");
        q2.setCreatedByUserId(1);
        q2.setDiscountAmount(null); // NULL保存
        quotationRepository.insert(q2);

        Quotation fetched2 = quotationRepository.findById(q2.getId());
        assertThat(fetched2.getDiscountAmount()).isNull(); // NULLであることを確認
    }

    @Test
    @DisplayName("update: 更新ができること")
    void testUpdate() {
        // 1. 新規作成
        Quotation q = new Quotation();
        q.setEstimateNo("Q-UPD-TEST");
        q.setCustomerName("Before");
        q.setCreatedByUserId(1);
        quotationRepository.insert(q);
        Long id = q.getId();

        // 2. 更新実行
        q.setCustomerName("After");
        int count = quotationRepository.update(q);

        // 3. 検証
        assertThat(count).isEqualTo(1);
        Quotation updated = quotationRepository.findById(id);
        assertThat(updated.getCustomerName()).isEqualTo("After");
    }

    @Test
    @DisplayName("delete: 削除ができること")
    void testDelete() {
        // 1. 新規作成
        Quotation q = new Quotation();
        q.setEstimateNo("Q-DEL-TEST");
        q.setCustomerName("Del");
        q.setCreatedByUserId(1);
        quotationRepository.insert(q);
        Long id = q.getId();

        // 2. 削除実行
        int count = quotationRepository.delete(id, 1); // 正しい作成者ID

        // 3. 検証
        assertThat(count).isEqualTo(1);
        assertThat(quotationRepository.findById(id)).isNull();
    }
}
