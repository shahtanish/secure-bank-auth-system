import { useState, useEffect } from 'react'
import Login from './components/Login'
import Dashboard from './components/Dashboard'

function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Check if user is already logged in
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      // Assuming JWT token is stored in localStorage by the Login component
      const token = localStorage.getItem('token');
      if (!token) {
        setLoading(false);
        return;
      }

      const res = await fetch('/api/user/profile', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      if (res.ok) {
        const data = await res.json();
        setUser(data.data); // data.data because ApiResponse wrapper
      } else {
        // Token might be expired
        localStorage.removeItem('token');
      }
    } catch (err) {
      console.error("Failed to fetch profile", err);
    } finally {
      setLoading(false);
    }
  };

  const handleLogin = (userData, token) => {
    localStorage.setItem('token', token);
    setUser(userData);
    fetchProfile(); // fetch full profile details
  };

  const handleLogout = async () => {
    const token = localStorage.getItem('token');
    if (token) {
      await fetch('/api/auth/logout', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });
    }
    localStorage.removeItem('token');
    setUser(null);
  };

  if (loading) {
    return (
      <div className="glass-panel" style={{ textAlign: 'center' }}>
        <div className="loader" style={{ width: 40, height: 40, borderWidth: 4 }}></div>
        <p style={{ marginTop: 20 }}>Loading Application...</p>
      </div>
    );
  }

  return (
    <>
      {!user ? (
        <Login onLogin={handleLogin} />
      ) : (
        <Dashboard user={user} onLogout={handleLogout} fetchProfile={fetchProfile} />
      )}
    </>
  )
}

export default App
