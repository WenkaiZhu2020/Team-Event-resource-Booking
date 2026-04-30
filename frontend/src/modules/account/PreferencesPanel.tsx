import type { FormEvent } from 'react';
import type { NotificationPreference } from '../../types';
import { SectionPanel } from '../../components/SectionPanel';

interface PreferencesPanelProps {
  preferences: NotificationPreference;
  loading: boolean;
  onChange: (preferences: NotificationPreference) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}

export function PreferencesPanel({ preferences, loading, onChange, onSubmit }: PreferencesPanelProps) {
  return (
    <SectionPanel eyebrow="Preferences" title="Notifications">
      <div className="insight-grid three-column">
        <article className="insight-card accent-green">
          <small>In-app lane</small>
          <strong>{preferences.inAppEnabled ? 'Enabled' : 'Disabled'}</strong>
          <p>User-visible notification records remain available in the inbox when this lane is active.</p>
        </article>
        <article className="insight-card accent-blue">
          <small>Email lane</small>
          <strong>{preferences.emailEnabled ? 'Enabled' : 'Disabled'}</strong>
          <p>Email-simulation delivery can be toggled independently from the in-app read model.</p>
        </article>
        <article className="insight-card accent-amber">
          <small>Reminder window</small>
          <strong>{preferences.reminderMinutesBefore} min</strong>
          <p>Lead time used by reminder-oriented notification paths before the event or booking moment arrives.</p>
        </article>
      </div>
      <form className="form-grid" onSubmit={onSubmit}>
        <label className="checkbox-row">
          <input
            type="checkbox"
            checked={preferences.inAppEnabled}
            onChange={(event) => onChange({ ...preferences, inAppEnabled: event.target.checked })}
          />
          Enable in-app notifications
        </label>
        <label className="checkbox-row">
          <input
            type="checkbox"
            checked={preferences.emailEnabled}
            onChange={(event) => onChange({ ...preferences, emailEnabled: event.target.checked })}
          />
          Enable email notifications
        </label>
        <label>
          Reminder lead time (minutes)
          <input
            type="number"
            min={5}
            max={1440}
            value={preferences.reminderMinutesBefore}
            onChange={(event) => onChange({ ...preferences, reminderMinutesBefore: Number(event.target.value) })}
          />
        </label>
        <button className="primary-button" type="submit" disabled={loading}>Save preferences</button>
      </form>
    </SectionPanel>
  );
}
