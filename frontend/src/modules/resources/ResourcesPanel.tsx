import { SectionPanel } from '../../components/SectionPanel';
import type { ResourceItem } from '../../types';

interface ResourcesPanelProps {
  resources: ResourceItem[];
  onReload: () => void;
  onUseResource: (resourceId: string) => void;
}

export function ResourcesPanel({ resources, onReload, onUseResource }: ResourcesPanelProps) {
  const approvalRequired = resources.filter((resource) => resource.requiresApproval).length;
  const waitlistEnabled = resources.filter((resource) => resource.allowWaitlist).length;
  const activeResources = resources.filter((resource) => resource.status === 'ACTIVE').length;

  return (
    <div className="stack-grid">
      <SectionPanel
        eyebrow="Catalog"
        title="Resources"
        actions={<button className="secondary-button" type="button" onClick={onReload}>Reload</button>}
      >
        <div className="insight-grid three-column">
          <article className="insight-card accent-green">
            <small>Active inventory</small>
            <strong>{activeResources}</strong>
            <p>Resources currently available to the booking engine and visible in the catalog.</p>
          </article>
          <article className="insight-card accent-amber">
            <small>Approval protected</small>
            <strong>{approvalRequired}</strong>
            <p>Resources that force manager or admin review before a reservation can move to approved state.</p>
          </article>
          <article className="insight-card accent-blue">
            <small>Waitlist ready</small>
            <strong>{waitlistEnabled}</strong>
            <p>Resources configured to queue demand instead of immediately rejecting overlap or contention scenarios.</p>
          </article>
        </div>
      </SectionPanel>

      <SectionPanel eyebrow="Catalog" title="Resource workspace">
        <div className="card-list">
          {resources.map((resource) => (
            <article className="list-card action-card showcase-card" key={resource.resourceId}>
              <div>
                <strong>{resource.name}</strong>
                <p>{resource.type} · {resource.location}</p>
                <div className="micro-stat-row">
                  <span>{resource.approvalMode}</span>
                  <span>Waitlist {resource.allowWaitlist ? 'enabled' : 'disabled'}</span>
                  <span>{resource.capacity ? `Capacity ${resource.capacity}` : 'Shared inventory'}</span>
                </div>
                <p>{resource.description ?? 'Shared internal resource with policy-based booking control.'}</p>
              </div>
              <div className="showcase-rail">
                <span className="status-pill">{resource.status}</span>
                <div className="rail-stack">
                  <div className="showcase-meter">
                    <label>Booking rule</label>
                    <strong>{resource.maxBookingDurationMinutes} min max</strong>
                  </div>
                  <div className="showcase-meter">
                    <label>Advance window</label>
                    <strong>{resource.advanceBookingWindowDays} days</strong>
                  </div>
                </div>
                <button className="secondary-button" type="button" onClick={() => onUseResource(resource.resourceId)}>
                  Use in booking form
                </button>
              </div>
            </article>
          ))}
          {!resources.length ? <p className="empty-state">No resources found.</p> : null}
        </div>
      </SectionPanel>

      <SectionPanel eyebrow="Engineering" title="Booking-control mechanics shown by this page">
        <div className="insight-grid">
          <article className="insight-card">
            <small>Policy strategy</small>
            <strong>Approval behavior by mode</strong>
            <p>
              Resource approval mode drives whether bookings auto-approve, require a manager, or route into admin review.
            </p>
          </article>
          <article className="insight-card">
            <small>Conflict safety</small>
            <strong>Availability and overlap control</strong>
            <p>
              Availability rules, maintenance windows, and booking-time conflict checks are combined before a slot can be accepted.
            </p>
          </article>
          <article className="insight-card">
            <small>Practical concurrency</small>
            <strong>Queue instead of collision</strong>
            <p>
              Waitlist-enabled resources can absorb demand spikes without double-booking the same asset or facility.
            </p>
          </article>
        </div>
      </SectionPanel>
    </div>
  );
}
