import type { FormEvent } from 'react';
import type { UserProfile } from '../../types';
import { SectionPanel } from '../../components/SectionPanel';

interface ProfilePanelProps {
  profile: UserProfile | null;
  draft: { displayName: string; timezone: string };
  loading: boolean;
  onChange: (draft: { displayName: string; timezone: string }) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}

export function ProfilePanel({ profile, draft, loading, onChange, onSubmit }: ProfilePanelProps) {
  return (
    <SectionPanel eyebrow="Identity" title="Profile">
      <div className="detail-grid compact">
        <div>
          <span className="detail-label">Email</span>
          <strong>{profile?.email ?? '-'}</strong>
        </div>
        <div>
          <span className="detail-label">Roles</span>
          <strong>{profile?.roleSummary ?? '-'}</strong>
        </div>
      </div>
      <form className="form-grid" onSubmit={onSubmit}>
        <label>
          Display name
          <input
            value={draft.displayName}
            onChange={(event) => onChange({ ...draft, displayName: event.target.value })}
            maxLength={120}
            required
          />
        </label>
        <label>
          Timezone
          <input
            value={draft.timezone}
            onChange={(event) => onChange({ ...draft, timezone: event.target.value })}
            maxLength={64}
            required
          />
        </label>
        <button className="primary-button" type="submit" disabled={loading}>Save profile</button>
      </form>
    </SectionPanel>
  );
}
