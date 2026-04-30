import { useEffect } from 'react';
import { Navigate } from 'react-router-dom';
import { Notice } from '../../components/Notice';
import { AuthPanel } from '../../modules/auth/AuthPanel';
import { useAppContext } from '../state/AppContext';

interface AuthPageProps {
  mode: 'login' | 'register';
}

export function AuthPage({ mode }: AuthPageProps) {
  const {
    authMode,
    authenticated,
    email,
    error,
    googleEnabled,
    loading,
    message,
    password,
    setAuthMode,
    setEmail,
    setPassword,
    startGoogleLogin,
    submitAuth
  } = useAppContext();

  useEffect(() => {
    setAuthMode(mode);
  }, [mode, setAuthMode]);

  if (authenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  return (
    <>
      <Notice message={message} tone="success" />
      <Notice message={error} tone="error" />
      <AuthPanel
        authMode={authMode}
        email={email}
        password={password}
        loading={loading}
        googleEnabled={googleEnabled}
        onModeChange={setAuthMode}
        onEmailChange={setEmail}
        onPasswordChange={setPassword}
        onSubmit={(event) => {
          event.preventDefault();
          void submitAuth();
        }}
        onGoogleLogin={startGoogleLogin}
      />
    </>
  );
}
