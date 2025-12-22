import React, { useState } from 'react';
import { type User } from '../types';
import './LoginPage.css';

interface LoginPageProps {
  onLoginSuccess: (user: User) => void;
}

export const LoginPage: React.FC<LoginPageProps> = ({ onLoginSuccess }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleLogin = async () => {
    setError('');
    if (!email || !password) {
      setError('メールアドレスとパスワードを入力してください');
      return;
    }
    try {
      const res = await fetch('/api/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });
      const json = await res.json();
      if (json.success) {
        // ログイン成功時、デフォルトの営業所IDがない場合は仮設定
        const loggedInUser = json.data;
        const userWithBranch = { ...loggedInUser, branchId: loggedInUser.branchId || 9443 };
        // 親コンポーネントへ通知
        onLoginSuccess(userWithBranch);
      } else {
        setError(json.message || 'ログインに失敗しました');
      }
    } catch (e) {
      console.error(e);
      setError('サーバー接続エラー');
    }
  };
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    // 変換中 (IME入力中) でなく、Enterキーが押された場合のみ実行
    if (e.key === 'Enter' && !e.nativeEvent.isComposing) {
      e.preventDefault(); // フォーム送信などを防ぐおまじない
      handleLogin();
    }
  };

  return (
    <div className="loginContainer">
      <div className="loginBox">
        <h2 className="loginTitle">見積作成システム</h2>
        
        <div style={{textAlign:'left'}}>
          <label className="inputLabelText">メールアドレス</label>
          <input 
            type="text" 
            className="loginInput" 
            value={email} 
            onChange={(e) => setEmail(e.target.value)} 
            placeholder="user@saywell.co.jp" 
            onKeyDown={handleKeyDown} 
          />
        </div>
        
        <div style={{textAlign:'left'}}>
          <label className="inputLabelText">パスワード</label>
          <input 
            type="password" 
            className="loginInput" 
            value={password} 
            onChange={(e) => setPassword(e.target.value)} 
            placeholder="パスワード" 
            onKeyDown={handleKeyDown}
          />
        </div>
        
        <button className="loginButton" onClick={handleLogin}>ログイン</button>
        
        {error && <div className="errorMsg">⚠️ {error}</div>}
      </div>
    </div>
  );
};