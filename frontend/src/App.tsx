import React, { useEffect, useMemo, useRef, useState } from 'react';

// --- 型定義 ---
type RowType = 'normal' | 'manufacturer' | 'note' | 'detail';

interface Row {
  id: number;
  type: RowType;
  code: string;
  manufacturer: string;
  item: string;
  quantity: number;
  cost: number;
  price: number;
}

interface User {
  id: number;
  branchId: number;
  name: string;
  password: string;
}

// 見積データ型
interface Quotation {
  id: number;
  estimateNo: string;
  version: number;
  branchId: number;
  staffId: number;
  customerId: number;
  customerName: string;
  projectName: string;
  date: string;
  rows: Row[];
  discount: number;
  remarks: string;
  attachedFileName?: string;
}

// 座標管理用
interface CellCoords {
  rowIndex: number;
  colKey: string;
}

// --- モックデータ ---
const BRANCHES = [
  { id: 1, name: '広島北営業所', address: '広島市安佐南区伴西2-3-14', phone: '082-225-8411' },
  { id: 2, name: '広島東営業所', address: '広島市東区温品1-2-3', phone: '082-111-2222' },
  { id: 3, name: '岡山営業所', address: '岡山市北区下中野7-8-9', phone: '086-333-4444' },
];

const USERS: User[] = [
  { id: 1, branchId: 1, name: '佐藤 宏樹', password: '1234' },
  { id: 2, branchId: 1, name: '田中 一郎', password: '1234' },
  { id: 3, branchId: 2, name: '鈴木 次郎', password: '1234' },
  { id: 4, branchId: 3, name: '高橋 三郎', password: '1234' },
];

const CUSTOMERS = [
  { id: 1, staffId: 1, name: '医療法人社団 うすい会' },
  { id: 2, staffId: 1, name: '社会福祉法人 あおぞら会' },
  { id: 3, staffId: 2, name: '株式会社 広島メディカル' },
  { id: 4, staffId: 3, name: '温品クリニック' },
  { id: 5, staffId: 4, name: '岡山中央病院' },
];

const SAVED_QUOTATIONS: Quotation[] = [
  {
    id: 1001, estimateNo: '231215001', version: 1, branchId: 1, staffId: 1, customerId: 1,
    customerName: '医療法人社団 うすい会', projectName: '本館 空調設備更新工事',
    date: '2023年12月15日', discount: 5000, attachedFileName: '見積依頼書.pdf',
    remarks: '工期：ご発注後約2週間\n支払条件：検収後翌月末振込',
    rows: Array.from({ length: 35 }, (_, i) => ({ id: i + 1, type: 'normal', code: '', manufacturer: '', item: i === 0 ? '空調機A' : '', quantity: i === 0 ? 1 : 0, cost: 0, price: 100000 }))
  },
  {
    id: 1002, estimateNo: '231216002', version: 1, branchId: 1, staffId: 1, customerId: 1,
    customerName: '医療法人社団 うすい会', projectName: 'MRI室 新設工事',
    date: '2023年12月16日', discount: 0,
    remarks: '有効期限：発行より1ヶ月',
    rows: Array.from({ length: 20 }, (_, i) => ({ id: i + 1, type: 'normal', code: '', manufacturer: '', item: '', quantity: 0, cost: 0, price: 0 }))
  },
];

// --- 設定 (行数を増やして1ページの密度を上げる) ---
const ROWS_FIRST_PAGE = 32;  // 変更: 30 -> 32
const ROWS_OTHER_PAGES = 40; // 変更: 36 -> 40

// --- ユーティリティ ---
const toHalfWidth = (str: string) => {
  return str.replace(/[０-９]/g, (s) => {
    return String.fromCharCode(s.charCodeAt(0) - 0xFEE0);
  });
};

// --- スタイル定義 ---
const styles: { [key: string]: React.CSSProperties } = {
  loginContainer: { height: '100vh', display: 'flex', justifyContent: 'center', alignItems: 'center', backgroundColor: '#2c3e50', flexDirection: 'column', color: 'white', fontFamily: 'sans-serif' },
  loginBox: { padding: '40px', backgroundColor: 'white', borderRadius: '8px', boxShadow: '0 10px 25px rgba(0,0,0,0.5)', color: '#333', width: '350px', display: 'flex', flexDirection: 'column', gap: '15px' },
  loginTitle: { margin: '0 0 10px 0', textAlign: 'center', color: '#2c3e50', borderBottom: '2px solid #3498db', paddingBottom: '10px' },
  inputLabelText: { fontSize: '0.9em', fontWeight: 'bold', color: '#555', marginBottom: '5px', display: 'block' },
  loginSelect: { width: '100%', padding: '10px', fontSize: '1em', border: '1px solid #ccc', borderRadius: '4px', boxSizing: 'border-box' },
  loginInput: { width: '100%', padding: '10px', fontSize: '1em', border: '1px solid #ccc', borderRadius: '4px', boxSizing: 'border-box' },
  loginButton: { padding: '12px', backgroundColor: '#3498db', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '1.1em', fontWeight: 'bold', marginTop: '10px', transition: 'background 0.3s' },
  errorMsg: { color: '#e74c3c', fontSize: '0.9em', textAlign: 'center', marginTop: '10px', fontWeight: 'bold' },

  container: { display: 'flex', height: '100vh', overflow: 'hidden', fontFamily: '"Hiragino Kaku Gothic ProN", "Meiryo", sans-serif', backgroundColor: '#555', padding: '10px', boxSizing: 'border-box', gap: '15px' },
  
  searchPanel: { width: '100%', height: '100%', backgroundColor: '#f4f6f9', borderRadius: '4px', padding: '20px', boxSizing: 'border-box', overflowY: 'auto' },
  searchHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', borderBottom: '1px solid #ddd', paddingBottom: '10px' },
  searchInput: { padding: '10px', fontSize: '1em', width: '300px', borderRadius: '4px', border: '1px solid #ccc' },
  searchBtn: { padding: '10px 20px', backgroundColor: '#3498db', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold', fontSize: '1em', marginLeft: '10px' },
  searchTable: { width: '100%', borderCollapse: 'collapse', backgroundColor: 'white', boxShadow: '0 2px 5px rgba(0,0,0,0.1)' },
  searchTh: { backgroundColor: '#34495e', color: 'white', padding: '12px', textAlign: 'left', borderBottom: '2px solid #2c3e50' },
  searchTd: { padding: '12px', borderBottom: '1px solid #eee', cursor: 'pointer' },
  searchRow: { transition: 'background 0.2s' },
  newBtn: { padding: '10px 20px', backgroundColor: '#2ecc71', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold', fontSize: '1em' },
  logoutBtn: { padding: '8px 15px', backgroundColor: '#95a5a6', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.9em', marginLeft: '15px' },

  leftPanel: { width: '55%', flexShrink: 0, backgroundColor: '#f4f6f9', borderRadius: '4px', display: 'flex', flexDirection: 'column', height: '100%', boxSizing: 'border-box', boxShadow: '0 0 10px rgba(0,0,0,0.3)' },
  leftHeader: { padding: '15px', backgroundColor: 'white', borderBottom: '1px solid #ddd', borderTopLeftRadius: '4px', borderTopRightRadius: '4px', flexShrink: 0 },
  filterRow: { display: 'flex', gap: '10px', marginBottom: '10px' },
  filterGroup: { flex: 1, display: 'flex', flexDirection: 'column' },
  labelSmall: { fontSize: '0.8em', color: '#666', marginBottom: '3px', fontWeight: 'bold' },
  headerSelect: { padding: '8px', fontSize: '0.9em', borderRadius: '4px', border: '1px solid #ccc', width: '100%', backgroundColor: '#fff' },
  customerSelect: { padding: '8px', fontSize: '1em', borderRadius: '4px', border: '1px solid #ccc', width: '100%', fontWeight: 'bold', color: '#2c3e50' },
  projectInput: { padding: '8px', fontSize: '1em', borderRadius: '4px', border: '2px solid #3498db', width: '100%', fontWeight: 'bold', color: '#2c3e50', backgroundColor: '#ebf5fb', boxSizing: 'border-box' },

  attachArea: { marginTop: '10px', padding: '10px', backgroundColor: '#f8f9fa', border: '1px dashed #ccc', borderRadius: '4px', display: 'flex', alignItems: 'center', gap: '10px' },
  attachBtn: { fontSize: '0.9em', padding: '5px 12px', backgroundColor: '#2980b9', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '5px' },
  ocrBtn: { fontSize: '0.9em', padding: '5px 12px', backgroundColor: '#95a5a6', color: 'white', border: 'none', borderRadius: '4px', cursor: 'not-allowed', display: 'flex', alignItems: 'center', gap: '5px', opacity: 0.7 },
  fileName: { fontSize: '0.85em', color: '#333', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '200px' },
  backBtn: { fontSize: '0.9em', padding: '5px 15px', backgroundColor: '#7f8c8d', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', marginRight: '10px' },

  gridContainer: { flexGrow: 1, overflow: 'auto', padding: '0', position: 'relative', backgroundColor: '#fff' },
  gridTable: { width: '100%', borderCollapse: 'collapse', fontSize: '0.85em', backgroundColor: 'white' },
  gridTh: { backgroundColor: '#34495e', color: 'white', padding: '10px 5px', border: '1px solid #2c3e50', textAlign: 'center', whiteSpace: 'nowrap', position: 'sticky', top: 0, zIndex: 100, boxShadow: '0 2px 2px rgba(0,0,0,0.1)' },
  gridTd: { border: '1px solid #dee2e6', padding: '0', verticalAlign: 'middle', backgroundColor: 'white' },

  remarksInputArea: { padding: '10px', backgroundColor: '#fcfcfc', borderTop: '1px solid #ddd' },
  remarksInput: { 
    width: '100%', 
    minHeight: '60px', 
    padding: '8px', 
    boxSizing: 'border-box', 
    border: '1px solid #ccc', 
    borderRadius: '4px', 
    fontSize: '0.9em', 
    resize: 'none', 
    fontFamily: 'inherit',
    overflow: 'hidden'
  },

  leftFooter: { padding: '15px 20px', backgroundColor: '#2c3e50', color: 'white', borderTop: '1px solid #ccc', borderBottomLeftRadius: '4px', borderBottomRightRadius: '4px', flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'space-between' },
  calcContainer: { display: 'flex', alignItems: 'center', gap: '20px', backgroundColor: 'rgba(0,0,0,0.2)', padding: '5px 15px', borderRadius: '4px' },
  calcItem: { display: 'flex', alignItems: 'center', gap: '10px' },
  footerLabel: { fontSize: '0.9em', color: '#bdc3c7', whiteSpace: 'nowrap' },
  footerValue: { fontSize: '1.2em', fontWeight: 'bold', minWidth: '80px', textAlign: 'right' as const },
  discountInput: { width: '120px', padding: '5px', textAlign: 'right', borderRadius: '4px', border: 'none', fontWeight: 'bold', fontSize: '1.1em' },
  footerBtns: { display: 'flex', gap: '10px' },
  footerActionBtn: { padding: '10px 20px', backgroundColor: '#e67e22', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold', fontSize: '1em' },
  
  smallInput: { width: '100%', height: '100%', border: 'none', padding: '8px', boxSizing: 'border-box', outline: 'none', fontSize: '1em', background: 'transparent' },
  typeSelect: { width: '100%', border: 'none', padding: '8px', fontSize: '0.9em', cursor: 'pointer', outline: 'none', background: 'transparent' },
  profitCell: { padding: '0 8px', textAlign: 'right', verticalAlign: 'middle' },
  amountCell: { padding: '0 8px', textAlign: 'right', verticalAlign: 'middle', fontWeight: 'bold', backgroundColor: '#f9f9f9', color: '#333' },
  
  rightPanel: { width: '45%', flexShrink: 0, overflowY: 'auto', display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '0 10px' },
  
  pageContainer: { 
    width: '210mm', minHeight: '297mm', backgroundColor: 'white', 
    padding: '10mm 15mm', 
    boxSizing: 'border-box', marginBottom: '20px', position: 'relative', 
    fontFamily: '"MS Mincho", "Hiragino Mincho ProN", serif', color: '#333', 
    transform: 'scale(0.85)', transformOrigin: 'top center', boxShadow: '0 5px 15px rgba(0,0,0,0.5)', marginTop: '20px' 
  },
  
  headerTitle: { textAlign: 'center', fontSize: '1.5em', textDecoration: 'underline', marginBottom: '5px', letterSpacing: '0.3em' },
  topSection: { display: 'flex', justifyContent: 'space-between', marginBottom: '10px', alignItems: 'flex-start' },
  customerInfo: { width: '58%' },
  companyInfo: { width: '40%', fontSize: '0.85em', lineHeight: '1.2', textAlign: 'right' },
  summaryBox: { marginTop: '5px', borderBottom: '2px solid #000', paddingBottom: '3px', width: '95%' },
  summaryRow: { display: 'flex', justifyContent: 'space-between', padding: '0', borderBottom: '1px solid #ccc' },
  totalRow: { display: 'flex', justifyContent: 'space-between', padding: '2px 0', fontWeight: 'bold', fontSize: '1.1em', borderBottom: 'none' },
  table: { width: '100%', borderCollapse: 'collapse', marginTop: '5px', marginBottom: '5px', fontSize: '0.9em', tableLayout: 'fixed' },
  th: { border: '1px solid #000', padding: '2px', backgroundColor: '#f0f0f0', textAlign: 'center', fontWeight: 'bold', height: '22px', fontSize: '0.9em' },
  td: { border: '1px solid #000', padding: '0 4px', height: '22px', verticalAlign: 'middle' },
  
  footerArea: { 
    marginTop: 'auto', 
    width: '100%',
    breakInside: 'avoid',        
    pageBreakInside: 'avoid',    
    display: 'block'             
  },
  footerTable: { width: '50%', marginLeft: 'auto', borderCollapse: 'collapse', marginBottom: '5px' },
  
  remarksBox: { 
    border: '1px solid #000', 
    padding: '5px', 
    minHeight: '120px', 
    height: 'auto',     
    marginTop: '2px', 
    width: '100%', 
    whiteSpace: 'pre-wrap', 
    fontSize: '0.8em', 
    lineHeight: '1.2',
    wordBreak: 'break-all'
  },
  
  copyBtn: { fontSize: '0.8em', padding: '2px 8px', backgroundColor: '#95a5a6', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', marginLeft: '10px' }
};

export default function App() {
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [loginSelectId, setLoginSelectId] = useState(1);
  const [loginPassword, setLoginPassword] = useState('');
  const [loginError, setLoginError] = useState('');

  const handleLogin = () => {
    const targetUser = USERS.find(u => u.id === loginSelectId);
    if (targetUser && targetUser.password === loginPassword) {
      setCurrentUser(targetUser);
      setLoginError('');
    } else {
      setLoginError('パスワードが違います');
    }
  };

  const handleLogout = () => {
    setCurrentUser(null);
    setLoginPassword('');
    setLoginSelectId(1);
  };

  if (!currentUser) {
    return (
      <div style={styles.loginContainer}>
        <div style={styles.loginBox}>
          <h2 style={{...styles.loginTitle, borderBottom:'none'}}>業務システム</h2>
          <div style={{borderBottom:'1px solid #ddd', marginBottom:'15px'}}></div>
          <div style={{textAlign:'left'}}>
            <label style={styles.inputLabelText}>担当者を選択</label>
            <select style={styles.loginSelect} value={loginSelectId} onChange={(e) => setLoginSelectId(Number(e.target.value))}>
              {USERS.map(u => <option key={u.id} value={u.id}>{u.name} ({BRANCHES.find(b=>b.id===u.branchId)?.name})</option>)}
            </select>
          </div>
          <div style={{textAlign:'left'}}>
            <label style={styles.inputLabelText}>パスワード</label>
            <input type="password" style={styles.loginInput} value={loginPassword} onChange={(e) => setLoginPassword(e.target.value)} placeholder="パスワードを入力" onKeyPress={(e) => { if(e.key === 'Enter') handleLogin() }} />
          </div>
          <button style={styles.loginButton} onClick={handleLogin}>ログイン</button>
          {loginError && <div style={styles.errorMsg}>⚠️ {loginError}</div>}
          <div style={{fontSize:'0.8em', color:'#aaa', marginTop:'10px'}}>※テスト用パスワード: 1234</div>
        </div>
      </div>
    );
  }

  return <MainApp currentUser={currentUser} onLogout={handleLogout} />;
}

type ScreenMode = 'SEARCH' | 'EDIT';

interface EditScreenProps {
  currentUser: User;
  data: {
    date: string;
    estimateNo: string;
    searchBranchId: number;
    searchStaffId: number;
    projectName: string;
    customerName: string;
    discount: number | string;
    remarks: string;
    rows: Row[];
    attachedFile: File | null;
    currentQuotationId: number | null;
  };
  setters: {
    setSearchBranchId: (val: number) => void;
    setSearchStaffId: (val: number) => void;
    setProjectName: (val: string) => void;
    setCustomerName: (val: string) => void;
    setDiscount: (val: number | string) => void;
    setRemarks: (val: string) => void;
    setRows: (val: Row[]) => void;
    setAttachedFile: (val: File | null) => void;
  };
  onBack: () => void;
  onSave: (isBranch: boolean) => void;
}

function MainApp({ currentUser, onLogout }: { currentUser: User, onLogout: () => void }) {
  const [mode, setMode] = useState<ScreenMode>('SEARCH');
  
  const [searchText, setSearchText] = useState('');
  const [hasSearched, setHasSearched] = useState(false);
  const [quotationList] = useState<Quotation[]>(SAVED_QUOTATIONS);

  const [currentQuotationId, setCurrentQuotationId] = useState<number | null>(null);
  const [date, setDate] = useState('');
  const [estimateNo, setEstimateNo] = useState('');
  const [searchBranchId, setSearchBranchId] = useState(1);
  const [searchStaffId, setSearchStaffId] = useState(0);
  const [projectName, setProjectName] = useState('');
  const [customerName, setCustomerName] = useState('');
  const [discount, setDiscount] = useState<number | string>(''); 
  const [remarks, setRemarks] = useState('');
  const [rows, setRows] = useState<Row[]>([]);
  const [attachedFile, setAttachedFile] = useState<File | null>(null);

  const resetForm = () => {
    setRows(Array.from({ length: 20 }, (_, i) => ({ id: i + 1, type: 'normal', code: '', manufacturer: '', item: '', quantity: 0, cost: 0, price: 0 })));
    setProjectName('');
    setCustomerName('');
    setDiscount('');
    setRemarks('');
    setAttachedFile(null);
    setCurrentQuotationId(null);
  };

  useEffect(() => {
    const now = new Date();
    setDate(`${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`);
    setEstimateNo(now.getFullYear().toString().slice(-2) + (now.getMonth() + 1).toString().padStart(2, '0') + now.getDate().toString().padStart(2, '0') + "001");
    setSearchBranchId(currentUser.branchId);
    setSearchStaffId(currentUser.id);
    resetForm();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentUser]);

  const executeSearch = () => {
    setHasSearched(true);
  };

  const filteredQuotations = useMemo(() => {
    if (!searchText) return quotationList;
    const lowerText = searchText.toLowerCase();
    return quotationList.filter(q => 
      q.customerName.toLowerCase().includes(lowerText) || 
      q.projectName.toLowerCase().includes(lowerText) ||
      q.estimateNo.includes(lowerText)
    );
  }, [searchText, quotationList]);

  const handleSelectQuotation = (q: Quotation) => {
    setCurrentQuotationId(q.id);
    setEstimateNo(q.estimateNo);
    setSearchBranchId(q.branchId);
    setSearchStaffId(q.staffId);
    setCustomerName(q.customerName);
    setProjectName(q.projectName);
    setDate(q.date);
    setDiscount(q.discount === 0 ? '' : q.discount);
    setRemarks(q.remarks || '');
    setRows(q.rows.map(row => ({...row})));
    setAttachedFile(null);
    setMode('EDIT');
  };

  const handleCreateNew = () => {
    resetForm();
    setMode('EDIT');
  };

  const handleSave = (isBranch: boolean) => {
    alert(isBranch ? "修正保存しました（モック）" : "新規保存しました（モック）");
    setMode('SEARCH');
    setHasSearched(false);
    setSearchText('');
  };

  if (mode === 'SEARCH') {
    return (
      <div style={styles.container}>
        <div style={styles.searchPanel}>
          <div style={styles.searchHeader}>
            <div style={{display:'flex', alignItems:'center', gap:'10px'}}>
              <h2>見積検索</h2>
              <input 
                style={styles.searchInput} 
                placeholder="得意先名、案件名、見積Noで検索" 
                value={searchText} 
                onChange={(e) => setSearchText(e.target.value)} 
                onKeyPress={(e) => { if(e.key === 'Enter') executeSearch() }}
              />
              <button style={styles.searchBtn} onClick={executeSearch}>検 索</button>
            </div>
            <div>
              <button style={styles.newBtn} onClick={handleCreateNew}>＋ 新規作成</button>
              <button style={styles.logoutBtn} onClick={onLogout}>ログアウト</button>
            </div>
          </div>
          
          {hasSearched ? (
            <table style={styles.searchTable}>
              <thead>
                <tr>
                  <th style={styles.searchTh}>見積No</th>
                  <th style={styles.searchTh}>日付</th>
                  <th style={styles.searchTh}>得意先名</th>
                  <th style={styles.searchTh}>案件名</th>
                  <th style={styles.searchTh}>担当</th>
                  <th style={styles.searchTh}>合計金額</th>
                </tr>
              </thead>
              <tbody>
                {filteredQuotations.length > 0 ? (
                  filteredQuotations.map(q => {
                    const total = q.rows.reduce((acc, r) => acc + (r.price * r.quantity), 0);
                    const grandTotal = Math.floor(total * 1.1) - q.discount;
                    return (
                      <tr key={q.id} style={styles.searchRow} onClick={() => handleSelectQuotation(q)} onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#f0f8ff'} onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'white'}>
                        <td style={styles.searchTd}>{q.estimateNo}</td>
                        <td style={styles.searchTd}>{q.date}</td>
                        <td style={styles.searchTd}>{q.customerName}</td>
                        <td style={styles.searchTd}>{q.projectName}</td>
                        <td style={styles.searchTd}>{USERS.find(u => u.id === q.staffId)?.name}</td>
                        <td style={styles.searchTd}>¥{grandTotal.toLocaleString()}</td>
                      </tr>
                    );
                  })
                ) : (
                  <tr>
                    <td colSpan={6} style={{...styles.searchTd, textAlign:'center', color:'#999'}}>該当する見積が見つかりません</td>
                  </tr>
                )}
              </tbody>
            </table>
          ) : (
            <div style={{textAlign:'center', marginTop:'50px', color:'#aaa'}}>
              <p>検索条件を入力して「検索」ボタンを押してください</p>
            </div>
          )}
        </div>
      </div>
    );
  }

  return (
    <EditScreen 
      currentUser={currentUser}
      data={{
        date, estimateNo, searchBranchId, searchStaffId, projectName, customerName, discount, remarks, rows, attachedFile, currentQuotationId
      }}
      setters={{
        setSearchBranchId, setSearchStaffId, setProjectName, setCustomerName, setDiscount, setRemarks, setRows, setAttachedFile
      }}
      onBack={() => setMode('SEARCH')}
      onSave={handleSave}
    />
  );
}

function EditScreen({ currentUser, data, setters, onBack, onSave }: EditScreenProps) {
  const { date, estimateNo, searchBranchId, searchStaffId, projectName, customerName, discount, remarks, rows, attachedFile, currentQuotationId } = data;
  const { setSearchBranchId, setSearchStaffId, setProjectName, setCustomerName, setDiscount, setRemarks, setRows, setAttachedFile } = setters;

  const fileInputRef = useRef<HTMLInputElement>(null);
  const inputRefs = useRef<{ [key: string]: HTMLInputElement | HTMLSelectElement | null }>({});
  const remarksRef = useRef<HTMLTextAreaElement>(null);

  const [selectionStart, setSelectionStart] = useState<CellCoords | null>(null);
  const [selectionEnd, setSelectionEnd] = useState<CellCoords | null>(null);
  const [isSelecting, setIsSelecting] = useState(false);

  const filteredStaffs = useMemo(() => USERS.filter(u => u.branchId === searchBranchId), [searchBranchId]);
  const filteredCustomers = useMemo(() => searchStaffId === 0 ? [] : CUSTOMERS.filter(c => c.staffId === searchStaffId), [searchStaffId]);
  const userBranch = BRANCHES.find(b => b.id === currentUser.branchId);

  const subTotal = rows.filter((r:Row) => r.type === 'normal').reduce((acc:number, row:Row) => acc + (row.price * row.quantity), 0);
  const discountValue = typeof discount === 'string' ? 0 : discount;
  const mainTotal =subTotal-discountValue;
  const taxAmount = Math.floor(mainTotal * 0.1);
  const grandTotal = mainTotal + taxAmount;

  useEffect(() => {
    if (remarksRef.current) {
      remarksRef.current.style.height = 'auto'; 
      remarksRef.current.style.height = `${remarksRef.current.scrollHeight}px`; 
    }
  }, [remarks]);

  // ★修正: キー操作のロジック改善
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // 1. テキストエリア編集中は、ここでの制御を一切行わない（文字削除・改行などを優先）
      if (document.activeElement?.tagName === 'TEXTAREA') return;

      // 2. INPUT編集中で、かつ「範囲選択」をしていない場合は、ここでの制御を行わない（文字削除などを優先）
      const isInput = document.activeElement?.tagName === 'INPUT';
      const isRange = selectionStart && selectionEnd && (selectionStart.rowIndex !== selectionEnd.rowIndex || selectionStart.colKey !== selectionEnd.colKey);
      if (isInput && !isRange) return;

      // 3. 上記以外（セル移動モード、または範囲選択中）でDelete/BackSpaceが押されたらクリア処理
      if (!selectionStart || !selectionEnd) return;
      if (e.key === 'Delete' || e.key === 'Backspace') {
        e.preventDefault();
        clearSelection();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [selectionStart, selectionEnd, rows]);

  const handleGridKeyDown = (e: React.KeyboardEvent, rIndex: number, colKey: string) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      const nextKey = `${rIndex + 1}-${colKey}`;
      if (inputRefs.current[nextKey]) {
        inputRefs.current[nextKey]?.focus();
      }
    }
    else if (e.key === 'ArrowUp') {
      e.preventDefault();
      const prevKey = `${rIndex - 1}-${colKey}`;
      if (inputRefs.current[prevKey]) {
        inputRefs.current[prevKey]?.focus();
      }
    }
    else if (e.key === 'ArrowDown') {
      e.preventDefault();
      const nextKey = `${rIndex + 1}-${colKey}`;
      if (inputRefs.current[nextKey]) {
        inputRefs.current[nextKey]?.focus();
      }
    }
  };

  const calculateMargin = (price: number, cost: number) => (!price ? 0 : ((price - cost) / price) * 100);
  
  const handleInputChange = (id: number, field: keyof Row, value: string | number) => {
    setRows(rows.map((row:Row) => row.id === id ? { ...row, [field]: value } : row));
  };
  
  const handleNumberChange = (id: number, field: keyof Row, rawValue: string) => {
    const cleanValue = toHalfWidth(rawValue).replace(/,/g, '');
    if (cleanValue === '') {
      setRows(rows.map((row:Row) => row.id === id ? { ...row, [field]: 0 } : row));
    } else if (/^-?\d*$/.test(cleanValue)) {
      setRows(rows.map((row:Row) => row.id === id ? { ...row, [field]: Number(cleanValue) } : row));
    }
  };

  const handleDiscountChange = (rawValue: string) => {
    const cleanValue = toHalfWidth(rawValue).replace(/,/g, '');
    setDiscount(cleanValue === '' ? '' : Number(cleanValue));
  };

  const addRow = () => {
    const maxId = rows.length > 0 ? Math.max(...rows.map((r:Row) => r.id)) : 0;
    setRows([...rows, { id: maxId + 1, type: 'normal', code: '', manufacturer: '', item: '', quantity: 0, cost: 0, price: 0 }]);
  };

  const deleteRow = (id: number) => {
    setRows(rows.filter((r:Row) => r.id !== id));
  };

  const clearSelection = () => {
    if (!selectionStart || !selectionEnd) return;
    const colKeys: (keyof Row)[] = ['type', 'code', 'manufacturer', 'item', 'quantity', 'cost', 'price'];
    const startColIdx = colKeys.indexOf(selectionStart.colKey as keyof Row);
    const endColIdx = colKeys.indexOf(selectionEnd.colKey as keyof Row);
    const minRow = Math.min(selectionStart.rowIndex, selectionEnd.rowIndex);
    const maxRow = Math.max(selectionStart.rowIndex, selectionEnd.rowIndex);
    const minCol = Math.min(startColIdx, endColIdx);
    const maxCol = Math.max(startColIdx, endColIdx);

    const newRows = rows.map((row:Row, rIdx:number) => {
      if (rIdx >= minRow && rIdx <= maxRow) {
        const updatedRow = { ...row };
        for (let c = minCol; c <= maxCol; c++) {
          const key = colKeys[c];
          if (key === 'quantity' || key === 'cost' || key === 'price') updatedRow[key] = 0;
          else if (key === 'type') updatedRow[key] = 'normal';
          else (updatedRow as Record<string, string | number>)[key] = '';
        }
        return updatedRow;
      }
      return row;
    });
    setRows(newRows);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) setAttachedFile(e.target.files[0]);
  };

  const handlePaste = (e: React.ClipboardEvent<HTMLDivElement>, startRowIndex: number, startColKey: keyof Row) => {
    e.preventDefault();
    const pasteData = e.clipboardData.getData('text');
    const pasteRows = pasteData.split(/\r\n|\n|\r/).filter(row => row.trim() !== '');
    
    const colOrder: (keyof Row)[] = ['type', 'code', 'item', 'quantity', 'cost', 'price'];
    const startColIndex = colOrder.indexOf(startColKey);
    if (startColIndex === -1) return;

    const newRows = [...rows];
    pasteRows.forEach((rowStr, i) => {
      const targetRowIndex = startRowIndex + i;
      if (targetRowIndex >= newRows.length) {
         const maxId = newRows.length > 0 ? Math.max(...newRows.map(r => r.id)) : 0;
         newRows.push({ id: maxId + 1, type: 'normal', code: '', manufacturer: '', item: '', quantity: 0, cost: 0, price: 0 });
      }
      const cols = rowStr.split('\t');
      cols.forEach((colData, j) => {
        const targetColIndex = startColIndex + j;
        if (targetColIndex < colOrder.length) {
          const field = colOrder[targetColIndex];
          const currentRow = newRows[targetRowIndex];
          let val: string | number = colData.trim();
          if (field === 'quantity' || field === 'cost' || field === 'price') {
            val = Number(val.replace(/,/g, '')) || 0;
          }
          newRows[targetRowIndex] = { ...currentRow, [field]: val };
        }
      });
    });
    setRows(newRows);
  };

  const handleCopyAll = () => {
    const tsv = rows.map((row: Row) => {
      let itemText = row.item;
      if(row.type === 'manufacturer') itemText = row.manufacturer;
      return [row.code, itemText, row.quantity || 0, row.cost || 0, row.price || 0].join('\t');
    }).join('\n');
    navigator.clipboard.writeText(tsv).then(() => { alert('全データをクリップボードにコピーしました。'); });
  };

  // ★修正: 備考欄を含めたページネーション計算（精度向上版）
  const getPages = () => {
    const pages = [];
    let currentRow = 0;
    
    const remarksLineCount = remarks ? remarks.split('\n').length : 1;
    // 備考欄の高さ(px)を概算 (minHeight 120px)
    const remarksHeightPx = 10 + (Math.max(remarksLineCount * 18, 120)); 
    // 合計欄(約100px) + 備考欄
    const footerHeightPx = 100 + remarksHeightPx; 
    
    // 行の高さを22pxと仮定し、フッターに必要な「行数」を算出
    const footerRowsNeeded = Math.ceil(footerHeightPx / 22);

    const firstPageRows = rows.slice(0, ROWS_FIRST_PAGE);
    pages.push(firstPageRows);
    currentRow += ROWS_FIRST_PAGE;

    while (currentRow < rows.length) {
      pages.push(rows.slice(currentRow, currentRow + ROWS_OTHER_PAGES));
      currentRow += ROWS_OTHER_PAGES;
    }

    const lastPage = pages[pages.length - 1];
    const maxRowsOnLastPage = pages.length === 1 ? ROWS_FIRST_PAGE : ROWS_OTHER_PAGES;
    
    // 最終ページにフッターが入るか判定（余裕をもって+1行）
    if (lastPage.length + footerRowsNeeded + 1 > maxRowsOnLastPage) {
      pages.push([]); // 入らない場合は空ページを追加してそこへ送る
    }

    if (pages.length === 0) pages.push([]);
    return pages;
  };
  const pages = getPages();
  let globalItemIndex = 0;

  const renderCell = (rIndex: number, row: Row, colKey: keyof Row, content: React.ReactNode, extraStyle: React.CSSProperties = {}) => {
    const isSel = selectionStart && selectionEnd && 
      rIndex >= Math.min(selectionStart.rowIndex, selectionEnd.rowIndex) &&
      rIndex <= Math.max(selectionStart.rowIndex, selectionEnd.rowIndex) &&
      ['type', 'code', 'manufacturer', 'item', 'quantity', 'cost', 'price'].indexOf(colKey) >= Math.min(['type', 'code', 'manufacturer', 'item', 'quantity', 'cost', 'price'].indexOf(selectionStart.colKey), ['type', 'code', 'manufacturer', 'item', 'quantity', 'cost', 'price'].indexOf(selectionEnd.colKey)) &&
      ['type', 'code', 'manufacturer', 'item', 'quantity', 'cost', 'price'].indexOf(colKey) <= Math.max(['type', 'code', 'manufacturer', 'item', 'quantity', 'cost', 'price'].indexOf(selectionStart.colKey), ['type', 'code', 'manufacturer', 'item', 'quantity', 'cost', 'price'].indexOf(selectionEnd.colKey));

    return (
      <td 
        style={{...styles.gridTd, ...extraStyle, backgroundColor: isSel ? '#e3f2fd' : (extraStyle.backgroundColor || 'white'), border: isSel ? '2px solid #2196f3' : styles.gridTd.border}}
        onMouseDown={() => { setIsSelecting(true); setSelectionStart({ rowIndex: rIndex, colKey }); setSelectionEnd({ rowIndex: rIndex, colKey }); }}
        onMouseEnter={() => { if(isSelecting) setSelectionEnd({ rowIndex: rIndex, colKey }); }}
        onMouseUp={() => setIsSelecting(false)}
      >
        <div style={{width:'100%', height:'100%'}} onPaste={(e) => handlePaste(e, rIndex, colKey)}>{content}</div>
      </td>
    );
  };

  return (
    <div style={styles.container} onMouseUp={() => setIsSelecting(false)}>
      <style>{`
        @media print {
          @page { margin: 0; size: A4; }
          body { background-color: white !important; -webkit-print-color-adjust: exact; }
          .left-panel-print-hidden { display: none !important; }
          .right-panel-print-full { width: 100% !important; padding: 0 !important; background-color: white !important; overflow: visible !important; display: block !important; }
          .page-container-print { transform: none !important; margin: 0 !important; padding: 15mm 20mm !important; box-shadow: none !important; page-break-after: always; width: 100% !important; }
          .page-container-print:last-child { page-break-after: auto; }
          .no-print { display: none !important; }
        }
      `}</style>

      {/* --- 左パネル --- */}
      <div style={styles.leftPanel} className="left-panel-print-hidden">
        {/* ... (省略なし) ... */}
        <div style={styles.leftHeader}>
          <div style={{marginBottom:'10px'}}>
            <button style={styles.backBtn} onClick={onBack}>← 検索画面へ戻る</button>
            <span style={{fontWeight:'bold', fontSize:'1.1em'}}>{currentQuotationId ? `編集モード (ID: ${currentQuotationId})` : '新規作成モード'}</span>
          </div>
          <div style={styles.filterRow}>
            <div style={styles.filterGroup}>
              <label style={styles.labelSmall}>営業所</label>
              <select style={styles.headerSelect} value={searchBranchId} onChange={(e) => { setSearchBranchId(Number(e.target.value)); setSearchStaffId(0); setCustomerName(''); }}>
                {BRANCHES.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}
              </select>
            </div>
            <div style={styles.filterGroup}>
              <label style={styles.labelSmall}>担当者</label>
              <select style={styles.headerSelect} value={searchStaffId} onChange={(e) => { setSearchStaffId(Number(e.target.value)); setCustomerName(''); }}>
                <option value={0}>-- 選択 --</option>
                {filteredStaffs.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </div>
          </div>
          <div style={styles.filterRow}>
            <div style={styles.filterGroup}>
              <label style={styles.labelSmall}>得意先</label>
              <select 
                style={{...styles.customerSelect, backgroundColor: searchStaffId === 0 ? '#f0f0f0' : '#fff', cursor: searchStaffId === 0 ? 'not-allowed' : 'pointer', color: searchStaffId === 0 ? '#aaa' : '#2c3e50'}}
                value={customerName} disabled={searchStaffId === 0} onChange={(e) => { setCustomerName(e.target.value); }}
              >
                <option value="">{searchStaffId === 0 ? "← 先に担当者を選択" : "-- 得意先を選択 --"}</option>
                {filteredCustomers.map(c => <option key={c.id} value={c.name}>{c.name}</option>)}
              </select>
            </div>
            <div style={styles.filterGroup}>
              <label style={styles.labelSmall}>案件名 (自由入力)</label>
              <input 
                type="text"
                style={styles.projectInput} 
                placeholder="例：新規開業案件"
                value={projectName}
                onChange={(e) => setProjectName(e.target.value)}
              />
            </div>
          </div>

          <div style={styles.attachArea}>
            <input type="file" accept="image/*,application/pdf" ref={fileInputRef} style={{display:'none'}} onChange={handleFileChange} />
            <button style={styles.attachBtn} onClick={() => fileInputRef.current?.click()}>📎 仕入見積添付</button>
            <span style={styles.fileName}>{attachedFile ? `📄 ${attachedFile.name}` : '(未選択)'}</span>
            <button style={styles.ocrBtn} title="将来機能: 画像から文字を自動読取">
              [OCR] 自動読取 (未実装)
            </button>
          </div>
        </div>

        <div id="grid-container" style={styles.gridContainer}>
          <table style={styles.gridTable}>
            <thead>
              <tr>
                <th style={{...styles.gridTh, width: '30px'}}></th>
                <th style={{...styles.gridTh, width: '60px'}}>種別</th>
                <th style={{...styles.gridTh, width: '70px'}}>CD</th>
                <th style={{...styles.gridTh, minWidth: '250px'}}>
                  品名・規格
                  <button style={styles.copyBtn} onClick={handleCopyAll} title="データをコピー">📋コピー</button>
                </th>
                <th style={{...styles.gridTh, width: '50px'}}>数量</th>
                <th style={{...styles.gridTh, width: '70px', color:'#ff9999'}}>仕切価</th>
                <th style={{...styles.gridTh, width: '70px', color:'#99ccff'}}>単価</th>
                <th style={{...styles.gridTh, width: '80px', color:'#27ae60'}}>金額</th>
                <th style={{...styles.gridTh, width: '50px'}}>利益率%</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row: Row, rIndex: number) => {
                const margin = calculateMargin(row.price, row.cost);
                const isNegative = margin < 0;
                const isInputEnabled = row.type === 'normal' || row.type === 'detail';
                const rowAmount = row.price * row.quantity;
                return (
                  <tr key={row.id}>
                    <td style={{...styles.gridTd, textAlign:'center'}}>
                      <button onClick={() => deleteRow(row.id)} style={{border:'none', background:'transparent', color:'#ccc', cursor:'pointer'}}>×</button>
                    </td>
                    {renderCell(rIndex, row, 'type', 
                      <select 
                        ref={(el) => { inputRefs.current[`${rIndex}-type`] = el; }}
                        onKeyDown={(e) => handleGridKeyDown(e, rIndex, 'type')}
                        style={styles.typeSelect} value={row.type} onChange={(e) => handleInputChange(row.id, 'type', e.target.value)}
                      >
                        <option value="normal">通常</option><option value="manufacturer">メーカー</option><option value="detail">明細</option><option value="note">注釈</option>
                      </select>
                    )}
                    {renderCell(rIndex, row, 'code', isInputEnabled ? 
                      <input 
                        ref={(el) => { inputRefs.current[`${rIndex}-code`] = el; }}
                        onKeyDown={(e) => handleGridKeyDown(e, rIndex, 'code')}
                        placeholder="CD" style={styles.smallInput} value={row.code} onChange={e => handleInputChange(row.id, 'code', e.target.value)} onFocus={(e)=>e.target.select()} 
                      /> : null
                    )}
                    {renderCell(rIndex, row, 'item', row.type === 'manufacturer' ? 
                      <input 
                        ref={(el) => { inputRefs.current[`${rIndex}-item`] = el; }}
                        onKeyDown={(e) => handleGridKeyDown(e, rIndex, 'item')}
                        placeholder="メーカー名" style={{...styles.smallInput, fontWeight:'bold', backgroundColor: '#fffbe6'}} value={row.manufacturer} onChange={e => handleInputChange(row.id, 'manufacturer', e.target.value)} onFocus={(e)=>e.target.select()} 
                      /> : 
                      <input 
                        ref={(el) => { inputRefs.current[`${rIndex}-item`] = el; }}
                        onKeyDown={(e) => handleGridKeyDown(e, rIndex, 'item')}
                        placeholder="品名" style={styles.smallInput} value={row.item} onChange={e => handleInputChange(row.id, 'item', e.target.value)} onFocus={(e)=>e.target.select()} 
                      />
                    )}
                    {renderCell(rIndex, row, 'quantity', isInputEnabled && 
                      <input 
                        ref={(el) => { inputRefs.current[`${rIndex}-quantity`] = el; }}
                        onKeyDown={(e) => handleGridKeyDown(e, rIndex, 'quantity')}
                        type="text" style={{...styles.smallInput, textAlign:'right'}} value={row.quantity === 0 ? '' : row.quantity} onChange={e => handleNumberChange(row.id, 'quantity', e.target.value)} onFocus={(e)=>e.target.select()} 
                      />
                    )}
                    {renderCell(rIndex, row, 'cost', isInputEnabled && 
                      <input 
                        ref={(el) => { inputRefs.current[`${rIndex}-cost`] = el; }}
                        onKeyDown={(e) => handleGridKeyDown(e, rIndex, 'cost')}
                        type="text" style={{...styles.smallInput, textAlign:'right', backgroundColor: '#fff5f5'}} value={row.cost === 0 ? '' : row.cost} onChange={e => handleNumberChange(row.id, 'cost', e.target.value)} onFocus={(e)=>e.target.select()} 
                      />
                    )}
                    {renderCell(rIndex, row, 'price', isInputEnabled && 
                      <input 
                        ref={(el) => { inputRefs.current[`${rIndex}-price`] = el; }}
                        onKeyDown={(e) => handleGridKeyDown(e, rIndex, 'price')}
                        type="text" style={{...styles.smallInput, textAlign:'right', backgroundColor: '#f0f9ff'}} value={row.price === 0 ? '' : row.price} onChange={e => handleNumberChange(row.id, 'price', e.target.value)} onFocus={(e)=>e.target.select()} 
                      />
                    )}
                    <td style={styles.amountCell}>
                      {isInputEnabled && row.price > 0 && row.quantity > 0 ? rowAmount.toLocaleString() : ''}
                    </td>
                    <td style={{...styles.gridTd, ...styles.profitCell, color: isNegative ? 'red' : 'black', fontWeight: isNegative ? 'bold' : 'normal'}}>
                      {isInputEnabled && row.price > 0 ? `${margin.toFixed(0)}` : ''}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          <button onClick={addRow} style={{marginTop:'10px', width:'100%', padding:'10px', backgroundColor:'#ecf0f1', border:'1px dashed #bdc3c7', cursor:'pointer', color:'#7f8c8d'}}>＋ 行を追加</button>
        </div>

        <div style={styles.remarksInputArea}>
          <div style={{fontSize:'0.85em', fontWeight:'bold', color:'#555', marginBottom:'3px'}}>備考 (入力・編集)</div>
          <textarea 
            ref={remarksRef}
            style={styles.remarksInput} 
            placeholder="ここに備考を入力 (コピペ可)" 
            value={remarks}
            onChange={(e) => setRemarks(e.target.value)}
          />
        </div>

        <div style={styles.leftFooter}>
          <div style={styles.footerBtns}>
            <button style={styles.footerActionBtn} onClick={() => onSave(true)}>修正保存</button>
            <button style={{...styles.footerActionBtn, backgroundColor:'#3498db'}} onClick={() => onSave(false)}>新規保存</button>
            <button style={{...styles.footerActionBtn, backgroundColor:'#95a5a6'}} onClick={() => window.print()}>🖨️ 印刷</button>
          </div>
          <div style={styles.calcContainer}>
            <div style={styles.calcItem}>
              <span style={styles.footerLabel}>小計</span>
              <span style={styles.footerValue}>{subTotal.toLocaleString()}</span>
            </div>
            <div style={styles.calcItem}>
              <span style={styles.footerLabel}>値引</span>
              <input type="text" style={styles.discountInput} value={discount} onChange={(e) => handleDiscountChange(e.target.value)} onFocus={(e)=>e.target.select()} placeholder="" />
            </div>
            <div style={{...styles.calcItem, borderLeft:'1px solid #999', paddingLeft:'20px'}}>
              <span style={styles.footerLabel}>合計</span>
              <span style={{...styles.footerValue, color:'#2ecc71', fontSize:'1.5em'}}>¥{mainTotal.toLocaleString()}</span>
            </div>
          </div>
        </div>
      </div>

      {/* --- 右パネル (プレビュー) --- */}
      <div style={styles.rightPanel} className="right-panel-print-full">
        {pages.map((pageRows, pageIndex) => {
          const isFirstPage = pageIndex === 0;
          const isLastPage = pageIndex === pages.length - 1;
          return (
            <div key={pageIndex} style={styles.pageContainer} className="page-container-print">
              {isFirstPage ? (
                <>
                  <h1 style={styles.headerTitle}>御 見 積 書</h1>
                  <div style={styles.topSection}>
                    <div style={styles.customerInfo}>
                      <div style={{display:'flex', alignItems: 'flex-end', marginBottom:'10px', borderBottom:'1px solid #333', minHeight:'40px'}}>
                         <span style={{ fontSize: '1.2em', width:'100%', fontWeight:'bold', whiteSpace:'nowrap', overflow:'visible' }}>{customerName}</span>
                         <span style={{fontSize: '1.2em', marginLeft:'10px', whiteSpace:'nowrap'}}>御中</span>
                      </div>
                      <p style={{fontSize: '0.9em'}}>ご照会賜りました件につきまして、<br/>下記の通り御見積り致します。</p>
                      <div style={styles.summaryBox}>
                        <div style={styles.summaryRow}><span>ご提供価格 :</span><span>{mainTotal.toLocaleString()}</span></div>
                        <div style={styles.summaryRow}><span>消費税 (10%) :</span><span>{taxAmount.toLocaleString()}</span></div>
                        <div style={styles.totalRow}><span>{'総\u3000\u3000額 :'}</span><span>¥{grandTotal.toLocaleString()}</span></div>
                      </div>
                    </div>
                    <div style={styles.companyInfo}>
                      見積No: {estimateNo}<br/>日付: {date}<br/><br/>
                      <strong>株式会社セイエル</strong><br/>{userBranch?.name}<br/>{userBranch?.address}<br/>TEL: {userBranch?.phone}<br/>
                      <div style={{marginTop:'5px', paddingTop:'2px'}}>担当: {currentUser.name}</div>
                    </div>
                  </div>
                </>
              ) : (
                <div style={{textAlign:'left', fontSize:'0.8em', fontStyle:'italic', marginBottom:'10px', borderBottom:'1px dashed #ccc'}}>見積No: {estimateNo} / {customerName} 様 （前ページより）</div>
              )}

              <table style={styles.table}>
                <colgroup><col style={{width: '35px'}} /><col style={{width: 'auto'}} /><col style={{width: '50px'}} /><col style={{width: '110px'}} /><col style={{width: '130px'}} /></colgroup>
                <thead><tr><th style={styles.th}>項</th><th style={styles.th}>品名・規格</th><th style={styles.th}>数量</th><th style={styles.th}>単価</th><th style={styles.th}>金額</th></tr></thead>
                <tbody>
                  {pageRows.map((row) => {
                    let displayIndex = null;
                    let displayText = row.item;
                    let displayClass: React.CSSProperties = {};
                    if (row.type === 'manufacturer') {
                      displayText = `【メーカー: ${row.manufacturer}】`;
                      displayClass = { fontWeight: 'bold' };
                    } else if (row.type === 'detail') {
                      displayText = `\u3000└ ${row.item}`;
                      displayClass = { fontSize: '0.85em', color: '#555' }; 
                    }
                    if (row.type === 'normal') {
                      globalItemIndex++;
                      displayIndex = globalItemIndex;
                    }
                    const isPrintValueRow = row.type === 'normal';
                    return (
                      <tr key={row.id}>
                        <td style={{...styles.td, textAlign: 'center', backgroundColor: '#f9f9f9'}}>{displayIndex}</td>
                        <td style={{...styles.td, textAlign: 'left', ...displayClass}}>{displayText}</td>
                        <td style={{...styles.td, textAlign: 'right'}}>{isPrintValueRow && row.quantity > 0 ? row.quantity : ''}</td>
                        <td style={{...styles.td, textAlign: 'right'}}>{isPrintValueRow && row.price > 0 ? row.price.toLocaleString() : ''}</td>
                        <td style={{...styles.td, textAlign: 'right', backgroundColor: '#fcfcfc'}}>{isPrintValueRow && row.price > 0 ? (row.quantity * row.price).toLocaleString() : ''}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>

              {!isLastPage ? (
                <div style={{textAlign: 'right', fontSize: '0.8em', fontStyle: 'italic', marginTop: '5px', borderTop: '1px dashed #ccc'}}>-- 次ページへ続く --</div>
              ) : (
                <div style={styles.footerArea}>
                  <table style={styles.footerTable}>
                    <colgroup><col style={{width: '40%'}} /><col style={{width: '60%'}} /></colgroup>
                    <tbody>
                      <tr><td style={{border: '1px solid #000', padding: '5px', backgroundColor:'#f0f0f0'}}>小計</td><td style={{border: '1px solid #000', padding: '5px', textAlign: 'right'}}>{subTotal.toLocaleString()}</td></tr>
                      <tr><td style={{border: '1px solid #000', padding: '5px', backgroundColor:'#f0f0f0'}}>値引き</td><td style={{border: '1px solid #000', padding: '5px', textAlign: 'right'}}>{discountValue > 0 ? `-${discountValue.toLocaleString()}` : '-'}</td></tr>
                      <tr><td style={{border: '1px solid #000', padding: '5px', backgroundColor:'#f0f0f0', fontWeight: 'bold'}}>本体価計</td><td style={{border: '1px solid #000', padding: '5px', textAlign: 'right', fontWeight: 'bold'}}>¥{mainTotal.toLocaleString()}</td></tr>
                    </tbody>
                  </table>
                  <div style={{fontSize: '0.9em', fontWeight: 'bold'}}>備考</div>
                  <div style={{...styles.remarksBox, border:'1px solid #000', height:'auto', minHeight:'120px'}}>
                    {remarks}
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}