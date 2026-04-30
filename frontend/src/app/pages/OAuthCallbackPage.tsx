import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { Notice } from '../../components/Notice';
import { useAppContext } from '../state/AppContext';

export function OAuthCallbackPage() {
  const location = useLocation();
  const { completeOAuthCallback, error, oauthHandling } = useAppContext();

  useEffect(() => {
    void completeOAuthCallback(location.search);
  }, [completeOAuthCallback, location.search]);

  return (
    <section className="panel auth-panel">
      <p className="eyebrow">OAuth2</p>
      <h3>{oauthHandling ? 'Completing sign-in' : 'Redirecting'}</h3>
      <p className="helper-copy">Finishing Google sign-in.</p>
      <Notice message={error} tone="error" />
    </section>
  );
}
