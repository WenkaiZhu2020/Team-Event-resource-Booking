import { SectionPanel } from '../../components/SectionPanel';
import type { DashboardOverview, ResourcePopularityItem } from '../../types';

interface DashboardPanelProps {
  overview: DashboardOverview;
  popularResources: ResourcePopularityItem[];
  onReload: () => void;
}

const metricLabels: Array<[keyof DashboardOverview, string]> = [
  ['totalBookings', 'Total bookings'],
  ['approvedBookings', 'Approved'],
  ['pendingApprovals', 'Pending approvals'],
  ['waitlistedBookings', 'Waitlisted'],
  ['cancelledOrRejectedBookings', 'Cancelled / rejected'],
  ['uniqueResourcesUsed', 'Unique resources used'],
  ['nextSevenDaysApprovedBookings', 'Next 7 days'],
  ['totalApprovedReservedMinutes', 'Approved minutes']
];

export function DashboardPanel({ overview, popularResources, onReload }: DashboardPanelProps) {
  const approvalRatio = overview.totalBookings ? Math.round((overview.approvedBookings / overview.totalBookings) * 100) : 0;
  const pressureRatio = overview.totalBookings ? Math.round(((overview.pendingApprovals + overview.waitlistedBookings) / overview.totalBookings) * 100) : 0;

  return (
    <div className="stack-grid">
      <SectionPanel
        eyebrow="Analytics"
        title="Dashboard overview"
        actions={<button className="secondary-button" type="button" onClick={onReload}>Reload</button>}
      >
        <div className="metrics-grid">
          {metricLabels.map(([key, label]) => (
            <article className="metric-card" key={key}>
              <small>{label}</small>
              <strong>{overview[key]}</strong>
            </article>
          ))}
        </div>
      </SectionPanel>

      <SectionPanel eyebrow="Runtime" title="System signals">
        <div className="insight-grid three-column">
          <article className="insight-card accent-amber">
            <small>Concurrency guard</small>
            <strong>{overview.pendingApprovals + overview.waitlistedBookings}</strong>
            <p>
              Requests currently flowing through approval or waitlist paths. This is the visible load on the
              conflict-safe booking pipeline.
            </p>
          </article>
          <article className="insight-card accent-green">
            <small>Approval efficiency</small>
            <strong>{approvalRatio}%</strong>
            <p>
              Share of bookings that reached approved state. This gives the dashboard a quick operational completion signal.
            </p>
          </article>
          <article className="insight-card accent-blue">
            <small>Pressure score</small>
            <strong>{pressureRatio}%</strong>
            <p>
              Pending approvals and waitlist volume expressed against total bookings. Useful for demoing system contention.
            </p>
          </article>
        </div>
      </SectionPanel>

      <SectionPanel eyebrow="Analytics" title="Popular resources">
        <div className="card-list">
          {popularResources.map((item) => (
            <article className="list-card showcase-card" key={item.resourceId}>
              <div>
                <strong>{item.resourceName}</strong>
                <p>{item.resourceType} · popularity {item.popularityScore}</p>
                <p>{item.totalBookings} total · {item.approvedBookings} approved · {item.totalReservedMinutes} minutes</p>
                <div className="micro-stat-row">
                  <span>Pending {item.pendingBookings}</span>
                  <span>Waitlist {item.waitlistedBookings}</span>
                  <span>Cancelled {item.cancelledBookings}</span>
                </div>
              </div>
              <div className="showcase-rail">
                <div className="showcase-meter">
                  <label>Demand intensity</label>
                  <strong>{Math.min(item.popularityScore, 100)} / 100</strong>
                </div>
              </div>
            </article>
          ))}
          {!popularResources.length ? <p className="empty-state">No analytics data yet.</p> : null}
        </div>
      </SectionPanel>

      <SectionPanel eyebrow="Engineering" title="Platform implementation highlights">
        <div className="insight-grid">
          <article className="insight-card">
            <small>Outbox and relay</small>
            <strong>Async delivery path</strong>
            <p>
              Booking, workflow, and notification flows use persisted handoff points so user actions can complete
              safely before downstream fan-out work executes.
            </p>
          </article>
          <article className="insight-card">
            <small>Parallel aggregation</small>
            <strong>Dashboard composition</strong>
            <p>
              The analytics view is assembled from pre-aggregated booking facts and asynchronous dashboard queries rather
              than synchronous deep joins at request time.
            </p>
          </article>
          <article className="insight-card">
            <small>Idempotent messaging</small>
            <strong>Repeat-safe consumers</strong>
            <p>
              Notification and workflow event ingestion is designed to tolerate redelivery without multiplying business effects.
            </p>
          </article>
        </div>
      </SectionPanel>
    </div>
  );
}
