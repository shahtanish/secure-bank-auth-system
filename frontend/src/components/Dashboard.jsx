import { useState, useEffect } from 'react';

export default function Dashboard({ user, onLogout, fetchProfile }) {
  const [setupMfaData, setSetupMfaData] = useState(null);
  const [mfaCode, setMfaCode] = useState('');
  const [mfaError, setMfaError] = useState('');
  const [mfaSuccess, setMfaSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [accounts, setAccounts] = useState([]);
  const [loadingAccounts, setLoadingAccounts] = useState(true);

  useEffect(() => {
    if (user.mfaEnabled || !setupMfaData) {
        fetchAccounts();
    }
  }, [user]);

  const fetchAccounts = async () => {
    try {
      const token = localStorage.getItem('token');
      const res = await fetch('/api/v1/accounts', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.ok) {
        const data = await res.json();
        setAccounts(data.accounts);
      }
    } catch (err) {
      console.error('Failed to fetch accounts', err);
    } finally {
      setLoadingAccounts(false);
    }
  };

  const handleSetupMfa = async () => {
    setLoading(true);
    setMfaError('');
    try {
      const token = localStorage.getItem('token');
      const res = await fetch('/api/auth/mfa/setup', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const data = await res.json();
      if (res.ok) {
        setSetupMfaData(data.data);
      } else {
        setMfaError(data.message || 'Failed to setup MFA');
      }
    } catch (err) {
      setMfaError('Network error');
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmMfa = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMfaError('');
    try {
      const token = localStorage.getItem('token');
      const res = await fetch('/api/auth/mfa/confirm', {
        method: 'POST',
        headers: { 
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ code: mfaCode })
      });
      const data = await res.json();
      if (res.ok) {
        setMfaSuccess('MFA Successfully Enabled!');
        setSetupMfaData(null);
        fetchProfile(); // Refresh profile to show MFA is enabled
      } else {
        setMfaError(data.message || 'Invalid code');
      }
    } catch (err) {
      setMfaError('Network error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="glass-panel dashboard-container" style={{ maxWidth: '900px' }}>
      <div className="dashboard-header">
        <h2>Welcome, {user.username}</h2>
        <button onClick={onLogout} style={{ width: 'auto', padding: '8px 16px', background: 'rgba(255,255,255,0.1)' }}>Logout</button>
      </div>

      <div style={{ display: 'flex', gap: '24px', flexWrap: 'wrap' }}>
        <div style={{ flex: '1', minWidth: '300px' }}>
          <div className="profile-card">
            <h3 style={{ marginBottom: 16 }}>Profile Information</h3>
            <div className="profile-row">
              <span className="profile-label">Email</span>
              <span className="profile-value">{user.email}</span>
            </div>
            <div className="profile-row">
              <span className="profile-label">Role</span>
              <span className="profile-value"><span className="badge badge-success">{user.role}</span></span>
            </div>
            <div className="profile-row">
              <span className="profile-label">MFA Status</span>
              <span className="profile-value">
                {user.mfaEnabled ? 
                  <span className="badge badge-success">Enabled</span> : 
                  <span className="badge badge-danger">Disabled</span>
                }
              </span>
            </div>
          </div>

          {!user.mfaEnabled && !setupMfaData && (
            <div style={{ textAlign: 'center', background: 'rgba(0,0,0,0.2)', padding: 20, borderRadius: 16 }}>
              <p style={{ marginBottom: 16, color: 'var(--text-muted)' }}>Secure your account by enabling Two-Factor Authentication.</p>
              <button onClick={handleSetupMfa} disabled={loading} style={{ maxWidth: 200 }}>
                {loading ? <span className="loader"></span> : 'Enable MFA'}
              </button>
              {mfaError && <div className="error-message" style={{ marginTop: 16 }}>{mfaError}</div>}
            </div>
          )}

          {mfaSuccess && <div className="success-message">{mfaSuccess}</div>}

          {setupMfaData && (
            <div className="glass-panel mfa-setup-box" style={{ padding: 24, marginTop: 24 }}>
              <h3>Setup Authenticator App</h3>
              <p className="subtitle">Scan this QR code with Google Authenticator or Authy</p>
              
              <div className="qr-placeholder">
                <img src={`https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(setupMfaData.otpAuthUri)}`} alt="MFA QR Code" />
              </div>
              
              <p style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 16 }}>
                Manual Entry Code: <strong style={{ color: 'white' }}>{setupMfaData.secret}</strong>
              </p>

              <form onSubmit={handleConfirmMfa}>
                {mfaError && <div className="error-message">{mfaError}</div>}
                <div className="form-group">
                  <input 
                    type="text" 
                    placeholder="Enter 6-digit code" 
                    value={mfaCode}
                    onChange={e => setMfaCode(e.target.value)}
                    required
                    maxLength="6"
                  />
                </div>
                <button type="submit" disabled={loading || mfaCode.length < 6}>
                  {loading ? <span className="loader"></span> : 'Verify Code'}
                </button>
              </form>
            </div>
          )}
        </div>

        <div style={{ flex: '2', minWidth: '400px' }}>
          <div className="profile-card" style={{ height: '100%' }}>
            <h3 style={{ marginBottom: 16 }}>Your Accounts</h3>
            {loadingAccounts ? (
              <div style={{ textAlign: 'center', padding: '40px' }}><span className="loader"></span></div>
            ) : accounts.length === 0 ? (
              <p style={{ color: 'var(--text-muted)' }}>No accounts found.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                {accounts.map(acc => (
                  <div key={acc.accountNumber} style={{ background: 'rgba(255,255,255,0.05)', padding: '16px', borderRadius: '12px', border: '1px solid var(--glass-border)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '12px' }}>
                      <div>
                        <h4 style={{ margin: 0, color: 'white' }}>{acc.productName}</h4>
                        <p style={{ margin: '4px 0 0 0', color: 'var(--text-muted)', fontSize: '14px', letterSpacing: '1px' }}>{acc.maskedAccountNumber}</p>
                      </div>
                      <span className={`badge ${acc.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'}`}>{acc.status}</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span className="profile-label">{acc.accountType}</span>
                      <span style={{ fontSize: '20px', fontWeight: 'bold', color: 'white' }}>
                        {acc.availableBalance.toLocaleString('en-US', { style: 'currency', currency: acc.currency })}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
