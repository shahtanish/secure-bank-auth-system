import { useState } from 'react';

export default function Login({ onLogin }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [mfaCode, setMfaCode] = useState('');
  const [requiresMfa, setRequiresMfa] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const payload = { username, password };
      if (requiresMfa && mfaCode) {
        payload.mfaCode = mfaCode;
      }

      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      const data = await res.json();

      if (res.ok) {
        onLogin(data, data.accessToken);
      } else {
        if (res.status === 428) {
          setRequiresMfa(true);
          setError('MFA Code is required. Please check your authenticator app.');
        } else {
          setError(data.message || 'Login failed. Please check your credentials.');
        }
      }
    } catch (err) {
      setError('A network error occurred. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="glass-panel">
      <h1>SecureBank</h1>
      <p className="subtitle">Enter your credentials to access your account</p>

      {error && <div className="error-message">{error}</div>}

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label>Username</label>
          <input 
            type="text" 
            value={username} 
            onChange={e => setUsername(e.target.value)}
            placeholder="e.g. jdoe"
            required 
            disabled={loading || requiresMfa}
          />
        </div>

        <div className="form-group">
          <label>Password</label>
          <input 
            type="password" 
            value={password} 
            onChange={e => setPassword(e.target.value)}
            placeholder="••••••••"
            required 
            disabled={loading || requiresMfa}
          />
        </div>

        {requiresMfa && (
          <div className="form-group">
            <label>MFA Code</label>
            <input 
              type="text" 
              value={mfaCode} 
              onChange={e => setMfaCode(e.target.value)}
              placeholder="000000"
              required 
              disabled={loading}
              autoFocus
            />
          </div>
        )}

        <button type="submit" disabled={loading || !username || !password}>
          {loading ? <span className="loader"></span> : (requiresMfa ? 'Verify MFA & Login' : 'Secure Login')}
        </button>
      </form>
    </div>
  );
}
