-- Branches
CREATE TABLE IF NOT EXISTS branches (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    sort_no INT DEFAULT 0,
    address VARCHAR(200),
    phone VARCHAR(20),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Users
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    department_code VARCHAR(20),
    department_name VARCHAR(100),
    branch_id INT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (branch_id) REFERENCES branches(id)
);

-- Sales Staffs
CREATE TABLE IF NOT EXISTS sales_staffs (
    id INT PRIMARY KEY,
    branch_id INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Customers
CREATE TABLE IF NOT EXISTS customers (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    kana VARCHAR(100),
    branch_id INT,
    sales_staff_id INT,
    address VARCHAR(200),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Quotations
CREATE TABLE IF NOT EXISTS quotations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, -- ★AUTO_INCREMENT に戻す
    estimate_no VARCHAR(50),
    version INT NOT NULL DEFAULT 1,
    is_submitted BOOLEAN DEFAULT FALSE,   -- BOOLEAN でOK
    created_by_user_id INT NOT NULL,
    user_department_name VARCHAR(100),
    sales_branch_id INT,
    sales_staff_id INT,
    customer_id INT,
    customer_name VARCHAR(100) NOT NULL,
    project_name VARCHAR(200),
    
    total_amount DECIMAL(12,0),
    discount_amount DECIMAL(19,2),
    total_cost DECIMAL(12,0),
    total_profit DECIMAL(12,0),
    profit_rate DECIMAL(5,2),
    grand_total DECIMAL(12,0),
    
    issue_date DATE,
    remarks TEXT,
    attached_file_path VARCHAR(500),
    
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Quotation Items
CREATE TABLE IF NOT EXISTS quotation_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, -- ★AUTO_INCREMENT に戻す
    quotation_id BIGINT NOT NULL,
    row_order INT NOT NULL,
    row_type VARCHAR(20) DEFAULT 'normal',
    item_code CHAR(9),
    item_name VARCHAR(255),
    manufacturer VARCHAR(100),
    quantity DECIMAL(10,2),
    cost_price DECIMAL(12,0),
    unit_price DECIMAL(12,0)
);
