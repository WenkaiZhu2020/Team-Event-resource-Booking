import type { FormEvent } from 'react';
import { SectionPanel } from '../../components/SectionPanel';
import type { EventDraft, EventItem, EventRegistrationItem } from '../../types';

const eventCategories = ['WORKSHOP', 'MEETING', 'SEMINAR', 'SOCIAL', 'TRAINING', 'OTHER'] as const;

interface EventsPanelProps {
  publishedEvents: EventItem[];
  myEvents: EventItem[];
  myRegistrations: EventRegistrationItem[];
  managedRegistrations: EventRegistrationItem[];
  selectedManagedEventId: string;
  draft: EventDraft;
  loading: boolean;
  onDraftChange: (draft: EventDraft) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onPublish: (eventId: string) => void;
  onCancel: (eventId: string) => void;
  onReload: () => void;
  onRegister: (eventId: string) => void;
  onCancelRegistration: (eventId: string) => void;
  onManagedEventChange: (eventId: string) => void;
  onCheckIn: (eventId: string, registrationId: string) => void;
}

export function EventsPanel(props: EventsPanelProps) {
  const {
    publishedEvents,
    myEvents,
    myRegistrations,
    managedRegistrations,
    selectedManagedEventId,
    draft,
    loading,
    onDraftChange,
    onSubmit,
    onPublish,
    onCancel,
    onReload,
    onRegister,
    onCancelRegistration,
    onManagedEventChange,
    onCheckIn
  } = props;

  const registrationsByEvent = Object.fromEntries(myRegistrations.map((item) => [item.eventId, item]));
  const approvalSensitiveEvents = myEvents.filter((item) => item.status === 'PENDING_APPROVAL').length;
  const totalCheckIns = myEvents.reduce((sum, item) => sum + item.checkedInCount, 0);
  const totalWaitlist = publishedEvents.reduce((sum, item) => sum + item.waitlistProjectedCount, 0);

  return (
    <div className="stack-grid">
      <SectionPanel eyebrow="Events" title="Event operations snapshot">
        <div className="insight-grid three-column">
          <article className="insight-card accent-green">
            <small>Published stream</small>
            <strong>{publishedEvents.length}</strong>
            <p>Live events visible to members across the workspace.</p>
          </article>
          <article className="insight-card accent-amber">
            <small>Approval-sensitive</small>
            <strong>{approvalSensitiveEvents}</strong>
            <p>Organizer-owned events currently waiting for approval-driven release.</p>
          </article>
          <article className="insight-card accent-blue">
            <small>Attendance activity</small>
            <strong>{totalCheckIns}</strong>
            <p>Completed check-ins across owned events, with {totalWaitlist} visible waitlist positions in the broader event catalog.</p>
          </article>
        </div>
      </SectionPanel>

      <SectionPanel
        eyebrow="Events"
        title="Published events"
        actions={<button className="secondary-button" type="button" onClick={onReload}>Reload</button>}
      >
        <div className="card-list">
          {publishedEvents.map((item) => {
            const registration = registrationsByEvent[item.eventId];
            return (
              <article className="list-card action-card showcase-card" key={item.eventId}>
                <div>
                  <strong>{item.title}</strong>
                  <p>{item.category} · {item.location}</p>
                  <div className="micro-stat-row">
                    <span>{item.status}</span>
                    <span>{item.attendeeProjectedCount}/{item.capacity} attendees</span>
                    <span>{item.waitlistProjectedCount} waitlisted</span>
                    <span>{item.checkedInCount} checked in</span>
                  </div>
                  <p>{item.description ?? 'Published event with live registration and attendance tracking.'}</p>
                </div>
                <div className="showcase-rail">
                  <span className="status-pill">{registration ? registration.status : 'OPEN'}</span>
                  <div className="showcase-meter">
                    <label>Registration window</label>
                    <strong>{item.registrationCloseAt ? 'Open / scheduled close' : 'Always open in current data'}</strong>
                  </div>
                  {!registration || registration.status === 'CANCELLED' ? (
                    <button className="secondary-button" type="button" disabled={loading} onClick={() => onRegister(item.eventId)}>
                      Register
                    </button>
                  ) : null}
                  {registration && registration.status !== 'CANCELLED' ? (
                    <button className="danger-button" type="button" disabled={loading} onClick={() => onCancelRegistration(item.eventId)}>
                      Cancel registration
                    </button>
                  ) : null}
                </div>
              </article>
            );
          })}
          {!publishedEvents.length ? <p className="empty-state">No published events yet.</p> : null}
        </div>
      </SectionPanel>

      <SectionPanel eyebrow="Events" title="My registrations">
        <div className="card-list">
          {myRegistrations.map((item) => (
            <article className="list-card" key={item.registrationId}>
              <div>
                <strong>{publishedEvents.find((eventItem) => eventItem.eventId === item.eventId)?.title ?? item.eventId}</strong>
                <p>{item.status}{item.waitlistPosition ? ` · waitlist #${item.waitlistPosition}` : ''}</p>
                <p>{item.checkedInAt ? `Checked in at ${item.checkedInAt}` : 'Not checked in yet'}</p>
              </div>
            </article>
          ))}
          {!myRegistrations.length ? <p className="empty-state">No event registrations yet.</p> : null}
        </div>
      </SectionPanel>

      <SectionPanel eyebrow="Events" title="My events">
        <div className="card-list">
          {myEvents.map((item) => (
            <article className="list-card action-card showcase-card" key={item.eventId}>
              <div>
                <strong>{item.title}</strong>
                <p>{item.category} · {item.location}</p>
                <div className="micro-stat-row">
                  <span>{item.status}</span>
                  <span>Capacity {item.capacity}</span>
                  <span>{item.attendeeProjectedCount} attendees</span>
                  <span>{item.waitlistProjectedCount} waitlisted</span>
                  <span>{item.checkedInCount} checked in</span>
                </div>
                <p>
                  This card reflects event lifecycle handling, organizer ownership, and operational projections used by registration and check-in flows.
                </p>
              </div>
              <div className="showcase-rail">
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

      <SectionPanel eyebrow="Events" title="Attendee check-in">
        <div className="form-grid">
          <label>
            Managed event
            <select value={selectedManagedEventId} onChange={(event) => onManagedEventChange(event.target.value)}>
              <option value="">Select an event</option>
              {myEvents.map((item) => (
                <option key={item.eventId} value={item.eventId}>{item.title}</option>
              ))}
            </select>
          </label>
          <div className="card-list">
            {managedRegistrations.map((registration) => (
              <article className="list-card action-card" key={registration.registrationId}>
                <div>
                  <strong>{registration.userId}</strong>
                  <p>{registration.status}{registration.waitlistPosition ? ` · waitlist #${registration.waitlistPosition}` : ''}</p>
                  <p>{registration.checkedInAt ? `Checked in at ${registration.checkedInAt}` : 'Pending check-in'}</p>
                </div>
                <div className="button-row">
                  {registration.status === 'REGISTERED' && !registration.checkedInAt && selectedManagedEventId ? (
                    <button className="secondary-button" type="button" disabled={loading} onClick={() => onCheckIn(selectedManagedEventId, registration.registrationId)}>
                      Check in
                    </button>
                  ) : null}
                </div>
              </article>
            ))}
            {!managedRegistrations.length ? <p className="empty-state">No registrations loaded for the selected event.</p> : null}
          </div>
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

      <SectionPanel eyebrow="Engineering" title="Event-system mechanics shown by this page">
        <div className="insight-grid">
          <article className="insight-card">
            <small>Lifecycle state</small>
            <strong>Draft to publish to approval</strong>
            <p>
              Event status transitions are visible through organizer-owned cards, including approval-sensitive publication for large events.
            </p>
          </article>
          <article className="insight-card">
            <small>Projection support</small>
            <strong>Attendance, waitlist, check-in</strong>
            <p>
              Each event surface includes attendee, waitlist, and check-in projections so the UI reflects operational state rather than static event metadata.
            </p>
          </article>
          <article className="insight-card">
            <small>Operational callback chain</small>
            <strong>Workflow-linked publication</strong>
            <p>
              Large events can enter approval paths, with decision callbacks feeding back into the event lifecycle before organizers see final published state.
            </p>
          </article>
        </div>
      </SectionPanel>
    </div>
  );
}
