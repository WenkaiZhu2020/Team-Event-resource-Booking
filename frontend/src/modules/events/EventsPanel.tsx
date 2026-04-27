import type { FormEvent } from 'react';
import { SectionPanel } from '../../components/SectionPanel';
import type { EventDraft, EventItem } from '../../types';

const eventCategories = ['WORKSHOP', 'MEETING', 'SEMINAR', 'SOCIAL', 'TRAINING', 'OTHER'] as const;

interface EventsPanelProps {
  publishedEvents: EventItem[];
  myEvents: EventItem[];
  draft: EventDraft;
  loading: boolean;
  onDraftChange: (draft: EventDraft) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onPublish: (eventId: string) => void;
  onCancel: (eventId: string) => void;
  onReload: () => void;
}

export function EventsPanel(props: EventsPanelProps) {
  const { publishedEvents, myEvents, draft, loading, onDraftChange, onSubmit, onPublish, onCancel, onReload } = props;

  return (
    <div className="stack-grid">
      <SectionPanel
        eyebrow="Events"
        title="Published events"
        actions={<button className="secondary-button" type="button" onClick={onReload}>Reload</button>}
      >
        <div className="card-list">
          {publishedEvents.map((item) => (
            <article className="list-card" key={item.eventId}>
              <div>
                <strong>{item.title}</strong>
                <p>{item.category} · {item.location}</p>
              </div>
              <span className="status-pill">{item.status}</span>
            </article>
          ))}
          {!publishedEvents.length ? <p className="empty-state">No published events yet.</p> : null}
        </div>
      </SectionPanel>

      <SectionPanel eyebrow="Events" title="My events">
        <div className="card-list">
          {myEvents.map((item) => (
            <article className="list-card action-card" key={item.eventId}>
              <div>
                <strong>{item.title}</strong>
                <p>{item.status} · Capacity {item.capacity}</p>
              </div>
              <div className="button-row">
                {item.status === 'DRAFT' ? (
                  <button className="secondary-button" type="button" disabled={loading} onClick={() => onPublish(item.eventId)}>
                    Publish
                  </button>
                ) : null}
                {item.status !== 'CANCELLED' ? (
                  <button className="danger-button" type="button" disabled={loading} onClick={() => onCancel(item.eventId)}>
                    Cancel
                  </button>
                ) : null}
              </div>
            </article>
          ))}
          {!myEvents.length ? <p className="empty-state">No organizer-owned events yet.</p> : null}
        </div>
      </SectionPanel>

      <SectionPanel eyebrow="Events" title="Create event">
        <form className="form-grid two-column" onSubmit={onSubmit}>
          <label>
            Title
            <input value={draft.title} onChange={(event) => onDraftChange({ ...draft, title: event.target.value })} required />
          </label>
          <label>
            Category
            <select value={draft.category} onChange={(event) => onDraftChange({ ...draft, category: event.target.value })}>
              {eventCategories.map((category) => <option key={category} value={category}>{category}</option>)}
            </select>
          </label>
          <label>
            Location
            <input value={draft.location} onChange={(event) => onDraftChange({ ...draft, location: event.target.value })} required />
          </label>
          <label>
            Capacity
            <input type="number" min={1} value={draft.capacity} onChange={(event) => onDraftChange({ ...draft, capacity: Number(event.target.value) })} required />
          </label>
          <label>
            Registration opens
            <input type="datetime-local" value={draft.registrationOpenAt} onChange={(event) => onDraftChange({ ...draft, registrationOpenAt: event.target.value })} />
          </label>
          <label>
            Registration closes
            <input type="datetime-local" value={draft.registrationCloseAt} onChange={(event) => onDraftChange({ ...draft, registrationCloseAt: event.target.value })} />
          </label>
          <label>
            Starts at
            <input type="datetime-local" value={draft.startAt} onChange={(event) => onDraftChange({ ...draft, startAt: event.target.value })} required />
          </label>
          <label>
            Ends at
            <input type="datetime-local" value={draft.endAt} onChange={(event) => onDraftChange({ ...draft, endAt: event.target.value })} required />
          </label>
          <label className="full-span">
            Description
            <textarea rows={4} value={draft.description} onChange={(event) => onDraftChange({ ...draft, description: event.target.value })} />
          </label>
          <button className="primary-button" type="submit" disabled={loading}>Create event</button>
        </form>
      </SectionPanel>
    </div>
  );
}
