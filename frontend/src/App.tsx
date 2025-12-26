import { useEffect, useState } from 'react';
import { EditScreen } from './components/EditScreen';
import { LoginPage } from './components/LoginPage';
import { QuotationList } from './components/QuotationList';
import {
  type Branch,
  type Customer,
  type QuotationDto, type QuotationItemDto,
  type Row, type RowType,
  type SalesStaff,
  type User
} from './types';

// =============================================================================
// MainApp
// =============================================================================
type ScreenMode = 'SEARCH' | 'EDIT';

const createInitialRows = (): Row[] => 
  Array.from({ length: 20 }, (_, i) => ({ 
    id: i + 1, type: 'normal', code: '', manufacturer: '', item: '', quantity: 0, cost: 0, price: 0 
  }));

const getTodayString = () => {
  const now = new Date();
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`;
};

// 枝番採番ロジック
const generateNextBranchNo = (currentNo: string): string => {
  if (!currentNo) return '';

  const match = currentNo.match(/^(.*)-(\d+)$/);

  if (match) {
    const base = match[1];
    const numStr = match[2];
    
    const num = parseInt(numStr, 10);
    const nextNum = String(num + 1).padStart(numStr.length, '0');
    
    return `${base}-${nextNum}`;
  } else {
    return `${currentNo}-01`;
  }
};

export default function App() {
  const [currentUser, setCurrentUser] = useState<User | null>(null);

  const handleLogout = () => {
    setCurrentUser(null);
  };

  if (!currentUser) {
    return <LoginPage onLoginSuccess={setCurrentUser} />;
  }

  return <MainApp currentUser={currentUser} onLogout={handleLogout} />;
}

function MainApp({ currentUser, onLogout }: { currentUser: User, onLogout: () => void }) {
  const [mode, setMode] = useState<ScreenMode>('SEARCH');
  
  const [currentId, setCurrentId] = useState<number | null>(null);
  const [editingCreatorId, setEditingCreatorId] = useState<number | null>(null);
  const [editingCreatorName, setEditingCreatorName] = useState<string>('');

  const [estimateNo, setEstimateNo] = useState('新規作成');
  const [date, setDate] = useState(getTodayString());
  const [searchBranchId, setSearchBranchId] = useState(currentUser.branchId || 9443);
  const [searchStaffId, setSearchStaffId] = useState(0); 
  const [projectName, setProjectName] = useState('');
  const [customerName, setCustomerName] = useState('');
  const [discount, setDiscount] = useState<number | string>('');
  const [remarks, setRemarks] = useState('');  
  const [attachedFile, setAttachedFile] = useState<File | null>(null);
  const [currentAttachedFilePath,setCurrentAttachedFilePath] = useState<string | null>(null);
  
  const [rows, setRows] = useState(createInitialRows());
  const [isSubmitted, setIsSubmitted] = useState(false);

  const [branches, setBranches] = useState<Branch[]>([]);
  const [staffs, setStaffs] = useState<SalesStaff[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);

  useEffect(() => {
    fetch('/api/master/branches')
      .then(res => res.json())
      .then(data => setBranches(data))
      .catch(e => console.error("営業所取得エラー:", e));
  }, []);

  useEffect(() => {
    if (!searchBranchId) return;
    fetch(`/api/master/staffs?branchId=${searchBranchId}`)
      .then(res => res.json())
      .then(data => setStaffs(data))
      .catch(e => console.error("担当者取得エラー:", e));
  }, [searchBranchId]);

  useEffect(() => {
    if (!searchStaffId || searchStaffId === 0) return;
    fetch(`/api/master/customers?salesStaffId=${searchStaffId}`)
      .then(res => res.json())
      .then(data => setCustomers(data))
      .catch(e => console.error("顧客取得エラー:", e));
  }, [searchStaffId]);

  const handleBranchChange = (branchId: number) => {
    setSearchBranchId(branchId);
    setSearchStaffId(0);
    setCustomers([]);
  };

  const handleStaffChange = (staffId: number) => {
    setSearchStaffId(staffId);
    if (staffId === 0) setCustomers([]);
  };

  const resetForm = () => {
    setCurrentId(null);
    setEditingCreatorId(null);
    setDate(getTodayString());    
    setEstimateNo('(自動採番)');    
    setSearchBranchId(currentUser.branchId || 9443);
    setSearchStaffId(0);
    setProjectName('');
    setCustomerName('');
    setDiscount('');
    setRemarks('');
    setAttachedFile(null);
    setCurrentAttachedFilePath(null);
    setRows(createInitialRows());
    setCustomers([]);
    setIsSubmitted(false);
    setEditingCreatorName('');    
  };

  // 参照中のデータをコピーして新規作成モードへ移行
  const handleCopyCreate = () => {
    if (!window.confirm('現在表示中の内容をコピーして、新規作成モードに移行しますか？\n（現在表示している元のデータは変更されません）')) {
      return;
    }

    // 1. 基本情報のリセット
    setCurrentId(null);
    setEstimateNo('(自動採番)'); 
    setEditingCreatorId(currentUser.id); // 作成者を自分に変更
    setEditingCreatorName(currentUser.name);
    setDate(getTodayString());
    setIsSubmitted(false);

    // 2. 営業所・担当者・得意先のリセット
    setSearchBranchId(currentUser.branchId || 9443); 
    setSearchStaffId(0);
    setCustomerName('');
    setCustomers([]); 

    // 3. 添付ファイルのリセット（ファイルはコピーしない仕様）
    setAttachedFile(null);
    setCurrentAttachedFilePath(null);

    alert('新規作成モードに切り替えました。\n内容を編集して「新規保存」してください。');
  };

  const handleTransitionToNew = () => {
    if(!window.confirm('現在入力中の内容をコピーして、新規作成モードに移行しますか？\n(添付ファイルはクリアされます)')) return;
    setCurrentId(null);
    setEstimateNo('(自動採番)');
    setEditingCreatorId(currentUser.id);
    setEditingCreatorName(currentUser.name);
    setIsSubmitted(false);
    setAttachedFile(null);
    setCurrentAttachedFilePath(null);
    alert('新規作成モードに切り替えました。\n内容を確認し「新規保存」してください。');
  }

  const handleSelectQuotation = async (id: number) => {
    try {
      const res = await fetch(`/api/quotations/${id}`);
      const json = await res.json();
      if(json.success) {
        const q: QuotationDto = json.data;
        setCurrentId(q.id);
        setEditingCreatorId(q.createdByUserId);
        setEditingCreatorName(q.createdByUserName || '');
        setEstimateNo(q.estimateNo || '');
        setDate(q.issueDate.replace(/-/g, '/'));
        setSearchBranchId(q.salesBranchId);
        setSearchStaffId(q.salesStaffId);
        setCustomerName(q.customerName);
        setProjectName(q.projectName);
        setRemarks(q.remarks);        
        setDiscount(q.discountAmount !== undefined && q.discountAmount !== null ? q.discountAmount : '');
        setIsSubmitted(q.isSubmitted);        
        setAttachedFile(null);
        setCurrentAttachedFilePath(q.attachedFilePath); 

        const uiRows: Row[] = q.items.map((item, i) => ({
          id: i + 1,
          dbId: item.id,
          type: (item.rowType as RowType) || 'normal',
          code: item.itemCode,
          manufacturer: item.manufacturer,
          item: item.itemName,
          quantity: item.quantity,
          cost: item.costPrice,
          price: item.unitPrice
        }));
        while(uiRows.length < 20) {
          uiRows.push({ id: uiRows.length + 1, type: 'normal', code: '', manufacturer: '', item: '', quantity: 0, cost: 0, price: 0 });
        }
        setRows(uiRows);
        setMode('EDIT');
      }
    } catch(e) { console.error(e); alert('データ取得エラー'); }
  };

  const handleCreateNew = () => {
    resetForm();
    setMode('EDIT');
  };

  // ★FormDataを使った送信処理
  const handleSave = async (isUpdate: boolean) => {
    try {
      // 1. 必須チェック
      if (searchStaffId === 0) {
        alert('担当者を選択してください。');
        return;
      }
      if (!customerName.trim()) {
        alert('得意先名を入力してください。');
        return;
      }

      // 2. 計算ロジック
      const subTotal = rows.reduce((acc, r) => acc + (r.price * r.quantity), 0);
      const costTotal = rows.reduce((acc, r) => acc + (r.cost * r.quantity), 0);
      const profit = subTotal - costTotal;
      const profitRate = subTotal > 0 ? (profit / subTotal) * 100 : 0;
      
      const discountVal = discount === '' ? 0 : Number(discount);
      const mainTotal = subTotal - discountVal;
      const tax = Math.floor(mainTotal * 0.1);
      const grandTotal = mainTotal + tax;

      // 3. 枝番・新規判定ロジック
      let saveAsBranch = false;
      let finalEstimateNo = estimateNo;
      
      if (isUpdate && isSubmitted) {
        if (window.confirm('この見積は提出済みです。枝番を作成して新しい版として保存しますか？\n（キャンセルを押すと上書き保存を試みます）')) {
          saveAsBranch = true;
          finalEstimateNo = generateNextBranchNo(estimateNo);
        }
      }

      let payloadEstimateNo: string | null = finalEstimateNo;
      if (finalEstimateNo === '(自動採番)' || finalEstimateNo === '新規作成' || finalEstimateNo === '') {
        payloadEstimateNo = null;
      }

      const itemsPayload = rows
        .filter(r => r.item || r.quantity > 0 || r.price > 0)
        .map((r, i) => ({
          id: saveAsBranch ? null : (r.dbId || null),
          rowOrder: i + 1,
          rowType: r.type,
          itemCode: r.code,
          manufacturer: r.manufacturer,
          itemName: r.item,
          quantity: r.quantity,
          costPrice: r.cost,
          unitPrice: r.price
        }));

      // 4. ペイロード(DTO)作成
      const payload: QuotationDto = {
        id: saveAsBranch ? null : currentId,
        estimateNo: payloadEstimateNo,
        version: 1,
        isSubmitted: saveAsBranch ? false : isSubmitted,
        createdByUserId: currentUser.id,
        createdByUserName: null,
        salesBranchId: searchBranchId,
        salesStaffId: searchStaffId,
        customerId: null,
        customerName: customerName,
        projectName: projectName,
        issueDate: new Date().toISOString().split('T')[0],
        remarks: remarks,
        totalAmount: subTotal,
        discountAmount: discountVal,
        totalCost: costTotal,
        totalProfit: profit,
        profitRate: parseFloat(profitRate.toFixed(2)),
        grandTotal: grandTotal,       
        attachedFilePath: null, 
        items: itemsPayload as QuotationItemDto[]
      };

      // 5. FormDataの作成と送信
      const formData = new FormData();
      
      // JSONデータをBlobとして追加 ('quotation'というキーはBackendの@RequestPartと一致させる)
      const jsonBlob = new Blob([JSON.stringify(payload)], { type: 'application/json' });
      formData.append('quotation', jsonBlob);

      // ファイルがあれば追加
      if (attachedFile) {
        formData.append('file', attachedFile);
      }

      const isRealUpdate = isUpdate && !saveAsBranch && currentId !== null;
      const method = isRealUpdate ? 'PUT' : 'POST';
      const url = isRealUpdate 
        ? `/api/quotations/${currentId}?currentUserId=${currentUser.id}` 
        : '/api/quotations';
      
      const res = await fetch(url, {
        method,
        // headers: { 'Content-Type': 'application/json' }, ← これを削除！(ブラウザが自動設定するため)
        body: formData 
      });
      const json = await res.json();

      if(json.success) {
        const savedData: QuotationDto = json.data;
        
        setCurrentId(savedData.id);
        setEditingCreatorId(savedData.createdByUserId);
        setEditingCreatorName(savedData.createdByUserName || currentUser.name);
        setEstimateNo(savedData.estimateNo || ''); 
        
        const msg = saveAsBranch 
          ? `枝番「${savedData.estimateNo}」を作成して保存しました` 
          : `保存しました\n見積No: ${savedData.estimateNo}`;
        alert(msg);
        
        // 保存後は一覧に戻り、ファイル選択状態をクリア
        setMode('SEARCH'); 
        setAttachedFile(null);

      } else {
         console.error("Save Error Response:", json);
         let errorMsg = '保存エラー: ' + (json.message || '不明なエラー');
         if (json.errors) {
             if (Array.isArray(json.errors)) {
                  const details = json.errors.map((e: { field?: string; defaultMessage?: string; message?: string }) => 
                      `・${e.field || '項目'}: ${e.defaultMessage || e.message}`
                  ).join('\n');
                  errorMsg += '\n\n【詳細】\n' + details;
             } else if (typeof json.errors === 'object') {
                  const details = Object.entries(json.errors).map(([k, v]) => `・${k}: ${v}`).join('\n');
                  errorMsg += '\n\n【詳細】\n' + details;
             }
         }
         alert(errorMsg);
      }
    } catch(e) { 
      console.error(e); 
      alert('通信エラーが発生しました'); 
    }
  };

  const isReadOnly = currentId !== null && editingCreatorId !== null && editingCreatorId !== currentUser.id;

  if (mode === 'SEARCH') {
    return (
      <QuotationList 
        currentUser={currentUser}
        onLogout={onLogout}
        onSelectQuotation={handleSelectQuotation}
        onCreateNew={handleCreateNew}
      />
    );
  }

  return (
    <EditScreen 
      currentUser={currentUser}
      isReadOnly={isReadOnly}
      onCopyCreate={handleCopyCreate}
      onTransitionToNew={handleTransitionToNew}
      creatorName={editingCreatorName || currentUser.name}
      data={{ id: currentId, date, estimateNo, searchBranchId, searchStaffId, projectName, customerName, discount, remarks, rows, attachedFile, attachedFilePath: currentAttachedFilePath, isSubmitted }}
      setters={{ 
        setSearchBranchId: handleBranchChange, 
        setSearchStaffId: handleStaffChange, 
        setProjectName, setCustomerName, setDiscount, setRemarks, setRows, setAttachedFile, setIsSubmitted
      }}
      masterData={{ branches, staffs, customers }}
      onBack={() => setMode('SEARCH')}
      onSave={handleSave}
    />
  );
}