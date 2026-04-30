import { useEffect, useMemo, useState } from 'react';
import { SectionPanel } from '../../components/SectionPanel';
import type {
  ApprovalItem,
  AuthUser,
  BookingItem,
  DashboardOverview,
  EventItem,
  NotificationItem,
  ResourceItem,
  ResourcePopularityItem
} from '../../types';

interface SystemMonitorPanelProps {
  approvals: ApprovalItem[];
  bookings: BookingItem[];
  currentUser: AuthUser | null;
  dashboardOverview: DashboardOverview;
  myEvents: EventItem[];
  notifications: NotificationItem[];
  popularResources: ResourcePopularityItem[];
  publishedEvents: EventItem[];
  resources: ResourceItem[];
  unreadCount: number;
  onReload: () => void;
}

type HealthState = 'UP' | 'DOWN' | 'UNKNOWN';

type ServiceNode = {
  key: string;
  name: string;
  proxyBase: string;
  port: number;
  layer: 'Edge' | 'Identity' | 'Core Domain' | 'Workflow' | 'Messaging' | 'Analytics';
  schema: string;
  primaryRole: string;
  syncDependencies: string[];
  asyncFlows: string[];
  patterns: string[];
  concurrency: string[];
};

type HealthSnapshot = {
  status: HealthState;
  checkedAt: string;
  details: string;
};

const serviceNodes: ServiceNode[] = [
  {
    key: 'gateway',
    name: 'API Gateway',
    proxyBase: '/__monitor/gateway',
    port: 8080,
    layer: 'Edge',
    schema: 'n/a',
    primaryRole: 'JWT validation, route forwarding, request entry, front-door policy',
    syncDependencies: ['auth-service', 'user-service', 'event-service', 'resource-service', 'booking-service', 'notification-service', 'workflow-service', 'analytics-service'],
    asyncFlows: [],
    patterns: ['Proxy', 'Facade', 'Security filter chain'],
    concurrency: ['Reactive request fan-out', 'Trace boundary at edge']
  },
  {
    key: 'auth',
    name: 'Auth Service',
    proxyBase: '/__monitor/auth',
    port: 8081,
    layer: 'Identity',
    schema: 'auth',
    primaryRole: 'Registration, login, token issuance, Google OAuth entry, role claim propagation',
    syncDependencies: ['user-service'],
    asyncFlows: [],
    patterns: ['Facade', 'Adapter', 'Repository'],
    concurrency: ['Concurrent session issuance', 'Idempotent account lookup']
  },
  {
    key: 'user',
    name: 'User Service',
    proxyBase: '/__monitor/user',
    port: 8082,
    layer: 'Identity',
    schema: 'users',
    primaryRole: 'Profile data, account metadata, notification preference ownership',
    syncDependencies: [],
    asyncFlows: [],
    patterns: ['Repository', 'Service layer', 'DTO mapping'],
    concurrency: ['Low-contention profile updates']
  },
  {
    key: 'event',
    name: 'Event Service',
    proxyBase: '/__monitor/event',
    port: 8083,
    layer: 'Core Domain',
    schema: 'events',
    primaryRole: 'Event lifecycle, registration, waitlist, check-in, approval-sensitive publication',
    syncDependencies: ['workflow-service'],
    asyncFlows: ['event reminder candidate feed'],
    patterns: ['State-oriented lifecycle', 'Repository', 'Domain event boundary'],
    concurrency: ['Registration capacity protection', 'Waitlist promotion']
  },
  {
    key: 'resource',
    name: 'Resource Service',
    proxyBase: '/__monitor/resource',
    port: 8084,
    layer: 'Core Domain',
    schema: 'resources',
    primaryRole: 'Resource catalog, availability policy, maintenance windows, approval modes',
    syncDependencies: [],
    asyncFlows: [],
    patterns: ['Strategy', 'Factory Method', 'Specification'],
    concurrency: ['Availability conflict checks', 'Policy gate before booking']
  },
  {
    key: 'booking',
    name: 'Booking Service',
    proxyBase: '/__monitor/booking',
    port: 8085,
    layer: 'Core Domain',
    schema: 'bookings',
    primaryRole: 'Conflict-safe reservation, idempotent create, waitlist, outbox and approval handoff',
    syncDependencies: ['resource-service', 'event-service', 'workflow-service'],
    asyncFlows: ['booking.* domain events to notification / analytics / workflow'],
    patterns: ['Facade', 'State', 'Command', 'Outbox', 'Repository'],
    concurrency: ['Resource lock row bootstrap', 'Overlap detection', 'Waitlist promotion']
  },
  {
    key: 'notification',
    name: 'Notification Service',
    proxyBase: '/__monitor/notification',
    port: 8086,
    layer: 'Messaging',
    schema: 'notifications',
    primaryRole: 'In-app records, email simulation, delivery pipeline, unread state',
    syncDependencies: [],
    asyncFlows: ['booking.* consumption', 'future reminder fan-out'],
    patterns: ['Facade', 'Strategy', 'Decorator', 'Observer'],
    concurrency: ['Idempotent consumer handling', 'Retry-ready delivery path']
  },
  {
    key: 'workflow',
    name: 'Workflow Service',
    proxyBase: '/__monitor/workflow',
    port: 8087,
    layer: 'Workflow',
    schema: 'workflows',
    primaryRole: 'Approval request lifecycle, step persistence, callback completion, outbox relay',
    syncDependencies: ['booking-service', 'resource-service', 'event-service'],
    asyncFlows: ['approval relay publication', 'booking approval observer ingress'],
    patterns: ['Facade', 'Command', 'Chain of Responsibility', 'State', 'Outbox'],
    concurrency: ['Final-state protection', 'Duplicate decision prevention']
  },
  {
    key: 'analytics',
    name: 'Analytics Service',
    proxyBase: '/__monitor/analytics',
    port: 8088,
    layer: 'Analytics',
    schema: 'analytics',
    primaryRole: 'Booking facts, resource popularity, dashboard overview, asynchronous aggregation',
    syncDependencies: [],
    asyncFlows: ['booking-derived fact updates'],
    patterns: ['Facade', 'Builder', 'Observer-style event ingestion'],
    concurrency: ['Parallel dashboard composition', 'Scheduled aggregation refresh']
  }
];

const schemaCards = [
  {
    schema: 'auth',
    owns: ['app_users', 'app_user_roles', 'oauth identities and token session data'],
    highlights: ['token claims source', 'role propagation boundary', 'registration root']
  },
  {
    schema: 'users',
    owns: ['user_profiles', 'notification_preferences'],
    highlights: ['profile ownership', 'account metadata', 'frontend-facing preference state']
  },
  {
    schema: 'events',
    owns: ['events', 'event_registrations'],
    highlights: ['publication lifecycle', 'registration projections', 'check-in trail']
  },
  {
    schema: 'resources',
    owns: ['resources', 'availability_rules', 'maintenance_slots'],
    highlights: ['policy configuration', 'availability guardrails', 'resource lifecycle']
  },
  {
    schema: 'bookings',
    owns: ['bookings', 'booking_locks', 'idempotency_records', 'outbox_messages'],
    highlights: ['conflict prevention', 'write deduplication', 'async publication staging']
  },
  {
    schema: 'workflows',
    owns: ['approval_requests', 'approval_steps', 'approval_decision_history', 'workflow_outbox', 'consumed_messages'],
    highlights: ['approval state', 'step progression', 'relay and idempotent ingress']
  },
  {
    schema: 'notifications',
    owns: ['notification_records', 'processed_events', 'delivery attempts and delivery state'],
    highlights: ['user-visible delivery state', 'read model', 'repeat-safe event handling']
  },
  {
    schema: 'analytics',
    owns: ['booking_facts', 'resource_popularity'],
    highlights: ['read-optimized aggregates', 'dashboard totals', 'operational trend surfaces']
  }
];

const messageFlows = [
  {
    title: 'Booking approval request flow',
    summary: 'A create-booking action can become a workflow request when resource policy requires review.',
    steps: [
      'booking-service persists booking and outbox entry',
      'workflow-service receives approval creation path',
      'approval queue becomes visible to manager or admin',
      'decision callback updates booking state'
    ]
  },
  {
    title: 'Notification fan-out flow',
    summary: 'Business state changes become user-facing messages without making the initiating request synchronous and fragile.',
    steps: [
      'booking-service emits booking.* records',
      'notification-service consumes with idempotent processed-event tracking',
      'in-app record is stored',
      'email simulation path executes as secondary channel'
    ]
  },
  {
    title: 'Analytics aggregation flow',
    summary: 'Operational metrics are derived from persisted fact data rather than expensive runtime joins.',
    steps: [
      'booking facts capture booking lifecycle states',
      'analytics aggregates popularity and totals',
      'dashboard overview is assembled from read-side models',
      'frontend monitor visualizes resulting system pressure'
    ]
  }
];

const concurrencyHighlights = [
  {
    title: 'Resource-level booking safety',
    detail: 'Booking creation coordinates lock rows, overlap checks, and idempotency handling before the reservation is admitted.'
  },
  {
    title: 'Approval finality',
    detail: 'Workflow transitions are guarded so completed approvals cannot be decided twice or rolled into inconsistent end states.'
  },
  {
    title: 'Waitlist promotion',
    detail: 'Cancelled or rejected reservations can free capacity, allowing the next waitlisted booking to be promoted in a controlled sequence.'
  },
  {
    title: 'Idempotent consumers',
    detail: 'Notification and workflow ingestion can tolerate message redelivery without multiplying downstream business effects.'
  }
];

const patternMatrix = [
  { service: 'api-gateway', patterns: ['Proxy', 'Facade', 'Security filter chain'] },
  { service: 'auth-service', patterns: ['Facade', 'Adapter', 'Repository'] },
  { service: 'user-service', patterns: ['Repository', 'Service layer', 'DTO mapping'] },
  { service: 'event-service', patterns: ['State', 'Repository', 'Lifecycle transition'] },
  { service: 'resource-service', patterns: ['Strategy', 'Factory Method', 'Specification'] },
  { service: 'booking-service', patterns: ['Facade', 'Command', 'State', 'Outbox', 'Repository'] },
  { service: 'workflow-service', patterns: ['Facade', 'Command', 'Chain of Responsibility', 'State', 'Outbox'] },
  { service: 'notification-service', patterns: ['Facade', 'Observer', 'Strategy', 'Decorator'] },
  { service: 'analytics-service', patterns: ['Facade', 'Builder', 'Observer-style aggregation'] }
];

const walkthroughBoards = [
  {
    role: 'Admin walkthrough',
    focus: 'System-wide visibility, approval queue, dashboard, and policy-governed booking outcomes',
    stops: ['Dashboard', 'System', 'Resources', 'Events', 'Approvals', 'Notifications'],
    highlights: ['service health', 'approval-sensitive events', 'admin-only resource approvals', 'analytics overview']
  },
  {
    role: 'Organizer walkthrough',
    focus: 'Event publishing, participant tracking, approval-sensitive release, and check-in workflow',
    stops: ['Events', 'Dashboard', 'Notifications'],
    highlights: ['owned events', 'registration projections', 'check-in flow', 'event approval callback']
  },
  {
    role: 'Member walkthrough',
    focus: 'Published event participation, resource booking, waitlist behavior, and personal notifications',
    stops: ['Resources', 'Bookings', 'Events', 'Notifications', 'Account'],
    highlights: ['booking creation', 'waitlist visibility', 'registration state', 'read model inbox']
  },
  {
    role: 'Resource manager walkthrough',
    focus: 'Approval handling, managed resource policy, and booking control signals',
    stops: ['Approvals', 'Resources', 'Bookings', 'System'],
    highlights: ['approval queue', 'resource policy strategy', 'conflict-safe booking', 'relay state']
  }
];

export function SystemMonitorPanel(props: SystemMonitorPanelProps) {
  const {
    approvals,
    bookings,
    currentUser,
    dashboardOverview,
    myEvents,
    notifications,
    popularResources,
    publishedEvents,
    resources,
    unreadCount,
    onReload
  } = props;

  const [health, setHealth] = useState<Record<string, HealthSnapshot>>({});
  const [checkedAt, setCheckedAt] = useState<string>('');

  useEffect(() => {
    let active = true;

    async function loadHealth() {
      const results = await Promise.all(serviceNodes.map(async (service) => {
        try {
          const response = await fetch(`${service.proxyBase}/actuator/health`);
          if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
          }
          const payload = (await response.json()) as { status?: string };
          return [service.key, {
            status: payload.status === 'UP' ? 'UP' : 'DOWN',
            checkedAt: new Date().toISOString(),
            details: payload.status === 'UP' ? 'Service reachable through local monitor proxy' : 'Health endpoint returned a non-UP payload'
          } satisfies HealthSnapshot] as const;
        } catch {
          return [service.key, {
            status: 'UNKNOWN',
            checkedAt: new Date().toISOString(),
            details: 'Monitor probe unavailable from the current frontend runtime'
          } satisfies HealthSnapshot] as const;
        }
      }));

      if (!active) {
        return;
      }

      setHealth(Object.fromEntries(results));
      setCheckedAt(new Date().toISOString());
    }

    void loadHealth();
    const intervalId = window.setInterval(loadHealth, 15000);
    return () => {
      active = false;
      window.clearInterval(intervalId);
    };
  }, []);

  const runtimeSignals = useMemo(() => {
    const approvedBookings = bookings.filter((booking) => booking.status === 'APPROVED').length;
    const approvalQueue = approvals.filter((approval) => approval.status === 'PENDING').length;
    const approvalSensitiveResources = resources.filter((resource) => resource.requiresApproval).length;
    const approvalSensitiveEvents = myEvents.filter((eventItem) => eventItem.status === 'PENDING_APPROVAL').length;
    const waitlistedBookings = bookings.filter((booking) => booking.status === 'WAITLISTED').length;
    const eventWaitlist = publishedEvents.reduce((sum, item) => sum + item.waitlistProjectedCount, 0);
    const checkedIn = publishedEvents.reduce((sum, item) => sum + item.checkedInCount, 0);

    return {
      approvedBookings,
      approvalQueue,
      approvalSensitiveResources,
      approvalSensitiveEvents,
      waitlistedBookings,
      eventWaitlist,
      checkedIn
    };
  }, [approvals, bookings, myEvents, publishedEvents, resources]);

  const serviceHealthSummary = useMemo(() => {
    const values = Object.values(health);
    return {
      up: values.filter((item) => item.status === 'UP').length,
      unknown: values.filter((item) => item.status === 'UNKNOWN').length
    };
  }, [health]);

  const topResource = popularResources[0];
  const bookingConflictGuardState = runtimeSignals.waitlistedBookings > 0 || runtimeSignals.approvalQueue > 0 ? 'Guard engaged' : 'Low contention';
  const approvalRelayState = approvals.length > 0 ? 'Relay active' : 'Relay idle';
  const notificationConsumerState = notifications.length > 0 ? 'Consumer lane active' : 'No consumed records visible';
  const analyticsRefreshState = dashboardOverview.totalBookings > 0 || popularResources.length > 0 ? 'Aggregates populated' : 'Aggregation cold';

  return (
    <div className="stack-grid">
      <SectionPanel
        eyebrow="Runtime"
        title="Live operational monitor"
        actions={<button className="secondary-button" type="button" onClick={onReload}>Reload workspace data</button>}
      >
        <div className="insight-grid">
          <article className="insight-card accent-green">
            <small>Service health</small>
            <strong>{serviceHealthSummary.up}/{serviceNodes.length}</strong>
            <p>
              Services currently responding through frontend monitor probes. Unknown nodes usually indicate the dev proxy
              is unavailable rather than a business failure.
            </p>
          </article>
          <article className="insight-card accent-amber">
            <small>Approval pressure</small>
            <strong>{runtimeSignals.approvalQueue}</strong>
            <p>
              Pending approval requests visible to the current role context. This is the clearest live indicator of gated system work.
            </p>
          </article>
          <article className="insight-card accent-blue">
            <small>Unread signal</small>
            <strong>{unreadCount}</strong>
            <p>
              Unread notifications remaining in the current user workspace after asynchronous delivery and in-app persistence.
            </p>
          </article>
        </div>
      </SectionPanel>

      <SectionPanel eyebrow="Pseudo monitor" title="Control lanes and engineering signals">
        <div className="monitor-grid">
          <article className="monitor-card accent-amber">
            <small>Booking conflict guard</small>
            <strong>{bookingConflictGuardState}</strong>
            <p>
              Derived from visible waitlisted demand, pending approvals, and active booking throughput. This simulates the reservation
              protection lane driven by lock rows and overlap checks.
            </p>
            <div className="monitor-rail">
              <span>waitlisted {runtimeSignals.waitlistedBookings}</span>
              <span>pending approvals {runtimeSignals.approvalQueue}</span>
              <span>approved {runtimeSignals.approvedBookings}</span>
            </div>
          </article>
          <article className="monitor-card accent-green">
            <small>Approval relay state</small>
            <strong>{approvalRelayState}</strong>
            <p>
              Models the workflow outbox and callback path that drives booking or event completion after an approval decision commits.
            </p>
            <div className="monitor-rail">
              <span>queue {approvals.length}</span>
              <span>multi-step {approvals.filter((item) => item.totalSteps > 1).length}</span>
              <span>booking targets {approvals.filter((item) => item.targetType === 'BOOKING').length}</span>
            </div>
          </article>
          <article className="monitor-card accent-blue">
            <small>Notification consumer lane</small>
            <strong>{notificationConsumerState}</strong>
            <p>
              Represents downstream event consumption, in-app persistence, and delivery fan-out across notification channels.
            </p>
            <div className="monitor-rail">
              <span>total {notifications.length}</span>
              <span>unread {unreadCount}</span>
              <span>failed {notifications.filter((item) => item.status === 'FAILED').length}</span>
            </div>
          </article>
          <article className="monitor-card accent-blue">
            <small>Analytics refresh lane</small>
            <strong>{analyticsRefreshState}</strong>
            <p>
              Reflects the read-side aggregation path backing the dashboard and popularity cards shown elsewhere in the application.
            </p>
            <div className="monitor-rail">
              <span>bookings {dashboardOverview.totalBookings}</span>
              <span>top resources {popularResources.length}</span>
              <span>approved minutes {dashboardOverview.totalApprovedReservedMinutes}</span>
            </div>
          </article>
        </div>
      </SectionPanel>

      <SectionPanel eyebrow="Architecture" title="Service topology">
        <div className="card-list">
          {serviceNodes.map((service) => {
            const snapshot = health[service.key];
            return (
              <article className="list-card showcase-card" key={service.key}>
                <div>
                  <strong>{service.name}</strong>
                  <p>{service.layer} · port {service.port} · schema {service.schema}</p>
                  <div className="micro-stat-row">
                    {service.patterns.map((pattern) => (
                      <span key={pattern}>{pattern}</span>
                    ))}
                  </div>
                  <p>{service.primaryRole}</p>
                  <div className="detail-grid compact">
                    <div>
                      <span className="detail-label">Synchronous dependencies</span>
                      <p>{service.syncDependencies.length ? service.syncDependencies.join(', ') : 'None'}</p>
                    </div>
                    <div>
                      <span className="detail-label">Asynchronous flows</span>
                      <p>{service.asyncFlows.length ? service.asyncFlows.join(', ') : 'None'}</p>
                    </div>
                  </div>
                </div>
                <div className="showcase-rail">
                  <span className={`status-pill health-pill ${snapshot?.status === 'UP' ? 'health-up' : snapshot?.status === 'UNKNOWN' ? 'health-unknown' : 'health-down'}`}>
                    {snapshot?.status ?? 'UNKNOWN'}
                  </span>
                  <div className="showcase-meter">
                    <label>Concurrency focus</label>
                    <strong>{service.concurrency[0]}</strong>
                  </div>
                  <div className="showcase-meter">
                    <label>Secondary guard</label>
                    <strong>{service.concurrency[1] ?? 'Layer-specific protection'}</strong>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      </SectionPanel>

      <SectionPanel eyebrow="Storage" title="PostgreSQL ownership model">
        <div className="insight-grid">
          {schemaCards.map((schema) => (
            <article className="insight-card" key={schema.schema}>
              <small>{schema.schema} schema</small>
              <strong>{schema.owns[0]}</strong>
              <p>{schema.owns.slice(1).join(', ')}</p>
              <div className="micro-stat-row">
                {schema.highlights.map((highlight) => (
                  <span key={highlight}>{highlight}</span>
                ))}
              </div>
            </article>
          ))}
        </div>
      </SectionPanel>

      <SectionPanel eyebrow="Messaging" title="RabbitMQ and outbox event flow">
        <div className="timeline-grid">
          {messageFlows.map((flow) => (
            <article className="timeline-card" key={flow.title}>
              <small>{flow.title}</small>
              <strong>{flow.summary}</strong>
              <ol>
                {flow.steps.map((step) => (
                  <li key={step}>{step}</li>
                ))}
              </ol>
            </article>
          ))}
        </div>
      </SectionPanel>

      <SectionPanel eyebrow="Flow" title="Visual business flow map">
        <div className="flow-stage">
          <article className="flow-card">
            <small>Booking request pipeline</small>
            <div className="flow-track">
              <div className="flow-node">Booking request</div>
              <div className="flow-arrow">→</div>
              <div className="flow-node emphasis">Conflict guard</div>
              <div className="flow-arrow">→</div>
              <div className="flow-node">Approval</div>
              <div className="flow-arrow">→</div>
              <div className="flow-node">Callback</div>
              <div className="flow-arrow">→</div>
              <div className="flow-node">Notification</div>
              <div className="flow-arrow">→</div>
              <div className="flow-node">Analytics</div>
            </div>
            <p className="helper-copy">
              This path visualizes the booking-service write lane, workflow approval branch, downstream notification fan-out,
              and analytics fact aggregation after the booking state settles.
            </p>
          </article>

          <article className="flow-card">
            <small>Event publication pipeline</small>
            <div className="flow-track">
              <div className="flow-node">Event publish</div>
              <div className="flow-arrow">→</div>
              <div className="flow-node emphasis">Approval</div>
              <div className="flow-arrow">→</div>
              <div className="flow-node">Callback</div>
              <div className="flow-arrow">→</div>
              <div className="flow-node">Registration</div>
              <div className="flow-arrow">→</div>
              <div className="flow-node">Reminder</div>
            </div>
            <p className="helper-copy">
              This path highlights the event-service publication lifecycle, optional approval branch, registration stage,
              and reminder-oriented notification behavior for participant communication.
            </p>
          </article>
        </div>
      </SectionPanel>

      <SectionPanel eyebrow="Control" title="Concurrency and reliability highlights">
        <div className="insight-grid">
          {concurrencyHighlights.map((item) => (
            <article className="insight-card" key={item.title}>
              <small>{item.title}</small>
              <strong>{item.title}</strong>
              <p>{item.detail}</p>
            </article>
          ))}
        </div>
      </SectionPanel>

      <SectionPanel eyebrow="Patterns" title="Design pattern map by service">
        <div className="pattern-grid">
          {patternMatrix.map((entry) => (
            <article className="pattern-card" key={entry.service}>
              <small>{entry.service}</small>
              <strong>{entry.patterns[0]}</strong>
              <div className="micro-stat-row">
                {entry.patterns.map((pattern) => (
                  <span key={pattern}>{pattern}</span>
                ))}
              </div>
            </article>
          ))}
        </div>
      </SectionPanel>

      <SectionPanel eyebrow="Frontend" title="Role-based walkthrough board">
        <div className="walkthrough-grid">
          {walkthroughBoards.map((board) => (
            <article className="walkthrough-card" key={board.role}>
              <small>{board.role}</small>
              <strong>{board.focus}</strong>
              <div className="micro-stat-row">
                {board.stops.map((stop) => (
                  <span key={stop}>{stop}</span>
                ))}
              </div>
              <p className="helper-copy">Recommended highlights: {board.highlights.join(', ')}.</p>
            </article>
          ))}
        </div>
      </SectionPanel>

      <SectionPanel eyebrow="Signals" title="Current workspace-derived system signals">
        <div className="metrics-grid">
          <article className="metric-card">
            <small>Published events</small>
            <strong>{publishedEvents.length}</strong>
          </article>
          <article className="metric-card">
            <small>Owned events</small>
            <strong>{myEvents.length}</strong>
          </article>
          <article className="metric-card">
            <small>Approval-sensitive resources</small>
            <strong>{runtimeSignals.approvalSensitiveResources}</strong>
          </article>
          <article className="metric-card">
            <small>Approval-sensitive events</small>
            <strong>{runtimeSignals.approvalSensitiveEvents}</strong>
          </article>
          <article className="metric-card">
            <small>Approved bookings in workspace</small>
            <strong>{runtimeSignals.approvedBookings}</strong>
          </article>
          <article className="metric-card">
            <small>Waitlisted bookings in workspace</small>
            <strong>{runtimeSignals.waitlistedBookings}</strong>
          </article>
          <article className="metric-card">
            <small>Event waitlist projection</small>
            <strong>{runtimeSignals.eventWaitlist}</strong>
          </article>
          <article className="metric-card">
            <small>Check-ins recorded</small>
            <strong>{runtimeSignals.checkedIn}</strong>
          </article>
        </div>
      </SectionPanel>

      <SectionPanel eyebrow="Analytics" title="Top resource and throughput interpretation">
        <div className="architecture-highlight">
          <div>
            <p className="eyebrow">Most active resource</p>
            <h3>{topResource?.resourceName ?? 'No popularity data'}</h3>
            <p className="helper-copy">
              {topResource
                ? `${topResource.resourceType} with ${topResource.totalBookings} total bookings, ${topResource.approvedBookings} approved reservations, and ${topResource.totalReservedMinutes} approved minutes.`
                : 'Popularity aggregates are currently empty.'}
            </p>
          </div>
          <div className="detail-grid">
            <div>
              <span className="detail-label">Total bookings</span>
              <strong>{dashboardOverview.totalBookings}</strong>
            </div>
            <div>
              <span className="detail-label">Unique resources used</span>
              <strong>{dashboardOverview.uniqueResourcesUsed}</strong>
            </div>
            <div>
              <span className="detail-label">Pending approvals</span>
              <strong>{dashboardOverview.pendingApprovals}</strong>
            </div>
            <div>
              <span className="detail-label">Approved minutes</span>
              <strong>{dashboardOverview.totalApprovedReservedMinutes}</strong>
            </div>
          </div>
        </div>
      </SectionPanel>

      <SectionPanel eyebrow="Audience" title="Current session perspective">
        <div className="insight-grid three-column">
          <article className="insight-card">
            <small>Current user</small>
            <strong>{currentUser?.email ?? 'No session'}</strong>
            <p>The monitor renders through the same JWT and role context as the main workspace, so visible queue data remains role-aware.</p>
          </article>
          <article className="insight-card">
            <small>Role set</small>
            <strong>{currentUser?.roles.join(', ') ?? 'Anonymous'}</strong>
            <p>Approval queue visibility, dashboard context, and navigation surfaces all depend on the token role claim carried into the frontend.</p>
          </article>
          <article className="insight-card">
            <small>Probe time</small>
            <strong>{checkedAt ? formatTimestamp(checkedAt) : 'Not yet probed'}</strong>
            <p>Health snapshots refresh on a timer so the page can behave like a lightweight runtime monitor during local walkthroughs.</p>
          </article>
        </div>
      </SectionPanel>
    </div>
  );
}

function formatTimestamp(value: string) {
  try {
    return new Date(value).toLocaleString();
  } catch {
    return value;
  }
}
