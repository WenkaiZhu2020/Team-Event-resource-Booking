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
      <div className="insight-grid three-column">
        <article className="insight-card accent-green">
          <small>Account identity</small>
          <strong>{profile?.displayName ?? '-'}</strong>
          <p>Primary user-facing display identity used across the workspace and operational views.</p>
        </article>
        <article className="insight-card accent-blue">
          <small>Role summary</small>
          <strong>{profile?.roleSummary ?? '-'}</strong>
          <p>The role set coming from the authenticated session and profile projection.</p>
        </article>
        <article className="insight-card accent-amber">
          <small>Account status</small>
          <strong>{profile?.accountStatus ?? '-'}</strong>
          <p>Current account state retained outside the auth token itself so profile and preference data stay independently owned.</p>
        </article>
      </div>
      <div className="detail-grid compact">
        <div>
          <span className="detail-label">Email</span>
          <strong>{profile?.email ?? '-'}</strong>
        </div>
        <div>
          <span className="detail-label">Timezone</span>
          <strong>{profile?.timezone ?? '-'}</strong>
        </div>
        <div>
          <span className="detail-label">Created at</span>
          <strong>{profile?.createdAt ?? '-'}</strong>
        </div>
        <div>
          <span className="detail-label">Updated at</span>
          <strong>{profile?.updatedAt ?? '-'}</strong>
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
