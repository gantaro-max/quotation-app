import React, { useEffect, useRef, useState } from 'react';
import { type Branch, type Customer, type Row, type RowType, type SalesStaff, type User } from '../types';

interface EditScreenProps {
  currentUser: User;
  isReadOnly: boolean;
  creatorName: string;
  onCopyCreate: () => void;
  onTransitionToNew: () => void;
  data: {
    id: number | null;
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
    attachedFilePath: string | null;
    isSubmitted: boolean;
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
    setIsSubmitted: (val: boolean) => void;
  };
  masterData: {
    branches: Branch[];
    staffs: SalesStaff[];
    customers: Customer[];
  };
  onBack: () => void;
  onSave: (isUpdate: boolean) => void;
}

interface OcrResponseItem {
  itemName: string;
  quantity: number;
  unitPrice: number;
  costPrice: number;
}

interface Point { r: number; c: string; }
interface SelectionRange { start: Point; end: Point; }

const toHalfWidth = (str: string) => str.replace(/[０-９]/g, (s) => String.fromCharCode(s.charCodeAt(0) - 0xFEE0));
const ROWS_FIRST_PAGE = 32;
const ROWS_OTHER_PAGES = 40;

// スタイル定義
const styles: { [key: string]: React.CSSProperties } = {
  container: { display: 'flex', height: '100vh', overflow: 'hidden', fontFamily: '"Hiragino Kaku Gothic ProN", "Meiryo", sans-serif', backgroundColor: '#555', padding: '10px', boxSizing: 'border-box', gap: '15px' },
  leftPanel: { width: '50%', flexShrink: 0, backgroundColor: '#f4f6f9', borderRadius: '4px', display: 'flex', flexDirection: 'column', height: '100%', boxSizing: 'border-box', boxShadow: '0 0 10px rgba(0,0,0,0.3)' },
  leftHeader: { padding: '15px', backgroundColor: 'white', borderBottom: '1px solid #ddd', borderTopLeftRadius: '4px', borderTopRightRadius: '4px', flexShrink: 0 },
  filterRow: { display: 'flex', gap: '10px', marginBottom: '10px' },
  filterGroup: { flex: 1, display: 'flex', flexDirection: 'column' },
  labelSmall: { fontSize: '0.8em', color: '#666', marginBottom: '3px', fontWeight: 'bold' },
  headerSelect: { padding: '8px', fontSize: '0.9em', borderRadius: '4px', border: '1px solid #ccc', width: '100%', backgroundColor: '#fff' },
  customerSelect: { padding: '8px', fontSize: '1em', borderRadius: '4px', border: '1px solid #ccc', width: '100%', fontWeight: 'bold', color: '#2c3e50' },
  projectInput: { padding: '8px', fontSize: '1em', borderRadius: '4px', border: '2px solid #3498db', width: '100%', fontWeight: 'bold', color: '#2c3e50', backgroundColor: '#ebf5fb', boxSizing: 'border-box' },
  attachArea: { marginTop: '10px', padding: '10px', backgroundColor: '#f8f9fa', border: '1px dashed #ccc', borderRadius: '4px', display: 'flex', alignItems: 'center', gap: '10px' },
  attachBtn: { fontSize: '0.9em', padding: '5px 12px', backgroundColor: '#2980b9', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '5px' },  
  toolBtn: { fontSize: '0.9em', padding: '5px 12px', backgroundColor: '#fff', color: '#333', border: '1px solid #ccc', borderRadius: '4px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '5px' },
  fileName: { fontSize: '0.85em', color: '#333', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '200px' },
  backBtn: { fontSize: '0.9em', padding: '5px 15px', backgroundColor: '#7f8c8d', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', marginRight: '10px' },
  gridContainer: { flexGrow: 1, overflow: 'auto', padding: '0', position: 'relative', backgroundColor: '#fff', userSelect: 'none' },
  gridTable: { width: '100%', borderCollapse: 'collapse', fontSize: '0.85em', backgroundColor: 'white' },
  gridTh: { backgroundColor: '#34495e', color: 'white', padding: '10px 5px', border: '1px solid #2c3e50', textAlign: 'center', whiteSpace: 'nowrap', position: 'sticky', top: 0, zIndex: 100, boxShadow: '0 2px 2px rgba(0,0,0,0.1)' },
  gridTd: { border: '1px solid #dee2e6', padding: '0', verticalAlign: 'middle', backgroundColor: 'white' },
  remarksInputArea: { padding: '10px', backgroundColor: '#fcfcfc', borderTop: '1px solid #ddd' },
  remarksInput: { width: '100%', minHeight: '60px', padding: '8px', boxSizing: 'border-box', border: '1px solid #ccc', borderRadius: '4px', fontSize: '0.9em', resize: 'none', fontFamily: 'inherit', overflow: 'hidden' },
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
  rightPanel: { flex:1, minWidth: 0, flexShrink: 0, overflowY: 'auto', display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '0 10px' },
  pageContainer: { width: '210mm', minHeight: '297mm', backgroundColor: 'white', padding: '10mm 15mm', boxSizing: 'border-box', marginBottom: '20px', position: 'relative', fontFamily: '"MS Mincho", "Hiragino Mincho ProN", serif', color: '#333', transformOrigin: 'top center', boxShadow: '0 5px 15px rgba(0,0,0,0.5)', marginTop: '20px' },
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
  footerArea: { marginTop: 'auto', width: '100%', breakInside: 'avoid', pageBreakInside: 'avoid', display: 'block' },
  footerTable: { width: '50%', marginLeft: 'auto', borderCollapse: 'collapse', marginBottom: '5px' },
  remarksBox: { border: '1px solid #000', padding: '5px', minHeight: '120px', height: 'auto', marginTop: '2px', width: '100%', whiteSpace: 'pre-wrap', fontSize: '0.8em', lineHeight: '1.2', wordBreak: 'break-all' },
};

export const EditScreen: React.FC<EditScreenProps> = ({isReadOnly, creatorName, onCopyCreate, onTransitionToNew, data, setters, masterData, onBack, onSave }) => {
  const { id, date, estimateNo, searchBranchId, searchStaffId, projectName, customerName, discount, remarks, rows, attachedFile, attachedFilePath, isSubmitted } = data;
  const { setSearchBranchId, setSearchStaffId, setProjectName, setCustomerName, setDiscount, setRemarks, setRows, setAttachedFile, setIsSubmitted } = setters;
  const { branches, staffs, customers } = masterData;

  const fileInputRef = useRef<HTMLInputElement>(null);
  const inputRefs = useRef<{ [key: string]: HTMLInputElement | HTMLSelectElement | null }>({});
  const remarksRef = useRef<HTMLTextAreaElement>(null);
  
  const [selection, setSelection] = useState<SelectionRange | null>(null);
  const [isSelecting, setIsSelecting] = useState(false);
  // ★追加: OCR読み込み中フラグ
  const [isOcrLoading, setIsOcrLoading] = useState(false);
  
  const [previewScale, setPreviewScale] = useState(0.75);

  const selectedBranch = branches.find(b => b.id === searchBranchId);

  const subTotal = rows.filter(r => r.type === 'normal').reduce((acc, row) => acc + (row.price * row.quantity), 0);
  const discountValue = Number(discount) || 0;
  const mainTotal = subTotal - discountValue;
  const taxAmount = Math.floor(mainTotal * 0.1);
  const grandTotal = mainTotal + taxAmount;

  useEffect(() => { if (remarksRef.current) { remarksRef.current.style.height = 'auto'; remarksRef.current.style.height = `${remarksRef.current.scrollHeight}px`; } }, [remarks]);

  useEffect(() => {
    const handleGlobalMouseUp = () => setIsSelecting(false);
    window.addEventListener('mouseup', handleGlobalMouseUp);
    return () => window.removeEventListener('mouseup', handleGlobalMouseUp);
  }, []);

  const COL_ORDER: (keyof Row)[] = ['type', 'code', 'item', 'quantity', 'cost', 'price'];

  const handleGridKeyDown = (e: React.KeyboardEvent, rIndex: number, colKey: string) => {
    if (e.shiftKey && (e.key.startsWith('Arrow'))) {
      e.preventDefault();
      const currentPoint = { r: rIndex, c: colKey };
      const startPoint = selection ? selection.start : currentPoint;
      let newEndRow = selection ? selection.end.r : rIndex;
      let newEndColIdx = COL_ORDER.indexOf(selection ? selection.end.c as keyof Row : colKey as keyof Row);
      if (e.key === 'ArrowDown') newEndRow = Math.min(rows.length - 1, newEndRow + 1);
      if (e.key === 'ArrowUp') newEndRow = Math.max(0, newEndRow - 1);
      if (e.key === 'ArrowRight') newEndColIdx = Math.min(COL_ORDER.length - 1, newEndColIdx + 1);
      if (e.key === 'ArrowLeft') newEndColIdx = Math.max(0, newEndColIdx - 1);
      setSelection({ start: startPoint, end: { r: newEndRow, c: COL_ORDER[newEndColIdx] as string } });
      return;
    }
    if (e.key === 'Delete' || e.key === 'Backspace') {
       if (selection && !isReadOnly) { e.preventDefault(); handleDeleteSelection(); return; }
    }
    if (e.key === 'Enter' && !e.shiftKey) { 
      e.preventDefault(); 
      const nextKey = `${rIndex + 1}-${colKey}`; 
      if (inputRefs.current[nextKey]) { inputRefs.current[nextKey]?.focus(); setSelection(null); }
    }
  };

  const handleMouseDown = (e: React.MouseEvent,rIndex: number, colKey: string) => {
    if (e.button !== 0) return;
    setIsSelecting(true);
    setSelection({
      start: { r: rIndex, c: colKey },
      end: { r: rIndex, c: colKey }
    });
  };

  const handleMouseEnter = (rIndex: number, colKey: string) => {
    if (isSelecting) {
      setSelection(prev => {
        if (!prev) return null;
        return { ...prev, end: { r: rIndex, c: colKey } };
      });
    }
  };

  const handleDeleteSelection = () => {
    if (!selection) return;
    const { start, end } = selection;
    const minR = Math.min(start.r, end.r);
    const maxR = Math.max(start.r, end.r);
    const startCIdx = COL_ORDER.indexOf(start.c as keyof Row);
    const endCIdx = COL_ORDER.indexOf(end.c as keyof Row);
    const minC = Math.min(startCIdx, endCIdx);
    const maxC = Math.max(startCIdx, endCIdx);
    const newRows = [...rows];
    for(let r = minR; r <= maxR; r++) {
      let updatedRow = { ...newRows[r] };
      for(let c = minC; c <= maxC; c++) {
        const colName = COL_ORDER[c];
        if (colName === 'quantity' || colName === 'cost' || colName === 'price') { updatedRow = { ...updatedRow, [colName]: 0 }; } 
        else if (colName !== 'type' && colName !== 'id') { updatedRow = { ...updatedRow, [colName]: '' }; }
      }
      newRows[r] = updatedRow;
    }
    setRows(newRows);
    setSelection(null);
  };

  const processPaste = (text: string, startRowIndex: number, startColKey: string) => {
    const lines = text.split(/\r\n|\n|\r/).filter(l => l !== '');
    if (lines.length === 0) return;
    const matrix = lines.map(line => line.split('\t'));
    const newRows = [...rows];
    const startColIdx = COL_ORDER.indexOf(startColKey as keyof Row);
    
    matrix.forEach((rowVals, rOffset) => {
      const targetR = startRowIndex + rOffset;
      if (targetR >= newRows.length) return;
      let updatedRow = { ...newRows[targetR] };
      rowVals.forEach((val, cOffset) => {
        const targetCIdx = startColIdx + cOffset;
        if (targetCIdx >= COL_ORDER.length) return;
        const colName = COL_ORDER[targetCIdx];
        if (colName === 'quantity' || colName === 'cost' || colName === 'price') {
          const num = Number(val.replace(/,/g, '').trim());
          updatedRow = { ...updatedRow, [colName]: isNaN(num) ? 0 : num };
        } else if (colName === 'type') {
           if (['normal', 'manufacturer', 'note', 'detail'].includes(val)) { updatedRow = { ...updatedRow, [colName]: val as RowType }; }
        } else { updatedRow = { ...updatedRow, [colName]: val }; }
      });
      newRows[targetR] = updatedRow;
    });
    setRows(newRows);
  };

  const handlePaste = (e: React.ClipboardEvent, startRowIndex: number, startColKey: string) => {
    const text = e.clipboardData.getData('text');
    if (!text.includes('\t') && !text.includes('\n')) return;
    e.preventDefault();
    processPaste(text, startRowIndex, startColKey);
  };

  const getSelectedText = (): string | null => {
    if (!selection) return null;
    const { start, end } = selection;
    const minR = Math.min(start.r, end.r);
    const maxR = Math.max(start.r, end.r);
    const startCIdx = COL_ORDER.indexOf(start.c as keyof Row);
    const endCIdx = COL_ORDER.indexOf(end.c as keyof Row);
    const minC = Math.min(startCIdx, endCIdx);
    const maxC = Math.max(startCIdx, endCIdx);

    let copyText = "";
    for(let r = minR; r <= maxR; r++) {
      const rowTexts: string[] = [];
      for(let c = minC; c <= maxC; c++) {
        const colName = COL_ORDER[c];
        const val = rows[r][colName];
        if (val !== null && val !== undefined) { rowTexts.push(String(val)); } else { rowTexts.push("0"); }
      }
      copyText += rowTexts.join('\t') + (r < maxR ? '\n' : '');
    }
    return copyText;
  };

  const handleCopy = (e: React.ClipboardEvent) => {
    if (!selection) return; 
    e.preventDefault();
    const text = getSelectedText();
    if (text) {
        e.clipboardData.setData('text/plain', text);
    }
  };

  const handleManualCopy = async () => {
    const text = getSelectedText();
    if (!text) {
        alert('セルが選択されていません');
        return;
    }
    try {
        await navigator.clipboard.writeText(text);
    } catch (err) {
        console.error('コピーに失敗しました:', err);
        alert('コピーに失敗しました');
    }
  };

  const handleManualPaste = async () => {
    if (isReadOnly) return;
    if (!selection) {
        alert('貼り付け開始位置(セル)を選択してください');
        return;
    }
    try {
        const text = await navigator.clipboard.readText();
        if (!text) return;
        const { start, end } = selection;
        const minR = Math.min(start.r, end.r);
        const startCIdx = COL_ORDER.indexOf(start.c as keyof Row);
        const endCIdx = COL_ORDER.indexOf(end.c as keyof Row);
        const minCIdx = Math.min(startCIdx, endCIdx);
        const startColKey = COL_ORDER[minCIdx] as string;
        processPaste(text, minR, startColKey);
    } catch (err) {
        console.error('貼り付けに失敗しました:', err);
        alert('貼り付けに失敗しました(ブラウザの許可が必要な場合があります)');
    }
  };

  const isInSelection = (rIndex: number, colKey: string) => {
    if (!selection) return false;
    const { start, end } = selection;
    const minR = Math.min(start.r, end.r);
    const maxR = Math.max(start.r, end.r);
    const cIdx = COL_ORDER.indexOf(colKey as keyof Row);
    const startCIdx = COL_ORDER.indexOf(start.c as keyof Row);
    const endCIdx = COL_ORDER.indexOf(end.c as keyof Row);
    const minC = Math.min(startCIdx, endCIdx);
    const maxC = Math.max(startCIdx, endCIdx);
    return rIndex >= minR && rIndex <= maxR && cIdx >= minC && cIdx <= maxC;
  };

  const getDisplayFileName = () => {
    if(attachedFile) return attachedFile.name;
    if(attachedFilePath){
      const parts = attachedFilePath.split('_');
      return parts.length>1 ? parts.slice(1).join('_') : attachedFilePath;
    }
    return '(未選択)';
  }

  const calculateMargin = (price: number, cost: number) => (!price ? 0 : ((price - cost) / price) * 100);
  const handleInputChange = (id: number, field: keyof Row, value: string | number) => { setRows(rows.map(row => row.id === id ? { ...row, [field]: value } : row)); };
  const handleNumberChange = (id: number, field: keyof Row, rawValue: string) => { const cleanValue = toHalfWidth(rawValue).replace(/,/g, ''); if (cleanValue === '') { setRows(rows.map(row => row.id === id ? { ...row, [field]: 0 } : row)); } else if (/^-?\d*$/.test(cleanValue)) { setRows(rows.map(row => row.id === id ? { ...row, [field]: Number(cleanValue) } : row)); } };
  const addRow = () => { const maxId = rows.length > 0 ? Math.max(...rows.map(r => r.id)) : 0; setRows([...rows, { id: maxId + 1, type: 'normal', code: '', manufacturer: '', item: '', quantity: 0, cost: 0, price: 0 }]); };
  const deleteRow = (id: number) => { setRows(rows.filter(r => r.id !== id)); };

  const getPages = () => {
    const pages = []; let currentRow = 0; const remarksLineCount = remarks ? remarks.split('\n').length : 1; const remarksHeightPx = 10 + (Math.max(remarksLineCount * 18, 120)); const footerHeightPx = 100 + remarksHeightPx; const footerRowsNeeded = Math.ceil(footerHeightPx / 22);
    const firstPageRows = rows.slice(0, ROWS_FIRST_PAGE); pages.push(firstPageRows); currentRow += ROWS_FIRST_PAGE;
    while (currentRow < rows.length) { pages.push(rows.slice(currentRow, currentRow + ROWS_OTHER_PAGES)); currentRow += ROWS_OTHER_PAGES; }
    const lastPage = pages[pages.length - 1]; const maxRowsOnLastPage = pages.length === 1 ? ROWS_FIRST_PAGE : ROWS_OTHER_PAGES;
    if (lastPage.length + footerRowsNeeded + 1 > maxRowsOnLastPage) { pages.push([]); } if (pages.length === 0) pages.push([]); return pages;
  };
  const pages = getPages();
  let globalItemIndex = 0;

  const renderCell = (rIndex: number, _row: Row, colKey: keyof Row, content: React.ReactNode, extraStyle: React.CSSProperties = {}) => { 
    const isSelected = isInSelection(rIndex, colKey as string);
    const cellStyle = { ...styles.gridTd, ...extraStyle, backgroundColor: isSelected ? '#e6f7ff' : (extraStyle.backgroundColor || 'white'), border: isSelected ? '1px double #3498db' : styles.gridTd.border };
    return ( 
      <td 
        style={cellStyle}
        onMouseDown={(e) => handleMouseDown(e,rIndex, colKey)}
        onMouseEnter={() => handleMouseEnter(rIndex, colKey)}
      > 
        <div style={{width:'100%', height:'100%'}}>{content}</div> 
      </td> 
    ); 
  };

  const commonProps = (rIndex: number, colKey: string) => ({
    disabled: isReadOnly,
    ref: (el: HTMLInputElement | HTMLSelectElement | null) => { inputRefs.current[`${rIndex}-${colKey}`] = el; },
    onKeyDown: (e: React.KeyboardEvent) => handleGridKeyDown(e, rIndex, colKey),
    onPaste: (e: React.ClipboardEvent) => !isReadOnly && handlePaste(e, rIndex, colKey),
    onCopy: (e: React.ClipboardEvent) => handleCopy(e),
  });

  // ===========================================================================
  // OCR解析ハンドラ (Loading表示対応版)
  // ===========================================================================
  const handleOcrAnalysis = async () => {
    // 1. ファイル添付チェック
    if (!attachedFile) {
      alert("先にファイルを添付してください（PDFまたは画像）");
      return;
    }
    
    // 2. 上書き確認
    if (!window.confirm("添付ファイルの内容を読み取って明細に反映しますか？\n（現在の明細行は上書きされます）")) {
      return;
    }

    // ★修正: ローディング開始
    setIsOcrLoading(true);

    try {
      // 3. API送信準備
      const formData = new FormData();
      formData.append("file", attachedFile);

      // 4. API呼び出し
      const res = await fetch("/api/quotations/ocr", {
        method: "POST",
        body: formData,
      });
      const json = await res.json();

      if (json.success) {
        const items = json.data as OcrResponseItem[];
        
        if (!items || items.length === 0) {
          alert("読み取れる明細が見つかりませんでした。");
          return;
        }

        // 5. 行データ(Row型)への変換
        const newRows: Row[] = items.map((item, index) => ({
          id: index + 1,
          dbId: null, // 新規扱いなのでnull
          type: "normal",
          code: "", 
          manufacturer: "",
          item: item.itemName || "",    // 品名
          quantity: item.quantity || 0, // 数量
          cost: item.costPrice || 0,    // 仕切価
          price: item.unitPrice || 0,   // 単価(バックエンドが送ってこなければ0)
        }));

        // 6. 20行になるまで空行を追加
        while (newRows.length < 20) {
          newRows.push({
            id: newRows.length + 1,
            type: "normal",
            code: "",
            manufacturer: "",
            item: "",
            quantity: 0,
            cost: 0,
            price: 0,
          });
        }
        
        // 7. 画面に反映
        setRows(newRows);
        // 成功時のアラートはあってもなくても良いですが、処理完了がわかるので残しておきます
        // alert("読み取りが完了しました"); 
      } else {
        alert("OCR解析エラー: " + (json.message || "不明なエラー"));
      }
    } catch (e) {
      console.error(e);
      alert("通信エラーが発生しました");
    } finally {
      // ★修正: ローディング終了（成功・失敗にかかわらず）
      setIsOcrLoading(false);
    }
  };

  return (
    <div style={styles.container}>
      <style>{`@media print { @page { margin: 0; size: A4; } body { background-color: white !important; -webkit-print-color-adjust: exact; } .left-panel-print-hidden { display: none !important; } .right-panel-print-full { width: 100% !important; padding: 0 !important; background-color: white !important; overflow: visible !important; display: block !important; } .page-container-print { transform: none !important; margin: 0 !important; padding: 15mm 20mm !important; box-shadow: none !important; page-break-after: always; width: 100% !important; } .page-container-print:last-child { page-break-after: auto; } .no-print { display: none !important; } }`}</style>
      <div style={styles.leftPanel} className="left-panel-print-hidden">
        <div style={styles.leftHeader}>
          <div style={{marginBottom:'10px', display:'flex', justifyContent:'space-between', alignItems:'center'}}>
            <div>
              <button style={styles.backBtn} onClick={onBack}>← 検索画面へ戻る</button>
              <span style={{fontWeight:'bold', fontSize:'1.1em'}}>
                {id 
                  ? (isReadOnly ? '【参照モード】(編集不可)' : `編集モード (ID: ${estimateNo})`) 
                  : '新規作成モード'}
              </span>
            </div>
            <label style={{display:'flex', alignItems:'center', gap:'5px', cursor:'pointer', padding:'5px', border:'1px solid #ccc', borderRadius:'4px', backgroundColor: isSubmitted ? '#e8f5e9' : '#fff'}}>
              <input 
                type="checkbox" 
                checked={isSubmitted} 
                onChange={(e) => setIsSubmitted(e.target.checked)} 
                disabled={isReadOnly} 
                style={{transform:'scale(1.2)'}} 
              />
              <span style={{fontWeight:'bold', color: isSubmitted ? '#27ae60' : '#555'}}>提出済としてマーク</span>
            </label>
          </div>
          <div style={styles.filterRow}>
            <div style={styles.filterGroup}>
              <label style={styles.labelSmall}>営業所</label>
              <select style={styles.headerSelect} value={searchBranchId} onChange={(e) => { setSearchBranchId(Number(e.target.value)); setSearchStaffId(0); }} disabled={isReadOnly}>
                {branches.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}
              </select>
            </div>
            <div style={styles.filterGroup}>
              <label style={styles.labelSmall}>担当者</label>
              <select style={{...styles.headerSelect, backgroundColor: searchStaffId===0 ? '#fff0f0' : '#fff'}} value={searchStaffId} onChange={(e) => setSearchStaffId(Number(e.target.value))} disabled={isReadOnly}>
                <option value={0}>-- 担当者を選択 --</option>
                {staffs.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </div>
          </div>
          <div style={styles.filterRow}>
            <div style={styles.filterGroup}>
              <label style={styles.labelSmall}>得意先 (直接入力可)</label>
              <input list="customer-list" style={styles.customerSelect} value={customerName} onChange={(e) => setCustomerName(e.target.value)} placeholder="-- 得意先を入力または選択 --" disabled={isReadOnly} />
              <datalist id="customer-list">
                {customers.map(c => <option key={c.id} value={c.name} />)}
              </datalist>
            </div>
            <div style={styles.filterGroup}>
              <label style={styles.labelSmall}>案件名 (自由入力)</label>
              <input type="text" style={styles.projectInput} placeholder="例：新規開業案件" value={projectName} onChange={(e) => setProjectName(e.target.value)} disabled={isReadOnly} />
            </div>
          </div>
          <div style={styles.attachArea}>
             <div style={{display:'flex', gap:'5px', marginRight:'15px', borderRight:'1px solid #ccc', paddingRight:'15px'}}>
               <button onClick={handleManualCopy} style={styles.toolBtn} title="選択範囲をコピー">📄 コピー</button>
               {!isReadOnly && (
                 <button onClick={handleManualPaste} style={styles.toolBtn} title="現在のセルに貼り付け">📋 貼付</button>
               )}
             </div>
             <input type="file" ref={fileInputRef} style={{display:'none'}} onChange={(e) => e.target.files && setAttachedFile(e.target.files[0])} disabled={isReadOnly} />
             <button style={{...styles.attachBtn, opacity: isReadOnly ? 0.5 : 1}} onClick={() => fileInputRef.current?.click()} disabled={isReadOnly}>📎 仕入見積添付</button>
             
             <button // OCR自動読取ボタン
               onClick={handleOcrAnalysis} 
               // ★修正: 読み込み中もdisabledにする
               disabled={isReadOnly || !attachedFile || isOcrLoading} 
               style={{
                 fontSize: '0.9em', 
                 padding: '5px 12px', 
                 backgroundColor: '#8e44ad', 
                 color: 'white', 
                 border: 'none', 
                 borderRadius: '4px', 
                 // ★修正: 読み込み中はカーソルと透明度を変更
                 cursor: (isReadOnly || !attachedFile || isOcrLoading) ? 'not-allowed' : 'pointer',
                 opacity: (isReadOnly || !attachedFile || isOcrLoading) ? 0.5 : 1, 
                 marginLeft: '5px',
                 fontWeight: 'bold',
                 display: 'flex',       
                 alignItems: 'center',
                 gap: '5px'
               }}
               title="添付ファイルから明細を自動読み取りします"
             >
               {/* ★修正: 読み込み中はテキストを変更 */}
               {isOcrLoading ? '⏳ 読込中...' : '🤖 自動読取'}
             </button>

             <span style={styles.fileName}>{getDisplayFileName()}</span>
          </div>
        </div>

        <div style={styles.gridContainer}>
          <table style={styles.gridTable}>
            <thead>
              <tr><th style={{...styles.gridTh, width: '30px'}}></th><th style={{...styles.gridTh, width: '60px'}}>種別</th><th style={{...styles.gridTh, width: '75px'}}>商品CD</th><th style={{...styles.gridTh, minWidth: '250px'}}>品名・規格</th><th style={{...styles.gridTh, width: '50px'}}>数量</th><th style={{...styles.gridTh, width: '70px', color:'#ff9999'}}>仕切価</th><th style={{...styles.gridTh, width: '70px', color:'#99ccff'}}>単価</th><th style={{...styles.gridTh, width: '80px', color:'#27ae60'}}>金額</th><th style={{...styles.gridTh, width: '50px'}}>利益率%</th></tr>
            </thead>
            <tbody>
              {rows.map((row: Row, rIndex: number) => {
                const margin = calculateMargin(row.price, row.cost);
                const isNegative = margin < 0; const isInputEnabled = row.type === 'normal' || row.type === 'detail'; const rowAmount = row.price * row.quantity;
                return (
                  <tr key={row.id}>
                    <td style={{...styles.gridTd, textAlign:'center'}}>
                        {!isReadOnly && <button onClick={() => deleteRow(row.id)} style={{border:'none', background:'transparent', color:'#ccc', cursor:'pointer'}}>×</button>}
                    </td>
                    {renderCell(rIndex, row, 'type', 
                        <select {...commonProps(rIndex, 'type')} style={styles.typeSelect} value={row.type} onChange={(e) => handleInputChange(row.id, 'type', e.target.value)}>
                            <option value="normal">通常</option><option value="manufacturer">メーカー</option><option value="detail">明細</option><option value="note">注釈</option>
                        </select>)}
                    {renderCell(rIndex, row, 'code', isInputEnabled ? 
                        <input {...commonProps(rIndex, 'code')} placeholder="CD" style={styles.smallInput} value={row.code} onChange={e => handleInputChange(row.id, 'code', e.target.value)} /> : null)}
                    {renderCell(rIndex, row, 'item', row.type === 'manufacturer' ? 
                        <input {...commonProps(rIndex, 'item')} placeholder="メーカー名" style={{...styles.smallInput, fontWeight:'bold', backgroundColor: '#fffbe6'}} value={row.manufacturer} onChange={e => handleInputChange(row.id, 'manufacturer', e.target.value)} /> : 
                        <input {...commonProps(rIndex, 'item')} placeholder="品名" style={styles.smallInput} value={row.item} onChange={e => handleInputChange(row.id, 'item', e.target.value)} />)}
                    {renderCell(rIndex, row, 'quantity', isInputEnabled && 
                        <input {...commonProps(rIndex, 'quantity')} type="text" style={{...styles.smallInput, textAlign:'right'}} value={row.quantity === 0 ? '' : row.quantity} onChange={e => handleNumberChange(row.id, 'quantity', e.target.value)} />)}
                    {renderCell(rIndex, row, 'cost', isInputEnabled && 
                        <input {...commonProps(rIndex, 'cost')} type="text" style={{...styles.smallInput, textAlign:'right', backgroundColor: '#fff5f5'}} value={row.cost === 0 ? '' : row.cost} onChange={e => handleNumberChange(row.id, 'cost', e.target.value)} />)}
                    {renderCell(rIndex, row, 'price', isInputEnabled && 
                        <input {...commonProps(rIndex, 'price')} type="text" style={{...styles.smallInput, textAlign:'right', backgroundColor: '#f0f9ff'}} value={row.price === 0 ? '' : row.price} onChange={e => handleNumberChange(row.id, 'price', e.target.value)} />)}
                    <td style={styles.amountCell}>{isInputEnabled && row.price > 0 && row.quantity > 0 ? rowAmount.toLocaleString() : ''}</td>
                    <td style={{...styles.gridTd, ...styles.profitCell, color: isNegative ? 'red' : 'black', fontWeight: isNegative ? 'bold' : 'normal'}}>{isInputEnabled && row.price > 0 ? `${margin.toFixed(0)}` : ''}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>        
        <div style={{marginTop:'10px', marginBottom:'10px', padding:'0 10px', display:'flex', gap:'10px'}}>
          {!isReadOnly && (
            <button onClick={addRow} style={{flex:1, padding:'10px', backgroundColor:'#ecf0f1', border:'1px dashed #bdc3c7', cursor:'pointer', color:'#7f8c8d'}}>＋ 行を追加</button>
          )}
        </div>
        
        <div style={styles.remarksInputArea}>
          <div style={{fontSize:'0.85em', fontWeight:'bold', color:'#555', marginBottom:'3px'}}>備考 (入力・編集)</div>
          <textarea ref={remarksRef} style={styles.remarksInput} placeholder="ここに備考を入力" value={remarks} onChange={(e) => setRemarks(e.target.value)} disabled={isReadOnly} />
        </div>

        <div style={styles.leftFooter}>
          <div style={styles.footerBtns}>
            {!isReadOnly ? (
              id?(
                // 編集モード（IDあり）-> 「修正保存」と「新規作成」
              <>
                <button style={styles.footerActionBtn} onClick={() => onSave(true)}>修正保存</button>
                <button style={{...styles.footerActionBtn, backgroundColor:'#8e44ad'}} onClick={onTransitionToNew}>新規作成</button>
              </>
              ):(
                // 新規作成モード（IDなし）-> 「新規保存」のみ
                <button style={{...styles.footerActionBtn, backgroundColor:'#3498db'}} onClick={() => onSave(false)}>新規保存</button>
              )
            ) : (
                <button style={{...styles.footerActionBtn, backgroundColor:'#8e44ad'}} onClick={onCopyCreate}>📝 この内容をコピーして新規作成</button>
            )}
            <button style={{...styles.footerActionBtn, backgroundColor:'#95a5a6'}} onClick={() => window.print()}>🖨️ 印刷</button>
          </div>
          <div style={styles.calcContainer}>
            <div style={styles.calcItem}><span style={styles.footerLabel}>小計</span><span style={styles.footerValue}>{subTotal.toLocaleString()}</span></div>
            <div style={styles.calcItem}><span style={styles.footerLabel}>値引</span><input type="text" style={styles.discountInput} value={discount} onChange={(e) => setDiscount(e.target.value)} placeholder="" disabled={isReadOnly} /></div>
            <div style={{...styles.calcItem, borderLeft:'1px solid #999', paddingLeft:'20px'}}><span style={styles.footerLabel}>合計</span><span style={{...styles.footerValue, color:'#2ecc71', fontSize:'1.5em'}}>¥{mainTotal.toLocaleString()}</span></div>
          </div>
        </div>
      </div>

      <div style={styles.rightPanel} className="right-panel-print-full">
        {/* ズーム操作バーを中央寄せに変更 */}
        <div style={{width:'100%', padding:'10px', display:'flex', justifyContent:'left', alignItems:'center', gap:'10px'}}>
            <span style={{fontSize:'0.8em', fontWeight:'bold', color:'white'}}></span>
            <button onClick={() => setPreviewScale(s => Math.max(0.5, s - 0.1))} style={{cursor:'pointer', width:'30px', fontWeight:'bold'}}>-</button>
            <span style={{color:'white', minWidth:'40px', textAlign:'center'}}>{Math.round(previewScale * 100)}%</span>
            <button onClick={() => setPreviewScale(s => Math.min(1.15, s + 0.1))} style={{cursor:'pointer', width:'30px', fontWeight:'bold'}}>+</button>
        </div>

        {pages.map((pageRows, pageIndex) => {
          const isFirstPage = pageIndex === 0; const isLastPage = pageIndex === pages.length - 1;
          const currentStaffName = creatorName;
          const currentBranchName = selectedBranch?.name || '';
          const currentBranchAddress = selectedBranch?.address || '';
          const currentBranchPhone = selectedBranch?.phone || '';

          return (
            <div key={pageIndex} style={{
                ...styles.pageContainer, 
                transform: `scale(${previewScale})`,
                marginBottom: `${(previewScale - 1) * 297}mm`
            }} className="page-container-print">
              {isFirstPage ? (
                <>
                  <h1 style={styles.headerTitle}>御 見 積 書</h1>
                  <div style={styles.topSection}>
                    <div style={styles.customerInfo}>
                      <div style={{display:'flex', alignItems: 'flex-end', marginBottom:'10px', borderBottom:'1px solid #333', minHeight:'40px'}}>
                         <span style={{ fontSize: '1.2em', width:'100%', fontWeight:'bold', whiteSpace:'nowrap', overflow:'visible' }}>{customerName}</span><span style={{fontSize: '1.2em', marginLeft:'10px', whiteSpace:'nowrap'}}>御中</span>
                      </div>
                      <p style={{fontSize: '0.9em'}}>ご照会賜りました件につきまして、<br/>下記の通り御見積り致します。</p>
                      <div style={styles.summaryBox}>
                        <div style={styles.summaryRow}><span>ご提供価格 :</span><span>{mainTotal.toLocaleString()}</span></div>
                        <div style={styles.summaryRow}><span>消費税 (10%) :</span><span>{taxAmount.toLocaleString()}</span></div>
                        <div style={styles.totalRow}><span>{'総\u3000\u3000額 :'}</span><span>¥{grandTotal.toLocaleString()}</span></div>
                      </div>
                    </div>
                    <div style={styles.companyInfo}>
                      見積No: {estimateNo}<br/>日付: {date}<br/><br/><strong>株式会社サンプル</strong><br/>{currentBranchName}<br/>{currentBranchAddress}<br/>TEL: {currentBranchPhone}<br/><div style={{marginTop:'5px', paddingTop:'2px'}}>作成: {currentStaffName}</div>
                    </div>
                  </div>
                </>
              ) : (<div style={{textAlign:'left', fontSize:'0.8em', fontStyle:'italic', marginBottom:'10px', borderBottom:'1px dashed #ccc'}}>見積No: {estimateNo} / {customerName} 様 （前ページより）</div>)}
              <table style={styles.table}>
                <colgroup><col style={{width: '35px'}} /><col style={{width: 'auto'}} /><col style={{width: '50px'}} /><col style={{width: '110px'}} /><col style={{width: '130px'}} /></colgroup>
                <thead><tr><th style={styles.th}>項</th><th style={styles.th}>品名・規格</th><th style={styles.th}>数量</th><th style={styles.th}>単価</th><th style={styles.th}>金額</th></tr></thead>
                <tbody>
                  {pageRows.map((row) => {
                    let displayIndex = null; let displayText = row.item; let displayClass: React.CSSProperties = {};
                    if (row.type === 'manufacturer') { displayText = `【メーカー: ${row.manufacturer}】`; displayClass = { fontWeight: 'bold' }; } else if (row.type === 'detail') { displayText = `\u3000└ ${row.item}`; displayClass = { fontSize: '0.85em', color: '#555' }; }
                    if (row.type === 'normal') { globalItemIndex++; displayIndex = globalItemIndex; }
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
              {!isLastPage ? (<div style={{textAlign: 'right', fontSize: '0.8em', fontStyle: 'italic', marginTop: '5px', borderTop: '1px dashed #ccc'}}>-- 次ページへ続く --</div>) : (
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
                  <div style={{...styles.remarksBox, border:'1px solid #000', height:'auto', minHeight:'120px'}}>{remarks}</div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};