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

      <SectionPanel eyebrow="Analytics" title="Popular resources">
        <div className="card-list">
          {popularResources.map((item) => (
            <article className="list-card" key={item.resourceId}>
              <div>
                <strong>{item.resourceName}</strong>
                <p>{item.resourceType} · popularity {item.popularityScore}</p>
                <p>{item.totalBookings} total · {item.approvedBookings} approved · {item.totalReservedMinutes} minutes</p>
              </div>
            </article>
          ))}
          {!popularResources.length ? <p className="empty-state">No analytics data yet.</p> : null}
        </div>
      </SectionPanel>
    </div>
  );
}
