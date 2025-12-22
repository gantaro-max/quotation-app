import React, { useEffect, useRef, useState } from 'react';
import { type Branch, type Customer, type Row, type RowType, type SalesStaff, type User } from '../types';
import './EditScreen.css';

// 設定定数
const ROWS_FIRST_PAGE = 32;
const ROWS_OTHER_PAGES = 40;

const toHalfWidth = (str: string) => {
  return str.replace(/[０-９]/g, (s) => String.fromCharCode(s.charCodeAt(0) - 0xFEE0));
};

interface EditScreenProps {
  currentUser: User;
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
  masterData: {
    branches: Branch[];
    staffs: SalesStaff[];
    customers: Customer[];
  };
  onBack: () => void;
  onSave: (isUpdate: boolean) => void;
}

// 範囲選択用の型定義
interface Point { r: number; c: string; }
interface SelectionRange { start: Point; end: Point; }

export const EditScreen: React.FC<EditScreenProps> = ({ 
  currentUser, data, setters, masterData, onBack, onSave 
}) => {
  const { id, date, estimateNo, searchBranchId, searchStaffId, projectName, customerName, discount, remarks, rows, attachedFile } = data;
  const { setSearchBranchId, setSearchStaffId, setProjectName, setCustomerName, setDiscount, setRemarks, setRows, setAttachedFile } = setters;
  const { branches, staffs, customers } = masterData;

  const fileInputRef = useRef<HTMLInputElement>(null);
  const inputRefs = useRef<{ [key: string]: HTMLInputElement | HTMLSelectElement | null }>({});
  const remarksRef = useRef<HTMLTextAreaElement>(null);
  const [selection, setSelection] = useState<SelectionRange | null>(null);

  // 印刷プレビュー用に選択情報を取得
  const selectedBranch = branches.find(b => b.id === searchBranchId);
  const selectedStaff = staffs.find(s => s.id === searchStaffId);

  // 金額計算
  const subTotal = rows.filter(r => r.type === 'normal').reduce((acc, row) => acc + (row.price * row.quantity), 0);
  const discountValue = typeof discount === 'string' ? 0 : discount;
  const mainTotal = subTotal - discountValue;
  const taxAmount = Math.floor(mainTotal * 0.1);
  const grandTotal = mainTotal + taxAmount;

  // 備考欄の自動高さ調整
  useEffect(() => { 
    if (remarksRef.current) { 
      remarksRef.current.style.height = 'auto'; 
      remarksRef.current.style.height = `${remarksRef.current.scrollHeight}px`; 
    } 
  }, [remarks]);

  const COL_ORDER: (keyof Row)[] = ['type', 'code', 'item', 'quantity', 'cost', 'price'];

  // キーボード操作・範囲選択
  const handleGridKeyDown = (e: React.KeyboardEvent, rIndex: number, colKey: string) => {
    // Shift + 矢印キーで範囲選択
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

      setSelection({
        start: startPoint,
        end: { r: newEndRow, c: COL_ORDER[newEndColIdx] as string }
      });
      return;
    }

    // Deleteキーで選択範囲消去
    if (e.key === 'Delete' || e.key === 'Backspace') {
       if (selection) {
         e.preventDefault();
         handleDeleteSelection();
         return;
       }
    }

    // Enter移動
    if (e.key === 'Enter' && !e.shiftKey) { 
      e.preventDefault(); 
      const nextKey = `${rIndex + 1}-${colKey}`; 
      if (inputRefs.current[nextKey]) {
        inputRefs.current[nextKey]?.focus();
        setSelection(null); 
      }
    }
  };

  // 範囲一括削除（修正済み）
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
      let updateRow = {...newRows[r]};
      for(let c = minC; c <= maxC; c++) {
        const colName = COL_ORDER[c];
        if (colName === 'quantity' || colName === 'cost' || colName === 'price') {
          updateRow = {...updateRow, [colName]: 0};
        } else if (colName !== 'type' && colName !== 'id') {
          updateRow = {...updateRow, [colName]: ''};
        }
      }
      newRows[r] = updateRow;
    }
    setRows(newRows);
    setSelection(null);
  };

  // Excelペースト（修正済み）
  const handlePaste = (e: React.ClipboardEvent, startRowIndex: number, startColKey: string) => {
    const text = e.clipboardData.getData('text');
    if (!text.includes('\t') && !text.includes('\n')) return;

    e.preventDefault();
    const lines = text.split(/\r\n|\n|\r/).filter(l => l !== '');
    if (lines.length === 0) return;

    const matrix = lines.map(line => line.split('\t'));
    const newRows = [...rows];
    const startColIdx = COL_ORDER.indexOf(startColKey as keyof Row);

    matrix.forEach((rowVals, rOffset) => {
      const targetR = startRowIndex + rOffset;
      if (targetR >= newRows.length) return;

      let updateRow = {...newRows[targetR]};

      rowVals.forEach((val, cOffset) => {
        const targetCIdx = startColIdx + cOffset;
        if (targetCIdx >= COL_ORDER.length) return;

        const colName = COL_ORDER[targetCIdx];
        if (colName === 'quantity' || colName === 'cost' || colName === 'price') {
          const num = Number(val.replace(/,/g, '').trim());
          updateRow = {...updateRow, [colName]: isNaN(num) ? 0 : num}; // ★修正済み: num代入
        }
        else if(colName === 'type') {       
          if(['normal','manufacturer','note','detail'].includes(val)){
            updateRow = {...updateRow, [colName]: val as RowType};
          }
        }
        else{
          updateRow = {...updateRow, [colName]: val};
        }
      });
      newRows[targetR] = updateRow;
    });
    setRows(newRows);
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

  const calculateMargin = (price: number, cost: number) => (!price ? 0 : ((price - cost) / price) * 100);
  const handleInputChange = (id: number, field: keyof Row, value: string | number) => { setRows(rows.map(row => row.id === id ? { ...row, [field]: value } : row)); };
  const handleNumberChange = (id: number, field: keyof Row, rawValue: string) => { const cleanValue = toHalfWidth(rawValue).replace(/,/g, ''); if (cleanValue === '') { setRows(rows.map(row => row.id === id ? { ...row, [field]: 0 } : row)); } else if (/^-?\d*$/.test(cleanValue)) { setRows(rows.map(row => row.id === id ? { ...row, [field]: Number(cleanValue) } : row)); } };
  const addRow = () => { const maxId = rows.length > 0 ? Math.max(...rows.map(r => r.id)) : 0; setRows([...rows, { id: maxId + 1, type: 'normal', code: '', manufacturer: '', item: '', quantity: 0, cost: 0, price: 0 }]); };
  const deleteRow = (id: number) => { setRows(rows.filter(r => r.id !== id)); };

  // 印刷プレビュー用ページ計算
  const getPages = () => {
    const pages = []; let currentRow = 0; 
    const remarksLineCount = remarks ? remarks.split('\n').length : 1; 
    const remarksHeightPx = 10 + (Math.max(remarksLineCount * 18, 120)); 
    const footerHeightPx = 100 + remarksHeightPx; 
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
    if (lastPage.length + footerRowsNeeded + 1 > maxRowsOnLastPage) { pages.push([]); } 
    if (pages.length === 0) pages.push([]); 
    return pages;
  };

  const pages = getPages();
  let globalItemIndex = 0;

  const renderCell = (rIndex: number, row: Row, colKey: keyof Row, content: React.ReactNode, extraStyle: React.CSSProperties = {}) => { 
    const isSelected = isInSelection(rIndex, colKey as string);
    // 選択状態に応じてクラスを付与
    const cellClass = `gridTd ${isSelected ? 'selected' : ''}`;
    
    return ( 
      <td className={cellClass} style={extraStyle}> 
        <div style={{width:'100%', height:'100%'}}>{content}</div> 
      </td> 
    ); 
  };

  // 共通props
  const commonProps = (rIndex: number, colKey: string) => ({
    ref: (el: HTMLInputElement | HTMLSelectElement | null) => { inputRefs.current[`${rIndex}-${colKey}`] = el; },
    onKeyDown: (e: React.KeyboardEvent) => handleGridKeyDown(e, rIndex, colKey),
    onPaste: (e: React.ClipboardEvent) => handlePaste(e, rIndex, colKey),
    onFocus: () => setSelection(null)
  });

  return (
    <div className="container" style={{display:'flex', height:'100vh', overflow:'hidden', fontFamily:'sans-serif', backgroundColor:'#555', padding:'10px', gap:'15px'}}>
      
      {/* 左パネル */}
      <div className="leftPanel">
        <div className="leftHeader">
          <div style={{marginBottom:'10px'}}>
            <button className="backBtn" onClick={onBack}>← 検索画面へ戻る</button>
            <span style={{fontWeight:'bold', fontSize:'1.1em'}}>{id ? `編集モード (ID: ${estimateNo})` : '新規作成モード'}</span>
          </div>
          <div className="filterRow">
            <div className="filterGroup">
              <label className="labelSmall">営業所</label>
              <select className="headerSelect" value={searchBranchId} onChange={(e) => { setSearchBranchId(Number(e.target.value)); setSearchStaffId(0); }}>
                {branches.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}
              </select>
            </div>
            <div className="filterGroup">
              <label className="labelSmall">担当者</label>
              <select className="headerSelect" style={{backgroundColor: searchStaffId===0 ? '#fff0f0' : '#fff'}} value={searchStaffId} onChange={(e) => setSearchStaffId(Number(e.target.value))}>
                <option value={0}>-- 担当者を選択 --</option>
                {staffs.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </div>
          </div>
          <div className="filterRow">
            <div className="filterGroup">
              <label className="labelSmall">得意先 (直接入力可)</label>
              <input 
                list="customer-list" 
                className="customerSelect" 
                value={customerName} 
                onChange={(e) => setCustomerName(e.target.value)} 
                placeholder="-- 得意先を入力または選択 --" 
              />
              <datalist id="customer-list">
                {customers.map(c => <option key={c.id} value={c.name} />)}
              </datalist>
            </div>
            <div className="filterGroup">
              <label className="labelSmall">案件名 (自由入力)</label>
              <input type="text" className="projectInput" placeholder="例：新規開業案件" value={projectName} onChange={(e) => setProjectName(e.target.value)} />
            </div>
          </div>
          <div className="attachArea">
             <input type="file" ref={fileInputRef} style={{display:'none'}} onChange={(e) => e.target.files && setAttachedFile(e.target.files[0])} />
             <button className="attachBtn" onClick={() => fileInputRef.current?.click()}>📎 仕入見積添付</button>
             <span className="fileName">{attachedFile ? attachedFile.name : '(未選択)'}</span>
          </div>
        </div>

        <div className="gridContainer">
          <table className="gridTable">
            <thead>
              <tr>
                <th className="gridTh" style={{width: '30px'}}></th>
                <th className="gridTh" style={{width: '60px'}}>種別</th>
                <th className="gridTh" style={{width: '70px'}}>CD</th>
                <th className="gridTh" style={{minWidth: '250px'}}>品名・規格</th>
                <th className="gridTh" style={{width: '50px'}}>数量</th>
                <th className="gridTh" style={{width: '70px', color:'#ff9999'}}>仕切価</th>
                <th className="gridTh" style={{width: '70px', color:'#99ccff'}}>単価</th>
                <th className="gridTh" style={{width: '80px', color:'#27ae60'}}>金額</th>
                <th className="gridTh" style={{width: '50px'}}>利益率%</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row: Row, rIndex: number) => {
                const margin = calculateMargin(row.price, row.cost);
                const isNegative = margin < 0; 
                const isInputEnabled = row.type === 'normal' || row.type === 'detail'; 
                const rowAmount = row.price * row.quantity;
                const cProps = (col: string) => commonProps(rIndex, col);

                return (
                  <tr key={row.id}>
                    <td className="gridTd" style={{textAlign:'center'}}>
                      <button onClick={() => deleteRow(row.id)} style={{border:'none', background:'transparent', color:'#ccc', cursor:'pointer'}}>×</button>
                    </td>
                    {renderCell(rIndex, row, 'type', 
                        <select {...cProps('type')} className="typeSelect" value={row.type} onChange={(e) => handleInputChange(row.id, 'type', e.target.value)}>
                            <option value="normal">通常</option><option value="manufacturer">メーカー</option><option value="detail">明細</option><option value="note">注釈</option>
                        </select>)}
                    {renderCell(rIndex, row, 'code', isInputEnabled ? 
                        <input {...cProps('code')} placeholder="CD" className="smallInput" value={row.code} onChange={e => handleInputChange(row.id, 'code', e.target.value)} /> : null)}
                    {renderCell(rIndex, row, 'item', row.type === 'manufacturer' ? 
                        <input {...cProps('item')} placeholder="メーカー名" className="smallInput" style={{fontWeight:'bold', backgroundColor: '#fffbe6'}} value={row.manufacturer} onChange={e => handleInputChange(row.id, 'manufacturer', e.target.value)} /> : 
                        <input {...cProps('item')} placeholder="品名" className="smallInput" value={row.item} onChange={e => handleInputChange(row.id, 'item', e.target.value)} />)}
                    {renderCell(rIndex, row, 'quantity', isInputEnabled && 
                        <input {...cProps('quantity')} type="text" className="smallInput" style={{textAlign:'right'}} value={row.quantity === 0 ? '' : row.quantity} onChange={e => handleNumberChange(row.id, 'quantity', e.target.value)} />)}
                    {renderCell(rIndex, row, 'cost', isInputEnabled && 
                        <input {...cProps('cost')} type="text" className="smallInput" style={{textAlign:'right', backgroundColor: '#fff5f5'}} value={row.cost === 0 ? '' : row.cost} onChange={e => handleNumberChange(row.id, 'cost', e.target.value)} />)}
                    {renderCell(rIndex, row, 'price', isInputEnabled && 
                        <input {...cProps('price')} type="text" className="smallInput" style={{textAlign:'right', backgroundColor: '#f0f9ff'}} value={row.price === 0 ? '' : row.price} onChange={e => handleNumberChange(row.id, 'price', e.target.value)} />)}
                    <td className="amountCell">{isInputEnabled && row.price > 0 && row.quantity > 0 ? rowAmount.toLocaleString() : ''}</td>
                    <td className="gridTd profitCell" style={{color: isNegative ? 'red' : 'black', fontWeight: isNegative ? 'bold' : 'normal'}}>{isInputEnabled && row.price > 0 ? `${margin.toFixed(0)}` : ''}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          <button onClick={addRow} className="addRowBtn">＋ 行を追加</button>
        </div>

        <div className="remarksInputArea">
          <div style={{fontSize:'0.85em', fontWeight:'bold', color:'#555', marginBottom:'3px'}}>備考 (入力・編集)</div>
          <textarea ref={remarksRef} className="remarksInput" placeholder="ここに備考を入力" value={remarks} onChange={(e) => setRemarks(e.target.value)} />
        </div>

        <div className="leftFooter">
          <div className="footerBtns">
            <button className="footerActionBtn" onClick={() => onSave(true)}>修正保存</button>
            <button className="footerActionBtn" style={{backgroundColor:'#3498db'}} onClick={() => onSave(false)}>新規保存</button>
            <button className="footerActionBtn" style={{backgroundColor:'#95a5a6'}} onClick={() => window.print()}>🖨️ 印刷</button>
          </div>
          <div className="calcContainer">
            <div className="calcItem"><span className="footerLabel">小計</span><span className="footerValue">{subTotal.toLocaleString()}</span></div>
            <div className="calcItem"><span className="footerLabel">値引</span><input type="text" className="discountInput" value={discount} onChange={(e) => setDiscount(e.target.value)} /></div>
            <div className="calcItem" style={{borderLeft:'1px solid #999', paddingLeft:'20px'}}><span className="footerLabel">合計</span><span className="footerValue" style={{color:'#2ecc71', fontSize:'1.5em'}}>¥{mainTotal.toLocaleString()}</span></div>
          </div>
        </div>
      </div>

      {/* 右パネル (印刷プレビュー) */}
      <div className="rightPanel">
        {pages.map((pageRows, pageIndex) => {
          const isFirstPage = pageIndex === 0; const isLastPage = pageIndex === pages.length - 1;
          const currentStaffName = selectedStaff?.name || currentUser.name;
          const currentBranchName = selectedBranch?.name || '';
          const currentBranchAddress = selectedBranch?.address || '';
          const currentBranchPhone = selectedBranch?.phone || '';

          return (
            <div key={pageIndex} className="pageContainer">
              {isFirstPage ? (
                <>
                  <h1 className="headerTitle">御 見 積 書</h1>
                  <div className="topSection">
                    <div className="customerInfo">
                      <div style={{display:'flex', alignItems: 'flex-end', marginBottom:'10px', borderBottom:'1px solid #333', minHeight:'40px'}}>
                         <span style={{ fontSize: '1.2em', width:'100%', fontWeight:'bold', whiteSpace:'nowrap', overflow:'visible' }}>{customerName}</span><span style={{fontSize: '1.2em', marginLeft:'10px', whiteSpace:'nowrap'}}>御中</span>
                      </div>
                      <p style={{fontSize: '0.9em'}}>ご照会賜りました件につきまして、<br/>下記の通り御見積り致します。</p>
                      <div className="summaryBox">
                        <div className="summaryRow"><span>ご提供価格 :</span><span>{mainTotal.toLocaleString()}</span></div>
                        <div className="summaryRow"><span>消費税 (10%) :</span><span>{taxAmount.toLocaleString()}</span></div>
                        <div className="totalRow"><span>{'総\u3000\u3000額 :'}</span><span>¥{grandTotal.toLocaleString()}</span></div>
                      </div>
                    </div>
                    <div className="companyInfo">
                      見積No: {estimateNo}<br/>日付: {date}<br/><br/><strong>株式会社セイエル</strong><br/>{currentBranchName}<br/>{currentBranchAddress}<br/>TEL: {currentBranchPhone}<br/><div style={{marginTop:'5px', paddingTop:'2px'}}>担当: {currentStaffName}</div>
                    </div>
                  </div>
                </>
              ) : (<div style={{textAlign:'left', fontSize:'0.8em', fontStyle:'italic', marginBottom:'10px', borderBottom:'1px dashed #ccc'}}>見積No: {estimateNo} / {customerName} 様 （前ページより）</div>)}
              
              <table className="printTable">
                <colgroup><col style={{width: '35px'}} /><col style={{width: 'auto'}} /><col style={{width: '50px'}} /><col style={{width: '110px'}} /><col style={{width: '130px'}} /></colgroup>
                <thead><tr><th className="printTh">項</th><th className="printTh">品名・規格</th><th className="printTh">数量</th><th className="printTh">単価</th><th className="printTh">金額</th></tr></thead>
                <tbody>
                  {pageRows.map((row) => {
                    let displayIndex = null; let displayText = row.item; let displayStyle: React.CSSProperties = {};
                    if (row.type === 'manufacturer') { displayText = `【メーカー: ${row.manufacturer}】`; displayStyle = { fontWeight: 'bold' }; } else if (row.type === 'detail') { displayText = `\u3000└ ${row.item}`; displayStyle = { fontSize: '0.85em', color: '#555' }; }
                    if (row.type === 'normal') { globalItemIndex++; displayIndex = globalItemIndex; }
                    const isPrintValueRow = row.type === 'normal';
                    return (
                      <tr key={row.id}>
                        <td className="printTd" style={{textAlign: 'center', backgroundColor: '#f9f9f9'}}>{displayIndex}</td>
                        <td className="printTd" style={{textAlign: 'left', ...displayStyle}}>{displayText}</td>
                        <td className="printTd" style={{textAlign: 'right'}}>{isPrintValueRow && row.quantity > 0 ? row.quantity : ''}</td>
                        <td className="printTd" style={{textAlign: 'right'}}>{isPrintValueRow && row.price > 0 ? row.price.toLocaleString() : ''}</td>
                        <td className="printTd" style={{textAlign: 'right', backgroundColor: '#fcfcfc'}}>{isPrintValueRow && row.price > 0 ? (row.quantity * row.price).toLocaleString() : ''}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>

              {!isLastPage ? (<div style={{textAlign: 'right', fontSize: '0.8em', fontStyle: 'italic', marginTop: '5px', borderTop: '1px dashed #ccc'}}>-- 次ページへ続く --</div>) : (
                <div className="footerArea">
                  <table className="footerTable">
                    <colgroup><col style={{width: '40%'}} /><col style={{width: '60%'}} /></colgroup>
                    <tbody>
                      <tr><td style={{border: '1px solid #000', padding: '5px', backgroundColor:'#f0f0f0'}}>小計</td><td style={{border: '1px solid #000', padding: '5px', textAlign: 'right'}}>{subTotal.toLocaleString()}</td></tr>
                      <tr><td style={{border: '1px solid #000', padding: '5px', backgroundColor:'#f0f0f0'}}>値引き</td><td style={{border: '1px solid #000', padding: '5px', textAlign: 'right'}}>{discountValue > 0 ? `-${discountValue.toLocaleString()}` : '-'}</td></tr>
                      <tr><td style={{border: '1px solid #000', padding: '5px', backgroundColor:'#f0f0f0', fontWeight: 'bold'}}>本体価計</td><td style={{border: '1px solid #000', padding: '5px', textAlign: 'right', fontWeight: 'bold'}}>¥{mainTotal.toLocaleString()}</td></tr>
                    </tbody>
                  </table>
                  <div style={{fontSize: '0.9em', fontWeight: 'bold'}}>備考</div>
                  <div className="remarksBox">{remarks}</div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};