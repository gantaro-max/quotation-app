-- Users
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50),
    email VARCHAR(100),
    password_hash VARCHAR(255),
    department_code VARCHAR(20),
    department_name VARCHAR(100),
    created_at DATETIME,
    updated_at DATETIME
);

-- Branches
CREATE TABLE IF NOT EXISTS branches (
    id INT PRIMARY KEY,
    name VARCHAR(50),
    address VARCHAR(200),
    phone VARCHAR(20),
    created_at DATETIME,
    updated_at DATETIME
);

-- Sales Staffs
CREATE TABLE IF NOT EXISTS sales_staffs (
    id INT PRIMARY KEY,
    branch_id INT,
    name VARCHAR(50),
    created_at DATETIME,
    updated_at DATETIME
);

-- Customers
CREATE TABLE IF NOT EXISTS customers (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    branch_id INT,
    sales_staff_id INT,
    created_at DATETIME,
    updated_at DATETIME
);

-- Quotations
CREATE TABLE IF NOT EXISTS quotations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    estimate_no VARCHAR(20),
    version INT,
    is_submitted BOOLEAN,
    created_by_user_id INT,
    user_department_name VARCHAR(100),
    sales_branch_id INT,
    sales_staff_id INT,
    customer_id INT,
    customer_name VARCHAR(100),
    project_name VARCHAR(200),
    total_amount DECIMAL(12,0),
    total_cost DECIMAL(12,0),
    total_profit DECIMAL(12,0),
    profit_rate DECIMAL(5,2),
    grand_total DECIMAL(12,0),
    issue_date DATE,
    remarks TEXT,
    attached_file_path VARCHAR(500),
    created_at DATETIME,
    updated_at DATETIME
);

-- Quotation Items
CREATE TABLE IF NOT EXISTS quotation_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quotation_id BIGINT,
    row_order INT,
    row_type VARCHAR(20),
    item_code VARCHAR(50),
    item_name VARCHAR(255),
    manufacturer VARCHAR(100),
    quantity DECIMAL(10,2),
    cost_price DECIMAL(12,0),
    unit_price DECIMAL(12,0)
);