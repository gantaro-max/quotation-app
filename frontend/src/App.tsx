// src/App.tsx
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

// 初期行データの生成ヘルパー
const createInitialRows = (): Row[] => 
  Array.from({ length: 20 }, (_, i) => ({ 
    id: i + 1, type: 'normal', code: '', manufacturer: '', item: '', quantity: 0, cost: 0, price: 0 
  }));

// 今日の日付文字列生成ヘルパー
const getTodayString = () => {
  const now = new Date();
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`;
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
  
  // 編集用State (初期値をここで設定することで、マウント時のresetForm呼び出しを不要にする)
  const [currentId, setCurrentId] = useState<number | null>(null);
  const [estimateNo, setEstimateNo] = useState('新規作成');
  const [date, setDate] = useState(getTodayString());
  
  // デフォルトでログインユーザーの営業所IDを設定
  const [searchBranchId, setSearchBranchId] = useState(currentUser.branchId || 9443);
  const [searchStaffId, setSearchStaffId] = useState(0); 
  const [projectName, setProjectName] = useState('');
  const [customerName, setCustomerName] = useState('');
  const [discount, setDiscount] = useState<number | string>('');
  const [remarks, setRemarks] = useState('');
  const [isSubmitted,setIsSubmitted] = useState(false);
  const [attachedFile, setAttachedFile] = useState<File | null>(null);
  const [rows, setRows] = useState(createInitialRows());

  // マスタデータState
  const [branches, setBranches] = useState<Branch[]>([]);
  const [staffs, setStaffs] = useState<SalesStaff[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);

  // 初期ロード：営業所一覧
  useEffect(() => {
    fetch('/api/master/branches')
      .then(res => res.json())
      .then(data => setBranches(data))
      .catch(e => console.error("営業所取得エラー:", e));
  }, []);

  // 1. 営業所変更 -> 担当者再取得
  useEffect(() => {
    if (!searchBranchId) return;
    fetch(`/api/master/staffs?branchId=${searchBranchId}`)
      .then(res => res.json())
      .then(data => setStaffs(data))
      .catch(e => console.error("担当者取得エラー:", e));
    
    // ★修正: ここでの setCustomers([]) を削除。
    // イベントハンドラ側で制御、またはstaffId変更時の副作用に任せる
  }, [searchBranchId]);

  // 2. 担当者変更 -> 顧客取得
  useEffect(() => {
    // ★修正: IDが0の場合は何もしない（クリア処理はイベントハンドラで行う）
    if (!searchStaffId || searchStaffId === 0) {
      return;
    }
    fetch(`/api/master/customers?salesStaffId=${searchStaffId}`)
      .then(res => res.json())
      .then(data => setCustomers(data))
      .catch(e => console.error("顧客取得エラー:", e));
  }, [searchStaffId]);

  // ★追加: 営業所変更時のハンドラ（EditScreenに渡す）
  // useEffectではなく、操作のタイミングで関連データをリセットする
  const handleBranchChange = (branchId: number) => {
    setSearchBranchId(branchId);
    setSearchStaffId(0);
    setCustomers([]); // 顧客リストもクリア
  };

  // ★追加: 担当者変更時のハンドラ
  const handleStaffChange = (staffId: number) => {
    setSearchStaffId(staffId);
    if (staffId === 0) {
      setCustomers([]); // 担当者が未選択になったら顧客もクリア
    }
  };

  const resetForm = () => {
    setCurrentId(null);
    setDate(getTodayString());
    setEstimateNo('新規作成');
    setSearchBranchId(currentUser.branchId || 9443);
    setSearchStaffId(0);
    setProjectName('');
    setCustomerName('');
    setDiscount('');
    setRemarks('');
    setIsSubmitted(false);
    setAttachedFile(null);
    setRows(createInitialRows());
    setCustomers([]); // 新規作成時は顧客リストもクリア
  };

  // ★修正: useEffect(() => resetForm(), [currentUser]) を削除
  // MainAppはログインのたびにマウントされるため、useStateの初期値だけで十分です。

  const handleSelectQuotation = async (id: number) => {
    try {
      const res = await fetch(`/api/quotations/${id}`);
      const json = await res.json();
      if(json.success) {
        const q: QuotationDto = json.data;
        setCurrentId(q.id);
        setEstimateNo(q.estimateNo);
        setDate(q.issueDate.replace(/-/g, '/'));
        setSearchBranchId(q.salesBranchId);
        setSearchStaffId(q.salesStaffId);
        setCustomerName(q.customerName);
        setProjectName(q.projectName);
        setRemarks(q.remarks);
        setIsSubmitted(q.isSubmitted);
        setDiscount(''); // DBに保存していない場合は空

        // 行データの復元
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
        // 20行未満なら空行で埋める
        while(uiRows.length < 20) {
          uiRows.push({ 
            id: uiRows.length + 1, type: 'normal', code: '', manufacturer: '', item: '', quantity: 0, cost: 0, price: 0 
          });
        }
        setRows(uiRows);
        setMode('EDIT');
        
        // 編集画面用に顧客リスト等をロードしておく必要がある場合はここで呼ぶことも検討
        // （現状はuseEffectがsalesStaffIdの変更を検知してロードしてくれます）
      }
    } catch(e) { console.error(e); alert('データ取得エラー'); }
  };

  const handleCreateNew = () => {
    resetForm();
    setMode('EDIT');
  };

  const handleSave = async (isUpdate: boolean) => {
    try {
      if (searchStaffId === 0) {
        alert('担当者を選択してください。');
        return;
      }
      const subTotal = rows.reduce((acc, r) => acc + (r.price * r.quantity), 0);
      const costTotal = rows.reduce((acc, r) => acc + (r.cost * r.quantity), 0);
      const profit = subTotal - costTotal;
      const profitRate = subTotal > 0 ? (profit / subTotal) * 100 : 0;
      const tax = Math.floor(subTotal * 0.1);
      const discountVal = Number(discount) || 0;
      const grandTotal = (subTotal - discountVal) + tax;

      const itemsPayload = rows
        .filter(r => r.item || r.quantity > 0 || r.price > 0)
        .map((r, i) => ({
          id: r.dbId || null,
          rowOrder: i + 1,
          rowType: r.type,
          itemCode: r.code,
          manufacturer: r.manufacturer,
          itemName: r.item,
          quantity: r.quantity,
          costPrice: r.cost,
          unitPrice: r.price
        }));

      const payload: QuotationDto = {
        id: currentId,
        estimateNo: estimateNo === '新規作成' ? '' : estimateNo,
        version: 1,
        isSubmitted: isSubmitted,
        createdByUserId: currentUser.id,
        salesBranchId: searchBranchId,
        salesStaffId: searchStaffId,
        customerId: null,
        customerName: customerName,
        projectName: projectName,
        issueDate: new Date().toISOString().split('T')[0],
        remarks: remarks,
        totalAmount: subTotal,
        totalCost: costTotal,
        totalProfit: profit,
        profitRate: parseFloat(profitRate.toFixed(2)),
        grandTotal: grandTotal,
        items: itemsPayload as QuotationItemDto[]
      };

      const method = isUpdate ? 'PUT' : 'POST';
      const url = isUpdate ? `/api/quotations/${currentId}?currentUserId=${currentUser.id}` : '/api/quotations';
      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      const json = await res.json();
      if(json.success) {
        alert('保存しました');
        setMode('SEARCH');
      } else {
        alert('エラー: ' + json.message);
      }
    } catch(e) { console.error(e); alert('通信エラー'); }
  };

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
      data={{ id: currentId, date, estimateNo, searchBranchId, searchStaffId, projectName, customerName, discount, remarks, rows, attachedFile, isSubmitted}}
      // ★修正: setterを直接渡すのではなく、ラッパー関数を渡して制御する
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