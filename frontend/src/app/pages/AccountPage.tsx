import { Notice } from '../../components/Notice';
import { PreferencesPanel } from '../../modules/account/PreferencesPanel';
import { ProfilePanel } from '../../modules/account/ProfilePanel';
import { useAppContext } from '../state/AppContext';

export function AccountPage() {
  const {
    currentUser,
    error,
    loading,
    message,
    preferences,
    profile,
    profileDraft,
    setPreferences,
    setProfileDraft,
    submitPreferences,
    submitProfile
  } = useAppContext();

  return (
    <>
      <Notice message={message} tone="success" />
      <Notice message={error} tone="error" />
      <div className="stack-grid">
        <section className="panel">
          <p className="eyebrow">Account</p>
          <h3>Identity workspace</h3>
          <p className="helper-copy">
            This page surfaces the user-domain side of the platform: profile data, notification preference ownership,
            and the role-aware identity information currently attached to the signed-in session.
          </p>
          <div className="micro-stat-row">
            <span>{currentUser?.email ?? 'No session'}</span>
            {currentUser?.roles.map((role) => <span key={role}>{role}</span>)}
          </div>
        </section>
        <ProfilePanel
          profile={profile}
          draft={profileDraft}
          loading={loading}
          onChange={setProfileDraft}
          onSubmit={(event) => {
            event.preventDefault();
            void submitProfile();
          }}
        />
        <PreferencesPanel
          preferences={preferences}
          loading={loading}
          onChange={setPreferences}
          onSubmit={(event) => {
            event.preventDefault();
            void submitPreferences();
          }}
        />
      </div>
    </>
  );
}
