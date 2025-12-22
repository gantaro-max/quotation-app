import React, { useEffect, useState } from 'react';
import { type QuotationDto, type User } from '../types';
import './QuotationList.css';

interface QuotationListProps {
  currentUser: User;
  onLogout: () => void;
  onSelectQuotation: (id: number) => void;
  onCreateNew: () => void;
}

export const QuotationList: React.FC<QuotationListProps> = ({ 
  currentUser, 
  onLogout, 
  onSelectQuotation, 
  onCreateNew 
}) => {
  const [searchList, setSearchList] = useState<QuotationDto[]>([]);
  const [searchText, setSearchText] = useState('');

  // 初期ロード：自分の見積一覧を取得
  useEffect(() => {
    fetchMyList();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentUser]);

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
      
      // 必要に応じて他の検索条件もここで追加可能です
      // if(searchText) params.append('projectName', searchText);
      
      const res = await fetch(`/api/quotations/search?${params.toString()}`);
      const json = await res.json();
      if(json.success) setSearchList(json.data);
    } catch(e) { console.error(e); }
  };

  return (
    <div className="searchPanel">
      <div className="searchHeader">
        <div className="searchHeaderLeft">
          <h2>見積検索</h2>
          <input 
            className="searchInput" 
            placeholder="得意先名で検索" 
            value={searchText} 
            onChange={(e) => setSearchText(e.target.value)} 
            onKeyDown={(e) => { if(e.key === 'Enter' && !e.nativeEvent.isComposing ) executeSearch() }} 
          />
          <button className="searchBtn" onClick={executeSearch}>検 索</button>
        </div>
        <div>
          <button className="newBtn" onClick={onCreateNew}>＋ 新規作成</button>
          <button className="logoutBtn" onClick={onLogout}>ログアウト</button>
        </div>
      </div>
      <table className="searchTable">
        <thead>
          <tr>
            <th className="searchTh">見積No</th>
            <th className="searchTh">日付</th>
            <th className="searchTh">得意先名</th>
            <th className="searchTh">案件名</th>
            <th className="searchTh">金額(税込)</th>
          </tr>
        </thead>
        <tbody>
          {searchList.length > 0 ? (
            searchList.map(q => (
              <tr 
                key={q.id} 
                className="searchRow" 
                onClick={() => q.id && onSelectQuotation(q.id)}
              >
                <td className="searchTd">{q.estimateNo}</td>
                <td className="searchTd">{q.issueDate}</td>
                <td className="searchTd">{q.customerName}</td>
                <td className="searchTd">{q.projectName}</td>
                <td className="searchTd">¥{q.grandTotal?.toLocaleString()}</td>
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan={5} className="searchTd noData">データがありません</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
};