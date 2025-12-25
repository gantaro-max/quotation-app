export type RowType = 'normal' | 'manufacturer' | 'note' | 'detail';

export interface Row {
  id: number;
  dbId?: number | null;
  type: RowType;
  code: string;
  manufacturer: string;
  item: string;
  quantity: number;
  cost: number;
  price: number;
}

export interface User {
  id: number;
  name: string;
  email?: string;
  branchId: number;
}

export interface Branch {
  id: number;
  name: string;
  address: string;
  phone: string;
}

export interface SalesStaff {
  id: number;
  branchId: number;
  name: string;
}

export interface Customer {
  id: number;
  salesStaffId?: number;
  name: string;
}

export interface QuotationDto {
  id: number | null;
  estimateNo: string | null;
  version: number;
  isSubmitted: boolean;
  createdByUserId: number;
  createdByUserName: string | null;
  salesBranchId: number;
  salesStaffId: number;
  customerId: number | null;
  customerName: string;
  projectName: string;
  issueDate: string;
  remarks: string;
  totalAmount: number;
  discountAmount?: number;
  totalCost: number;
  totalProfit: number;
  profitRate: number;
  grandTotal: number;
  attachedFilePath: string | null;
  items: QuotationItemDto[];
  salesBranch?: { name: string; address: string; phone: string };
  salesStaff?: { name: string };
}

export interface QuotationItemDto {
  id: number | null;
  rowOrder: number;
  rowType: string;
  itemCode: string;
  manufacturer: string;
  itemName: string;
  quantity: number;
  costPrice: number;
  unitPrice: number;
}