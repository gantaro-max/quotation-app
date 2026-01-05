import React, { useState } from 'react';
import { type QuotationDto, type User } from '../types';

interface QuotationListProps {
  currentUser: User;
  onLogout: () => void;
  onSelectQuotation: (id: number) => void;
  onCreateNew: () => void;
}

// 検索条件の型定義
interface SearchParams {
  customerCode: string;
  salesBranchName: string;
  projectName: string;
}

export const QuotationList: React.FC<QuotationListProps> = ({ currentUser, onLogout, onSelectQuotation, onCreateNew }) => {
  const [searchList, setSearchList] = useState<QuotationDto[]>([]);
  
  // 検索条件State
  const [searchParams, setSearchParams] = useState<SearchParams>({
    customerCode: '',
    salesBranchName: '',
    projectName: ''
  });
  
  // 開閉状態管理
  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>({});

  const toggleGroup = (key: string) => {
    setOpenGroups(prev => ({ ...prev, [key]: !prev[key] }));
  };

  // 自分の全件を取得する処理（条件なし検索用）
  const fetchMyList = async () => {
    try {
      const res = await fetch(`/api/quotations?createdByUserId=${currentUser.id}`);
      const json = await res.json();
      if(json.success) setSearchList(json.data);
    } catch(e) { console.error(e); }
  };

  // 検索ボタン押下時の処理
  const executeSearch = async () => {
    // 条件がすべて空かどうか判定
    const isConditionEmpty = 
      !searchParams.customerCode && 
      !searchParams.salesBranchName && 
      !searchParams.projectName;

    if (isConditionEmpty) {
      // 何も入力されていない場合は、自分の全件を表示
      await fetchMyList();
      return;
    }

    // 条件がある場合は検索APIをコール
    try {
      const params = new URLSearchParams();
      if(searchParams.customerCode) params.append('customerCode', searchParams.customerCode);
      if(searchParams.salesBranchName) params.append('salesBranchName', searchParams.salesBranchName);
      if(searchParams.projectName) params.append('projectName', searchParams.projectName);
      
      const res = await fetch(`/api/quotations/search?${params.toString()}`);
      const json = await res.json();
      if(json.success) setSearchList(json.data);
    } catch(e) { console.error(e); }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setSearchParams(prev => ({ ...prev, [name]: value }));
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') executeSearch();
  };  

  // --- グルーピングロジック ---
  interface GroupedData {
    [projectName: string]: {
      [baseEstimateNo: string]: QuotationDto[];
    };
  }

  const groupedData: GroupedData = searchList.reduce((acc, q) => {
    const projectKey = q.projectName || '（案件名なし）';
    const baseNoMatch = (q.estimateNo || '').match(/^(.*)-\d{2}$/);
    const baseNo = baseNoMatch ? baseNoMatch[1] : (q.estimateNo || 'No.なし');

    if (!acc[projectKey]) acc[projectKey] = {};
    if (!acc[projectKey][baseNo]) acc[projectKey][baseNo] = [];
    
    acc[projectKey][baseNo].push(q);
    return acc;
  }, {} as GroupedData);

  const sortedProjectKeys = Object.keys(groupedData).sort().reverse();

  // スタイル定義
  const styles = {
    // ...（既存のスタイル定義と同じ）...
    container: { 
      height: '100vh', 
      padding: '10px', 
      backgroundColor: '#555', 
      boxSizing: 'border-box' as const,
      display: 'flex',
      flexDirection: 'column' as const
    },
    panel: { 
      flex: 1, 
      backgroundColor: '#f4f6f9', 
      borderRadius: '4px', 
      display: 'flex',
      flexDirection: 'column' as const,
      overflow: 'hidden' 
    },
    header: { 
      padding: '20px', 
      borderBottom: '1px solid #ddd', 
      backgroundColor: '#fff',
      flexShrink: 0 
    },
    listArea: {
      flex: 1,
      overflowY: 'auto' as const,
      padding: '20px'
    },
    searchRow: { display: 'flex', gap: '10px', alignItems: 'center', marginBottom: '10px' },
    input: { padding: '8px', fontSize: '0.9em', borderRadius: '4px', border: '1px solid #ccc', width: '180px' },
    btn: { padding: '8px 16px', backgroundColor: '#3498db', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold' },
    actionRow: { display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' },
    projectRow: { backgroundColor: '#dfe6e9', padding: '12px 15px', fontWeight: 'bold', marginTop: '10px', borderRadius: '4px', color: '#2d3436', cursor: 'pointer', display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
    estimateGroupRow: { backgroundColor: '#fff', borderBottom:'1px solid #eee', padding: '8px 15px 8px 30px', cursor: 'pointer', display: 'flex', alignItems: 'center', color: '#2980b9', fontWeight: 'bold' },
    detailRow: { backgroundColor: '#fff', borderBottom:'1px solid #eee', fontSize:'0.9em' },
    submittedBadge: { fontSize: '0.8em', padding: '2px 6px', borderRadius: '4px', backgroundColor: '#27ae60', color: 'white' }
  };

  return (
    <div style={styles.container}>
      <div style={styles.panel}>
        {/* 固定ヘッダー部分 */}
        <div style={styles.header}>
          <div style={{marginBottom: '10px'}}>
            <h2 style={{margin: '0 0 10px 0'}}>見積検索</h2>
            <div style={styles.searchRow}>
              <input 
                name="customerCode"
                style={styles.input} 
                placeholder="得意先CD (前方一致)" 
                value={searchParams.customerCode} 
                onChange={handleInputChange} 
                onKeyDown={handleKeyDown} 
              />
              <input 
                name="salesBranchName"
                style={styles.input} 
                placeholder="営業所名 (前方一致)" 
                value={searchParams.salesBranchName} 
                onChange={handleInputChange} 
                onKeyDown={handleKeyDown} 
              />
              <input 
                name="projectName"
                style={{...styles.input, width: '250px'}} 
                placeholder="案件名 (前方一致)" 
                value={searchParams.projectName} 
                onChange={handleInputChange} 
                onKeyDown={handleKeyDown} 
              />
              <button style={styles.btn} onClick={executeSearch}>検 索</button>
            </div>
          </div>
          <div style={styles.actionRow}>
            <button style={{...styles.btn, backgroundColor:'#2ecc71'}} onClick={onCreateNew}>＋ 新規作成</button>
            <button style={{...styles.btn, backgroundColor:'#95a5a6'}} onClick={onLogout}>ログアウト</button>
          </div>
        </div>

        {/* スクロールするリスト部分 */}
        <div style={styles.listArea}>
          {searchList.length > 0 ? (
            sortedProjectKeys.map(projectKey => {
              const estimateGroups = groupedData[projectKey];
              const estimateKeys = Object.keys(estimateGroups).sort().reverse();
              const isProjectOpen = openGroups[projectKey] !== false;

              return (
                <div key={projectKey} style={{marginBottom:'5px'}}>
                  <div style={styles.projectRow} onClick={() => toggleGroup(projectKey)}>
                    <span>📁 {projectKey}</span>
                    <span style={{fontSize:'1.2em'}}>{isProjectOpen ? '−' : '＋'}</span>
                  </div>

                  {isProjectOpen && (
                    <div style={{borderLeft:'4px solid #dfe6e9', marginLeft:'10px'}}>
                      {estimateKeys.map(baseNo => {
                        const list = estimateGroups[baseNo];
                        list.sort((a, b) => (a.estimateNo || '').localeCompare(b.estimateNo || ''));
                        const groupKey = `${projectKey}-${baseNo}`;
                        const isGroupOpen = openGroups[groupKey] !== false;

                        return (
                          <div key={baseNo}>
                            <div style={styles.estimateGroupRow} onClick={() => toggleGroup(groupKey)}>
                              <span style={{marginRight:'10px'}}>{isGroupOpen ? '▼' : '▶'}</span>
                              <span>📄 {baseNo} シリーズ ({list.length}件)</span>
                            </div>
                            {isGroupOpen && list.map(q => (
                              <div 
                                key={q.id} 
                                style={styles.detailRow}
                                onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#f0f8ff'} 
                                onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'white'}
                                onClick={() => q.id && onSelectQuotation(q.id)}
                              >
                                <div style={{display:'flex', alignItems:'center', padding:'8px 10px 8px 50px', cursor:'pointer'}}>
                                  <div style={{width:'150px', fontWeight:'bold'}}>{q.estimateNo}</div>
                                  <div style={{width:'120px'}}>{q.issueDate}</div>
                                  <div style={{width:'200px'}}>{q.customerName}</div>
                                  <div style={{width:'120px', textAlign:'right'}}>¥{q.grandTotal?.toLocaleString()}</div>
                                  <div style={{marginLeft:'20px'}}>
                                    {q.isSubmitted && <span style={styles.submittedBadge}>提出済</span>}
                                  </div>
                                </div>
                              </div>
                            ))}
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              );
            })
          ) : (
            // データがない場合の表示
            <div style={{padding:'20px', textAlign:'center', color:'#999'}}>
                {/* 検索実行前でも「条件を指定して検索してください」等は出さず、単に空の状態でOKであればこのままで大丈夫です */}
                {Object.values(searchParams).some(v => v) || searchList.length === 0 ? 'データがありません' : '検索してください'}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};