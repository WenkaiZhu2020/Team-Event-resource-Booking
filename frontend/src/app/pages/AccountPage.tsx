import { Notice } from '../../components/Notice';
import { PreferencesPanel } from '../../modules/account/PreferencesPanel';
import { ProfilePanel } from '../../modules/account/ProfilePanel';
import { useAppContext } from '../state/AppContext';

export function AccountPage() {
  const {
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
