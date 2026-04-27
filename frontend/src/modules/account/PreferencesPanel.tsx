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
