-- Users
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    department_code VARCHAR(20),
    department_name VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Branches
CREATE TABLE IF NOT EXISTS branches (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    sort_no INT DEFAULT 0,
    address VARCHAR(200),
    phone VARCHAR(20),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Sales Staffs
CREATE TABLE IF NOT EXISTS sales_staffs (
    id INT PRIMARY KEY,
    branch_id INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Customers
CREATE TABLE IF NOT EXISTS customers (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    kana VARCHAR(100),
    branch_id INT,
    sales_staff_id INT,
    address VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Quotations
CREATE TABLE IF NOT EXISTS quotations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    estimate_no VARCHAR(50),
    version INT NOT NULL DEFAULT 1,
    is_submitted TINYINT(1) DEFAULT 0,
    created_by_user_id INT NOT NULL,
    user_department_name VARCHAR(100),
    sales_branch_id INT,
    sales_staff_id INT,
    customer_id INT,
    customer_name VARCHAR(100) NOT NULL,
    project_name VARCHAR(200),
    
    -- 金額系 (デフォルト値を削除しNULL許容に)
    total_amount DECIMAL(12,0),
    discount_amount DECIMAL(19,2), -- 実環境定義に合わせた桁数
    total_cost DECIMAL(12,0),
    total_profit DECIMAL(12,0),
    profit_rate DECIMAL(5,2),
    grand_total DECIMAL(12,0),
    
    issue_date DATE,
    remarks TEXT,
    attached_file_path VARCHAR(500),
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Quotation Items
CREATE TABLE IF NOT EXISTS quotation_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quotation_id BIGINT NOT NULL,
    row_order INT NOT NULL,
    row_type VARCHAR(20) DEFAULT 'normal',
    item_code CHAR(9), -- 実環境に合わせてCHAR(9)
    item_name VARCHAR(255),
    manufacturer VARCHAR(100),
    quantity DECIMAL(10,2),
    cost_price DECIMAL(12,0),
    unit_price DECIMAL(12,0)
);