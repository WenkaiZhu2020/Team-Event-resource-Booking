import type { FormEvent } from 'react';
import { SectionPanel } from '../../components/SectionPanel';
import type { BookingDraft, BookingItem, ResourceItem } from '../../types';

interface BookingsPanelProps {
  bookings: BookingItem[];
  resources: ResourceItem[];
  draft: BookingDraft;
  loading: boolean;
  onDraftChange: (draft: BookingDraft) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onCancel: (bookingId: string) => void;
  onReload: () => void;
}

export function BookingsPanel(props: BookingsPanelProps) {
  const { bookings, resources, draft, loading, onDraftChange, onSubmit, onCancel, onReload } = props;

  return (
    <div className="stack-grid">
      <SectionPanel eyebrow="Bookings" title="Create booking">
        <form className="form-grid two-column" onSubmit={onSubmit}>
          <label>
            Resource
            <select value={draft.resourceId} onChange={(event) => onDraftChange({ ...draft, resourceId: event.target.value })} required>
              <option value="">Select a resource</option>
              {resources.map((resource) => (
                <option key={resource.resourceId} value={resource.resourceId}>
                  {resource.name} ({resource.type})
                </option>
              ))}
            </select>
          </label>
          <label>
            Linked event id (optional)
            <input value={draft.linkedEventId} onChange={(event) => onDraftChange({ ...draft, linkedEventId: event.target.value })} />
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
            Purpose
            <input value={draft.purpose} onChange={(event) => onDraftChange({ ...draft, purpose: event.target.value })} maxLength={240} required />
          </label>
          <button className="primary-button" type="submit" disabled={loading}>Create booking</button>
        </form>
      </SectionPanel>

      <SectionPanel
        eyebrow="Bookings"
        title="My bookings"
        actions={<button className="secondary-button" type="button" onClick={onReload}>Reload</button>}
      >
        <div className="card-list">
          {bookings.map((booking) => (
            <article className="list-card action-card" key={booking.bookingId}>
              <div>
                <strong>{booking.resourceName}</strong>
                <p>{booking.startAt} → {booking.endAt}</p>
                <p>{booking.status} · {booking.purpose}</p>
              </div>
              <div className="button-row">
                {booking.status !== 'CANCELLED' && booking.status !== 'REJECTED' ? (
                  <button className="danger-button" type="button" disabled={loading} onClick={() => onCancel(booking.bookingId)}>
                    Cancel
                  </button>
                ) : null}
              </div>
            </article>
          ))}
          {!bookings.length ? <p className="empty-state">No bookings yet.</p> : null}
        </div>
      </SectionPanel>
    </div>
  );
}
