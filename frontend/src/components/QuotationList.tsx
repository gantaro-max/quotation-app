import React, { useEffect, useState } from 'react';
import { type QuotationDto, type User } from '../types';

interface QuotationListProps {
  currentUser: User;
  onLogout: () => void;
  onSelectQuotation: (id: number) => void;
  onCreateNew: () => void;
}

export const QuotationList: React.FC<QuotationListProps> = ({ currentUser, onLogout, onSelectQuotation, onCreateNew }) => {
  const [searchList, setSearchList] = useState<QuotationDto[]>([]);
  const [searchText, setSearchText] = useState('');
  
  // 開閉状態管理: { "案件名": boolean, "案件名-見積No": boolean }
  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>({});

  const toggleGroup = (key: string) => {
    setOpenGroups(prev => ({ ...prev, [key]: !prev[key] }));
  };

  const fetchMyList = async () => {
    try {
      const res = await fetch(`/api/quotations?createdByUserId=${currentUser.id}`);
      const json = await res.json();
      if(json.success) setSearchList(json.data);
    } catch(e) { console.error(e); }
  };

  const executeSearch = async () => {
    try {
      const params = new URLSearchParams();
      if(searchText) params.append('customerName', searchText);
      const res = await fetch(`/api/quotations/search?${params.toString()}`);
      const json = await res.json();
      if(json.success) setSearchList(json.data);
    } catch(e) { console.error(e); }
  };

  useEffect(() => {
    fetchMyList();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentUser]);

  // --- グルーピングロジック ---
  // 1. 案件名でまとめる
  // 2. その中で、見積番号の「親番号（枝番除く）」でまとめる
  interface GroupedData {
    [projectName: string]: {
      [baseEstimateNo: string]: QuotationDto[];
    };
  }

  const groupedData: GroupedData = searchList.reduce((acc, q) => {
    const projectKey = q.projectName || '（案件名なし）';
    // 枝番除去ロジック (Q-001-01 -> Q-001)
    const baseNoMatch = (q.estimateNo || '').match(/^(.*)-\d{2}$/);
    const baseNo = baseNoMatch ? baseNoMatch[1] : (q.estimateNo || 'No.なし');

    if (!acc[projectKey]) acc[projectKey] = {};
    if (!acc[projectKey][baseNo]) acc[projectKey][baseNo] = [];
    
    acc[projectKey][baseNo].push(q);
    return acc;
  }, {} as GroupedData);

  // ソート (新しい順)
  const sortedProjectKeys = Object.keys(groupedData).sort().reverse(); // 案件名は文字列順（暫定）

  // スタイル
  const styles = {
    container: { height: '100vh', padding: '10px', backgroundColor: '#555', boxSizing: 'border-box' as const },
    panel: { width: '100%', height: '100%', backgroundColor: '#f4f6f9', borderRadius: '4px', padding: '20px', boxSizing: 'border-box' as const, overflowY: 'auto' as const },
    header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', borderBottom: '1px solid #ddd', paddingBottom: '10px' },
    input: { padding: '10px', fontSize: '1em', width: '300px', borderRadius: '4px', border: '1px solid #ccc' },
    btn: { padding: '10px 20px', backgroundColor: '#3498db', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold', marginLeft: '10px' },
    
    // 階層表示用スタイル
    projectRow: { backgroundColor: '#dfe6e9', padding: '12px 15px', fontWeight: 'bold', marginTop: '10px', borderRadius: '4px', color: '#2d3436', cursor: 'pointer', display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
    estimateGroupRow: { backgroundColor: '#fff', borderBottom:'1px solid #eee', padding: '8px 15px 8px 30px', cursor: 'pointer', display: 'flex', alignItems: 'center', color: '#2980b9', fontWeight: 'bold' },
    detailRow: { backgroundColor: '#fff', borderBottom:'1px solid #eee', fontSize:'0.9em' },
    detailCell: { padding: '8px 10px', paddingLeft: '50px' },
    
    badge: { fontSize: '0.8em', padding: '2px 6px', borderRadius: '4px', backgroundColor: '#95a5a6', color: 'white', marginLeft: '10px' },
    submittedBadge: { fontSize: '0.8em', padding: '2px 6px', borderRadius: '4px', backgroundColor: '#27ae60', color: 'white' }
  };

  return (
    <div style={styles.container}>
      <div style={styles.panel}>
        <div style={styles.header}>
          <div style={{display:'flex', alignItems:'center', gap:'10px'}}>
            <h2>見積検索</h2>
            <input style={styles.input} placeholder="得意先名で検索" value={searchText} onChange={(e) => setSearchText(e.target.value)} onKeyPress={(e) => e.key === 'Enter' && executeSearch()} />
            <button style={styles.btn} onClick={executeSearch}>検 索</button>
          </div>
          <div>
            <button style={{...styles.btn, backgroundColor:'#2ecc71'}} onClick={onCreateNew}>＋ 新規作成</button>
            <button style={{...styles.btn, backgroundColor:'#95a5a6'}} onClick={onLogout}>ログアウト</button>
          </div>
        </div>

        {/* 階層表示 */}
        {sortedProjectKeys.length > 0 ? (
          sortedProjectKeys.map(projectKey => {
            const estimateGroups = groupedData[projectKey];
            const estimateKeys = Object.keys(estimateGroups).sort().reverse();
            const isProjectOpen = openGroups[projectKey] !== false; // デフォルトOpen

            return (
              <div key={projectKey} style={{marginBottom:'5px'}}>
                {/* 第1階層: 案件名 */}
                <div style={styles.projectRow} onClick={() => toggleGroup(projectKey)}>
                  <span>📁 {projectKey}</span>
                  <span style={{fontSize:'1.2em'}}>{isProjectOpen ? '−' : '＋'}</span>
                </div>

                {isProjectOpen && (
                  <div style={{borderLeft:'4px solid #dfe6e9', marginLeft:'10px'}}>
                    {estimateKeys.map(baseNo => {
                      const list = estimateGroups[baseNo];
                      // 枝番でソート (01, 02...)
                      list.sort((a, b) => (a.estimateNo || '').localeCompare(b.estimateNo || ''));
                      
                      const groupKey = `${projectKey}-${baseNo}`;
                      const isGroupOpen = openGroups[groupKey] !== false;

                      return (
                        <div key={baseNo}>
                          {/* 第2階層: 見積番号(親) */}
                          <div style={styles.estimateGroupRow} onClick={() => toggleGroup(groupKey)}>
                            <span style={{marginRight:'10px'}}>{isGroupOpen ? '▼' : '▶'}</span>
                            <span>📄 {baseNo} シリーズ ({list.length}件)</span>
                          </div>

                          {/* 第3階層: 各枝番 */}
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
          <div style={{padding:'20px', textAlign:'center', color:'#999'}}>データがありません</div>
        )}
      </div>
    </div>
  );
};