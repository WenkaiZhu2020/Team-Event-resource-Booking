import type { FormEvent } from 'react';
import { SectionPanel } from '../../components/SectionPanel';
import type { BookingDraft, BookingItem, EventItem, ResourceItem } from '../../types';

interface BookingsPanelProps {
  bookings: BookingItem[];
  resources: ResourceItem[];
  events: EventItem[];
  draft: BookingDraft;
  loading: boolean;
  onDraftChange: (draft: BookingDraft) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onCancel: (bookingId: string) => void;
  onReload: () => void;
}

export function BookingsPanel(props: BookingsPanelProps) {
  const { bookings, resources, events, draft, loading, onDraftChange, onSubmit, onCancel, onReload } = props;
  const selectedResource = resources.find((resource) => resource.resourceId === draft.resourceId);
  const selectedEvent = events.find((eventItem) => eventItem.eventId === draft.linkedEventId);
  const pendingCount = bookings.filter((booking) => booking.status === 'PENDING_APPROVAL').length;
  const approvedCount = bookings.filter((booking) => booking.status === 'APPROVED').length;
  const waitlistedCount = bookings.filter((booking) => booking.status === 'WAITLISTED').length;
  const closedCount = bookings.filter((booking) => ['CANCELLED', 'REJECTED'].includes(booking.status)).length;

  return (
    <div className="stack-grid">
      <SectionPanel eyebrow="Bookings" title="Reservation control snapshot">
        <div className="insight-grid three-column">
          <article className="insight-card accent-green">
            <small>Approved reservations</small>
            <strong>{approvedCount}</strong>
            <p>Bookings that cleared policy checks and are holding an active slot in the resource schedule.</p>
          </article>
          <article className="insight-card accent-amber">
            <small>Review queue</small>
            <strong>{pendingCount}</strong>
            <p>Bookings currently routed into workflow review because the selected resource requires manager or admin approval.</p>
          </article>
          <article className="insight-card accent-blue">
            <small>Demand overflow</small>
            <strong>{waitlistedCount}</strong>
            <p>Reservations admitted into a queue instead of being allowed to double-book the same time window.</p>
          </article>
        </div>
      </SectionPanel>

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
            Linked event (optional)
            <select value={draft.linkedEventId} onChange={(event) => onDraftChange({ ...draft, linkedEventId: event.target.value })}>
              <option value="">No linked event</option>
              {events.map((eventItem) => (
                <option key={eventItem.eventId} value={eventItem.eventId}>
                  {eventItem.title}
                </option>
              ))}
            </select>
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
        <div className="insight-grid">
          <article className="insight-card">
            <small>Selected resource policy</small>
            <strong>{selectedResource?.name ?? 'Choose a resource'}</strong>
            <p>
              {selectedResource
                ? `${selectedResource.approvalMode} · waitlist ${selectedResource.allowWaitlist ? 'enabled' : 'disabled'} · ${selectedResource.maxBookingDurationMinutes} minute max booking duration · ${selectedResource.advanceBookingWindowDays} day advance window.`
                : 'The booking form will expose policy-sensitive behavior once a resource is selected.'}
            </p>
          </article>
          <article className="insight-card">
            <small>Linked event context</small>
            <strong>{selectedEvent?.title ?? 'No linked event'}</strong>
            <p>
              {selectedEvent
                ? `Booking context inherits event timing visibility from ${selectedEvent.category} at ${selectedEvent.location}.`
                : 'Linked event is optional. Resource bookings can still flow through approval and waitlist paths without event coupling.'}
            </p>
          </article>
          <article className="insight-card">
            <small>Concurrency lane</small>
            <strong>Lock, validate, admit</strong>
            <p>
              Create-booking writes pass through conflict detection, slot protection, and idempotency handling before they become confirmed state.
            </p>
          </article>
        </div>
      </SectionPanel>

      <SectionPanel
        eyebrow="Bookings"
        title="My bookings"
        actions={<button className="secondary-button" type="button" onClick={onReload}>Reload</button>}
      >
        <div className="card-list">
          {bookings.map((booking) => (
            <article className="list-card action-card showcase-card" key={booking.bookingId}>
              <div>
                <strong>{booking.resourceName}</strong>
                <p>{booking.startAt} → {booking.endAt}</p>
                <div className="micro-stat-row">
                  <span>{booking.status}</span>
                  <span>{booking.resourceType}</span>
                  <span>{booking.approvalMode}</span>
                  {booking.waitlistPosition ? <span>Waitlist #{booking.waitlistPosition}</span> : null}
                </div>
                <p>{booking.purpose}</p>
                <p>
                  {booking.linkedEventId ? `Linked event ${booking.linkedEventId}` : 'Standalone booking'}
                  {booking.decisionNote ? ` · Decision note: ${booking.decisionNote}` : ''}
                </p>
              </div>
              <div className="showcase-rail">
                <div className="showcase-meter">
                  <label>Approval timeline</label>
                  <strong>{booking.approvalRequestedAt ? 'Approval path engaged' : 'Direct path'}</strong>
                </div>
                <div className="showcase-meter">
                  <label>Decision state</label>
                  <strong>{booking.decidedAt ? 'Decided' : booking.status === 'WAITLISTED' ? 'Queued' : 'Open'}</strong>
                </div>
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

      <SectionPanel eyebrow="Engineering" title="Booking engine characteristics">
        <div className="insight-grid">
          <article className="insight-card">
            <small>Concurrency-safe write path</small>
            <strong>Slot conflict control</strong>
            <p>
              The booking service protects shared resources with lock coordination and overlap checks so two users cannot acquire the same slot at the same time.
            </p>
          </article>
          <article className="insight-card">
            <small>Idempotent command handling</small>
            <strong>Duplicate-safe creation</strong>
            <p>
              Sensitive write requests are structured so repeat submission can be detected before duplicating persisted business state.
            </p>
          </article>
          <article className="insight-card">
            <small>Outbox and downstream fan-out</small>
            <strong>Async side effects after commit</strong>
            <p>
              Once a booking transition commits, downstream analytics, workflow, and notification work can proceed from persisted handoff records.
            </p>
          </article>
          <article className="insight-card">
            <small>Waitlist progression</small>
            <strong>{waitlistedCount} queued bookings</strong>
            <p>
              Waitlisted reservations keep the demand signal visible and allow the system to promote queued users after capacity is released.
            </p>
          </article>
          <article className="insight-card">
            <small>Closed booking outcomes</small>
            <strong>{closedCount}</strong>
            <p>
              Cancelled and rejected bookings remain observable so the walkthrough can show both success paths and terminal decision outcomes.
            </p>
          </article>
          <article className="insight-card">
            <small>Approval-linked flow</small>
            <strong>{pendingCount} under review</strong>
            <p>
              Approval-required bookings demonstrate the handoff from booking policy evaluation into workflow decision handling and callback completion.
            </p>
          </article>
        </div>
      </SectionPanel>
    </div>
  );
}
