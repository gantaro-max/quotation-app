import React, { useEffect, useMemo, useState } from 'react';

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

// --- 設定 ---
const ROWS_FIRST_PAGE = 13;
const ROWS_OTHER_PAGES = 22;

// --- ユーティリティ: 全角数字を半角に変換 ---
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
  
  leftPanel: { width: '50%', flexShrink: 0, backgroundColor: '#f4f6f9', borderRadius: '4px', display: 'flex', flexDirection: 'column', height: '100%', boxSizing: 'border-box', boxShadow: '0 0 10px rgba(0,0,0,0.3)' },
  leftHeader: { padding: '15px', backgroundColor: 'white', borderBottom: '1px solid #ddd', borderTopLeftRadius: '4px', borderTopRightRadius: '4px', flexShrink: 0 },
  filterRow: { display: 'flex', gap: '10px', marginBottom: '10px' },
  filterGroup: { flex: 1, display: 'flex', flexDirection: 'column' },
  labelSmall: { fontSize: '0.8em', color: '#666', marginBottom: '3px', fontWeight: 'bold' },
  headerSelect: { padding: '8px', fontSize: '0.9em', borderRadius: '4px', border: '1px solid #ccc', width: '100%', backgroundColor: '#fff' },
  customerSelect: { padding: '8px', fontSize: '1em', borderRadius: '4px', border: '1px solid #ccc', width: '100%', fontWeight: 'bold', color: '#2c3e50' },

  gridContainer: { flexGrow: 1, overflow: 'auto', padding: '0', position: 'relative', backgroundColor: '#fff' },
  gridTable: { width: '100%', borderCollapse: 'collapse', fontSize: '0.85em', backgroundColor: 'white' },
  gridTh: { backgroundColor: '#34495e', color: 'white', padding: '10px 5px', border: '1px solid #2c3e50', textAlign: 'center', whiteSpace: 'nowrap', position: 'sticky', top: 0, zIndex: 100, boxShadow: '0 2px 2px rgba(0,0,0,0.1)' },
  gridTd: { border: '1px solid #dee2e6', padding: '0', verticalAlign: 'middle', backgroundColor: 'white' },

  leftFooter: { padding: '15px 20px', backgroundColor: '#2c3e50', color: 'white', borderTop: '1px solid #ccc', borderBottomLeftRadius: '4px', borderBottomRightRadius: '4px', flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'space-between' },
  calcContainer: { display: 'flex', alignItems: 'center', gap: '20px', backgroundColor: 'rgba(0,0,0,0.2)', padding: '5px 15px', borderRadius: '4px' },
  calcItem: { display: 'flex', alignItems: 'center', gap: '10px' },
  footerLabel: { fontSize: '0.9em', color: '#bdc3c7', whiteSpace: 'nowrap' },
  footerValue: { fontSize: '1.2em', fontWeight: 'bold', minWidth: '80px', textAlign: 'right' as const },
  discountInput: { width: '120px', padding: '5px', textAlign: 'right', borderRadius: '4px', border: 'none', fontWeight: 'bold', fontSize: '1.1em' },
  footerPrintBtn: { padding: '10px 30px', backgroundColor: '#e74c3c', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold', fontSize: '1em', display: 'flex', alignItems: 'center', gap: '5px', boxShadow: '0 2px 5px rgba(0,0,0,0.3)' },

  smallInput: { width: '100%', height: '100%', border: 'none', padding: '8px', boxSizing: 'border-box', outline: 'none', fontSize: '1em', background: 'transparent' },
  typeSelect: { width: '100%', border: 'none', padding: '8px', fontSize: '0.9em', cursor: 'pointer', outline: 'none', background: 'transparent' },
  profitCell: { padding: '0 8px', textAlign: 'right', verticalAlign: 'middle' },
  
  rightPanel: { width: '50%', flexShrink: 0, overflowY: 'auto', display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '0 10px' },
  pageContainer: { width: '210mm', minHeight: '297mm', backgroundColor: 'white', padding: '15mm 20mm', boxSizing: 'border-box', marginBottom: '20px', position: 'relative', fontFamily: '"MS Mincho", "Hiragino Mincho ProN", serif', color: '#333', transform: 'scale(0.85)', transformOrigin: 'top center', boxShadow: '0 5px 15px rgba(0,0,0,0.5)', marginTop: '20px' },
  headerTitle: { textAlign: 'center', fontSize: '1.8em', textDecoration: 'underline', marginBottom: '10px', letterSpacing: '0.3em' },
  topSection: { display: 'flex', justifyContent: 'space-between', marginBottom: '15px', alignItems: 'flex-start' },
  customerInfo: { width: '58%' },
  companyInfo: { width: '40%', fontSize: '0.85em', lineHeight: '1.4', textAlign: 'right' },
  summaryBox: { marginTop: '10px', borderBottom: '2px solid #000', paddingBottom: '5px', width: '95%' },
  summaryRow: { display: 'flex', justifyContent: 'space-between', padding: '1px 0', borderBottom: '1px solid #ccc' },
  totalRow: { display: 'flex', justifyContent: 'space-between', padding: '3px 0', fontWeight: 'bold', fontSize: '1.1em', borderBottom: 'none' },
  table: { width: '100%', borderCollapse: 'collapse', marginTop: '5px', marginBottom: '5px', fontSize: '0.9em', tableLayout: 'fixed' },
  th: { border: '1px solid #000', padding: '2px', backgroundColor: '#f0f0f0', textAlign: 'center', fontWeight: 'bold', height: '25px', fontSize: '0.9em' },
  td: { border: '1px solid #000', padding: '0 5px', height: '28px', verticalAlign: 'middle' },
  footerArea: { marginTop: 'auto', breakInside: 'avoid' },
  footerTable: { width: '50%', marginLeft: 'auto', borderCollapse: 'collapse', marginBottom: '5px' },
  remarksBox: { border: '1px solid #000', padding: '5px', height: '100px', marginTop: '2px', width: '100%', whiteSpace: 'pre-wrap', fontSize: '0.9em', lineHeight: '1.2' },
  
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

  return <MainApp currentUser={currentUser} />;
}

function MainApp({ currentUser }: { currentUser: User }) {
  const [date, setDate] = useState('');
  const [estimateNo, setEstimateNo] = useState('');
  const [searchBranchId, setSearchBranchId] = useState(1);
  const [searchStaffId, setSearchStaffId] = useState(0);
  const [customerName, setCustomerName] = useState('');
  const [discount, setDiscount] = useState<number | string>(''); 
  const [rows, setRows] = useState<Row[]>(
    Array.from({ length: 20 }, (_, i) => ({ id: i + 1, type: 'normal', code: '', manufacturer: '', item: '', quantity: 0, cost: 0, price: 0 }))
  );

  const [selectionStart, setSelectionStart] = useState<CellCoords | null>(null);
  const [selectionEnd, setSelectionEnd] = useState<CellCoords | null>(null);
  const [isSelecting, setIsSelecting] = useState(false);

  useEffect(() => {
    const now = new Date();
    setDate(`${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`);
    setEstimateNo(now.getFullYear().toString().slice(-2) + (now.getMonth() + 1).toString().padStart(2, '0') + now.getDate().toString().padStart(2, '0') + "001");
    setSearchBranchId(currentUser.branchId);
    setSearchStaffId(currentUser.id);
  }, [currentUser]);

  // ★修正: キーボードイベント (Delete対応)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // 選択範囲がなければ何もしない
      if (!selectionStart || !selectionEnd) return;

      const isDelete = e.key === 'Delete';
      const isBackspace = e.key === 'Backspace';

      if (isDelete || isBackspace) {
        // 範囲選択かどうか (開始と終了が違うなら範囲)
        const isRangeSelection = (selectionStart.rowIndex !== selectionEnd.rowIndex) || (selectionStart.colKey !== selectionEnd.colKey);
        
        const activeTag = document.activeElement?.tagName;
        const isInput = activeTag === 'INPUT';

        // ★ロジック:
        // 1. 範囲選択中なら、DeleteでもBackspaceでも消す (エクセルと同じ)
        // 2. 単一選択中の場合:
        //    - Backspace: input編集中なら、文字編集を優先する (returnしてネイティブ動作)
        //    - Delete: input編集中でも、セル全体をクリアする (エクセルでセル選択してDelete押した挙動を再現)
        
        if (!isRangeSelection && isInput && isBackspace) {
           return; // 文字消去（編集）を許可
        }

        // ここに来たら「消去」を実行
        e.preventDefault();
        clearSelection();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [selectionStart, selectionEnd, rows]);

  const filteredStaffs = useMemo(() => USERS.filter(u => u.branchId === searchBranchId), [searchBranchId]);
  const filteredCustomers = useMemo(() => {
    if (searchStaffId === 0) return [];
    return CUSTOMERS.filter(c => c.staffId === searchStaffId);
  }, [searchStaffId]);

  const userBranch = BRANCHES.find(b => b.id === currentUser.branchId);
  const subTotal = rows.filter(r => r.type === 'normal').reduce((acc, row) => acc + (row.price * row.quantity), 0);
  const taxAmount = Math.floor(subTotal * 0.1);
  const discountValue = typeof discount === 'string' ? 0 : discount;
  const grandTotal = subTotal + taxAmount - discountValue;

  const calculateMargin = (price: number, cost: number) => {
    if (!price || price === 0) return 0;
    return ((price - cost) / price) * 100;
  };

  const handleInputChange = (id: number, field: keyof Row, value: string | number) => {
    setRows(rows.map(row => row.id === id ? { ...row, [field]: value } : row));
  };

  const handleNumberChange = (id: number, field: keyof Row, rawValue: string) => {
    const cleanValue = toHalfWidth(rawValue).replace(/,/g, '');
    if (cleanValue === '') {
      setRows(rows.map(row => row.id === id ? { ...row, [field]: 0 } : row));
    } else if (/^-?\d*$/.test(cleanValue)) {
      const numValue = Number(cleanValue);
      setRows(rows.map(row => row.id === id ? { ...row, [field]: numValue } : row));
    }
  };

  const handleDiscountChange = (rawValue: string) => {
    const cleanValue = toHalfWidth(rawValue).replace(/,/g, '');
    if (cleanValue === '') {
      setDiscount('');
    } else if (/^\d*$/.test(cleanValue)) {
      setDiscount(Number(cleanValue));
    }
  };

  // ★追加: フォーカス時に全選択（一発書き換え・一発消去用）
  const handleFocus = (e: React.FocusEvent<HTMLInputElement>) => {
    e.target.select();
  };

  const addRow = () => {
    const maxId = rows.length > 0 ? Math.max(...rows.map(r => r.id)) : 0;
    setRows([...rows, { id: maxId + 1, type: 'normal', code: '', manufacturer: '', item: '', quantity: 0, cost: 0, price: 0 }]);
    setTimeout(() => { const grid = document.getElementById('grid-container'); if (grid) grid.scrollTop = grid.scrollHeight; }, 50);
  };

  const deleteRow = (id: number) => {
    setRows(rows.filter(r => r.id !== id));
  };

  const handlePaste = (e: React.ClipboardEvent<HTMLElement>, startRowIndex: number, startColKey: keyof Row) => {
    e.preventDefault();
    const pasteData = e.clipboardData.getData('text');
    const pasteRows = pasteData.split(/\r\n|\n|\r/).filter(row => row.trim() !== '');
    
    // 貼り付け順序: type, code, item, quantity, cost, price
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
    const tsv = rows.map(row => {
      let itemText = row.item;
      if(row.type === 'manufacturer') itemText = row.manufacturer;
      return [row.code, itemText, row.quantity || 0, row.cost || 0, row.price || 0].join('\t');
    }).join('\n');
    navigator.clipboard.writeText(tsv).then(() => { alert('全データをクリップボードにコピーしました。'); });
  };

  // --- 選択範囲管理 ---
  const handleMouseDown = (rowIndex: number, colKey: string) => {
    setIsSelecting(true);
    setSelectionStart({ rowIndex, colKey });
    setSelectionEnd({ rowIndex, colKey });
    // 入力要素以外をクリックした時にフォーカスを外す（必要であれば）
  };

  const handleMouseEnter = (rowIndex: number, colKey: string) => {
    if (isSelecting) { setSelectionEnd({ rowIndex, colKey }); }
  };

  const handleMouseUp = () => { setIsSelecting(false); };

  const isSelected = (rowIndex: number, colKey: string) => {
    if (!selectionStart || !selectionEnd) return false;
    const colKeys = ['type', 'code', 'manufacturer', 'item', 'quantity', 'cost', 'price'];
    const startColIdx = colKeys.indexOf(selectionStart.colKey);
    const endColIdx = colKeys.indexOf(selectionEnd.colKey);
    const currentColIdx = colKeys.indexOf(colKey);
    const minRow = Math.min(selectionStart.rowIndex, selectionEnd.rowIndex);
    const maxRow = Math.max(selectionStart.rowIndex, selectionEnd.rowIndex);
    const minCol = Math.min(startColIdx, endColIdx);
    const maxCol = Math.max(startColIdx, endColIdx);
    return rowIndex >= minRow && rowIndex <= maxRow && currentColIdx >= minCol && currentColIdx <= maxCol;
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

    const newRows = rows.map((row, rIdx) => {
      if (rIdx >= minRow && rIdx <= maxRow) {
        const updatedRow = { ...row };
        for (let c = minCol; c <= maxCol; c++) {
          const key = colKeys[c];
          if (key === 'quantity' || key === 'cost' || key === 'price') { updatedRow[key] = 0; } 
          else if (key === 'type') { updatedRow[key] = 'normal'; } 
          else { (updatedRow as Record<string, string | number>)[key] = ''; }
        }
        return updatedRow;
      }
      return row;
    });
    setRows(newRows);
    // クリア後は選択解除しない（連続操作のため）
  };

  const getCustomerNameFontSize = (text: string) => {
    const len = text.length;
    if (len > 35) return '0.7em';
    if (len > 30) return '0.8em';
    if (len > 25) return '0.9em';
    if (len > 20) return '1.0em';
    if (len > 15) return '1.2em';
    return '1.4em';
  };

  const getPages = () => {
    const pages = [];
    let currentRow = 0;
    pages.push(rows.slice(0, ROWS_FIRST_PAGE));
    currentRow += ROWS_FIRST_PAGE;
    while (currentRow < rows.length) {
      pages.push(rows.slice(currentRow, currentRow + ROWS_OTHER_PAGES));
      currentRow += ROWS_OTHER_PAGES;
    }
    if (pages.length === 0) pages.push([]);
    return pages;
  };
  const pages = getPages();
  let globalItemIndex = 0;

  // セルレンダリング用ヘルパー
  const renderCell = (rIndex: number, row: Row, colKey: keyof Row, content: React.ReactNode, extraStyle: React.CSSProperties = {}) => {
    const selected = isSelected(rIndex, colKey);
    return (
      <td 
        style={{
          ...styles.gridTd, ...extraStyle, 
          backgroundColor: selected ? '#e3f2fd' : (extraStyle.backgroundColor || 'white'),
          border: selected ? '2px solid #2196f3' : styles.gridTd.border // 選択時に枠線強調
        }}
        onMouseDown={() => handleMouseDown(rIndex, colKey)}
        onMouseEnter={() => handleMouseEnter(rIndex, colKey)}
      >
        <div style={{width:'100%', height:'100%'}} onPaste={(e: React.ClipboardEvent<HTMLDivElement>) => handlePaste(e, rIndex, colKey)}>
          {content}
        </div>
      </td>
    );
  };

  return (
    <div style={styles.container} onMouseUp={handleMouseUp}>
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
        <div style={styles.leftHeader}>
          <div style={styles.filterRow}>
            <div style={styles.filterGroup}>
              <label style={styles.labelSmall}>営業所 (必須)</label>
              <select style={styles.headerSelect} value={searchBranchId} onChange={(e) => { setSearchBranchId(Number(e.target.value)); setSearchStaffId(0); setCustomerName(''); }}>
                {BRANCHES.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}
              </select>
            </div>
            <div style={styles.filterGroup}>
              <label style={styles.labelSmall}>担当者 (必須)</label>
              <select style={styles.headerSelect} value={searchStaffId} onChange={(e) => { setSearchStaffId(Number(e.target.value)); setCustomerName(''); }}>
                <option value={0}>-- 選択 --</option>
                {filteredStaffs.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </div>
          </div>
          <div style={styles.filterGroup}>
            <label style={styles.labelSmall}>得意先</label>
            <select 
              style={{...styles.customerSelect, backgroundColor: searchStaffId === 0 ? '#f0f0f0' : '#fff', cursor: searchStaffId === 0 ? 'not-allowed' : 'pointer', color: searchStaffId === 0 ? '#aaa' : '#2c3e50'}}
              value={customerName} disabled={searchStaffId === 0} onChange={(e) => setCustomerName(e.target.value)}
            >
              <option value="">{searchStaffId === 0 ? "← 先に担当者を選択してください" : "-- 得意先を選択してください --"}</option>
              {filteredCustomers.map(c => <option key={c.id} value={c.name}>{c.name}</option>)}
            </select>
          </div>
        </div>

        <div id="grid-container" style={styles.gridContainer}>
          <table style={styles.gridTable}>
            <thead>
              <tr>
                <th style={{...styles.gridTh, width: '30px'}}></th>
                <th style={{...styles.gridTh, width: '60px'}}>種別</th>
                <th style={{...styles.gridTh, width: '70px'}}>CD</th>
                <th style={{...styles.gridTh, minWidth: '300px'}}>
                  品名・規格
                  <button style={styles.copyBtn} onClick={handleCopyAll} title="データをコピー">📋コピー</button>
                </th>
                <th style={{...styles.gridTh, width: '50px'}}>数量</th>
                <th style={{...styles.gridTh, width: '70px', color:'#ff9999'}}>原価</th>
                <th style={{...styles.gridTh, width: '70px', color:'#99ccff'}}>売価</th>
                <th style={{...styles.gridTh, width: '50px'}}>利益率%</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row, rIndex) => {
                const margin = calculateMargin(row.price, row.cost);
                const isNegative = margin < 0;
                const isInputEnabled = row.type === 'normal' || row.type === 'detail';
                return (
                  <tr key={row.id}>
                    <td style={{...styles.gridTd, textAlign:'center'}}>
                      <button onClick={() => deleteRow(row.id)} style={{border:'none', background:'transparent', color:'#ccc', cursor:'pointer'}}>×</button>
                    </td>
                    {renderCell(rIndex, row, 'type', 
                      <select style={styles.typeSelect} value={row.type} onChange={(e) => handleInputChange(row.id, 'type', e.target.value)}>
                        <option value="normal">通常</option>
                        <option value="manufacturer">メーカー</option>
                        <option value="detail">明細</option>
                        <option value="note">注釈</option>
                      </select>
                    )}
                    {renderCell(rIndex, row, 'code', isInputEnabled ? <input placeholder="CD" style={styles.smallInput} value={row.code} onChange={e => handleInputChange(row.id, 'code', e.target.value)} onFocus={handleFocus} /> : null)}
                    {renderCell(rIndex, row, 'item', 
                      row.type === 'manufacturer' ? (
                         <input placeholder="メーカー名" style={{...styles.smallInput, fontWeight:'bold', backgroundColor: isSelected(rIndex,'manufacturer')?'transparent':'#fffbe6'}} value={row.manufacturer} onChange={e => handleInputChange(row.id, 'manufacturer', e.target.value)} onFocus={handleFocus} />
                      ) : (
                         <input placeholder="品名" style={styles.smallInput} value={row.item} onChange={e => handleInputChange(row.id, 'item', e.target.value)} onFocus={handleFocus} />
                      )
                    )}
                    {renderCell(rIndex, row, 'quantity', isInputEnabled && <input type="text" style={{...styles.smallInput, textAlign:'right'}} value={row.quantity === 0 ? '' : row.quantity} onChange={e => handleNumberChange(row.id, 'quantity', e.target.value)} onFocus={handleFocus} />)}
                    {renderCell(rIndex, row, 'cost', isInputEnabled && <input type="text" style={{...styles.smallInput, textAlign:'right', backgroundColor: isSelected(rIndex,'cost')?'transparent':'#fff5f5'}} value={row.cost === 0 ? '' : row.cost} onChange={e => handleNumberChange(row.id, 'cost', e.target.value)} onFocus={handleFocus} />)}
                    {renderCell(rIndex, row, 'price', isInputEnabled && <input type="text" style={{...styles.smallInput, textAlign:'right', backgroundColor: isSelected(rIndex,'price')?'transparent':'#f0f9ff'}} value={row.price === 0 ? '' : row.price} onChange={e => handleNumberChange(row.id, 'price', e.target.value)} onFocus={handleFocus} />)}
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

        <div style={styles.leftFooter}>
          <button style={styles.footerPrintBtn} onClick={() => window.print()}>🖨️ 印刷</button>
          <div style={styles.calcContainer}>
            <div style={styles.calcItem}>
              <span style={styles.footerLabel}>小計</span>
              <span style={styles.footerValue}>{subTotal.toLocaleString()}</span>
            </div>
            <div style={styles.calcItem}>
              <span style={styles.footerLabel}>値引</span>
              <input type="text" style={styles.discountInput} value={discount} onChange={(e) => handleDiscountChange(e.target.value)} onFocus={handleFocus} placeholder="0" />
            </div>
            <div style={{...styles.calcItem, borderLeft:'1px solid #999', paddingLeft:'20px'}}>
              <span style={styles.footerLabel}>合計</span>
              <span style={{...styles.footerValue, color:'#2ecc71', fontSize:'1.5em'}}>{grandTotal.toLocaleString()}</span>
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
              {/* (プレビュー内容は変更なし) */}
              {isFirstPage ? (
                <>
                  <h1 style={styles.headerTitle}>御 見 積 書</h1>
                  <div style={styles.topSection}>
                    <div style={styles.customerInfo}>
                      <div style={{display:'flex', alignItems: 'flex-end', marginBottom:'10px', borderBottom:'1px solid #333', minHeight:'40px'}}>
                         <span style={{ fontSize: getCustomerNameFontSize(customerName), width:'100%', fontWeight:'bold', whiteSpace:'nowrap', overflow:'visible' }}>{customerName}</span>
                         <span style={{fontSize: '1.2em', marginLeft:'10px', whiteSpace:'nowrap'}}>御中</span>
                      </div>
                      <p style={{fontSize: '0.9em'}}>ご照会賜りました件につきまして、<br/>下記の通り御見積り致します。</p>
                      <div style={styles.summaryBox}>
                        <div style={styles.summaryRow}><span>ご提供価格 :</span><span>{subTotal.toLocaleString()}</span></div>
                        <div style={styles.summaryRow}><span>消費税 (10%) :</span><span>{taxAmount.toLocaleString()}</span></div>
                        <div style={styles.totalRow}><span>{`総\u3000\u3000額 :`}</span><span>¥{grandTotal.toLocaleString()}</span></div>
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
                      <tr><td style={{border: '1px solid #000', padding: '5px', backgroundColor:'#f0f0f0'}}>値引き</td><td style={{border: '1px solid #000', padding: '5px', textAlign: 'right'}}>{discountValue > 0 ? `-${discountValue.toLocaleString()}` : '0'}</td></tr>
                      <tr><td style={{border: '1px solid #000', padding: '5px', backgroundColor:'#f0f0f0', fontWeight: 'bold'}}>合計</td><td style={{border: '1px solid #000', padding: '5px', textAlign: 'right', fontWeight: 'bold'}}>{grandTotal.toLocaleString()}</td></tr>
                    </tbody>
                  </table>
                  <div style={{fontSize: '0.9em', fontWeight: 'bold'}}>備考</div>
                  <div style={{...styles.remarksBox, border:'1px solid #000', height:'100px'}} />
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}