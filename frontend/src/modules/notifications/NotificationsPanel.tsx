import { SectionPanel } from '../../components/SectionPanel';
import type { NotificationItem } from '../../types';

interface NotificationsPanelProps {
  notifications: NotificationItem[];
  unreadCount: number;
  loading: boolean;
  onReload: () => void;
  onMarkRead: (notificationId: string) => void;
}

export function NotificationsPanel(props: NotificationsPanelProps) {
  const { notifications, unreadCount, loading, onReload, onMarkRead } = props;
  const emailLane = notifications.filter((item) => item.channel === 'EMAIL').length;
  const inAppLane = notifications.filter((item) => item.channel === 'IN_APP').length;
  const failedLane = notifications.filter((item) => item.status === 'FAILED').length;
  const readLane = notifications.filter((item) => item.status === 'READ').length;

  return (
    <div className="stack-grid">
      <SectionPanel
        eyebrow="Notifications"
        title={`Inbox (${unreadCount} unread)`}
        actions={<button className="secondary-button" type="button" onClick={onReload}>Reload</button>}
      >
        <div className="insight-grid three-column">
          <article className="insight-card accent-green">
            <small>Unread lane</small>
            <strong>{unreadCount}</strong>
            <p>Messages still requiring user attention inside the in-app notification center.</p>
          </article>
          <article className="insight-card accent-blue">
            <small>Channel spread</small>
            <strong>{inAppLane}/{emailLane}</strong>
            <p>In-app versus email-simulation delivery volume visible in the current notification history.</p>
          </article>
          <article className="insight-card accent-amber">
            <small>Delivery outcomes</small>
            <strong>{failedLane}</strong>
            <p>Notifications in failure state. This is useful for demoing retry-oriented delivery design even when the normal path is healthy.</p>
          </article>
        </div>
      </SectionPanel>

      <SectionPanel eyebrow="Notifications" title="Notification stream">
        <div className="card-list">
          {notifications.map((item) => (
            <article className="list-card action-card showcase-card" key={item.notificationId}>
              <div>
                <strong>{item.subject}</strong>
                <p>{item.body}</p>
                <div className="micro-stat-row">
                  <span>{item.channel}</span>
                  <span>{item.status}</span>
                  <span>{item.notificationType}</span>
                  <span>{item.sourceEventType}</span>
                </div>
                <p>
                  created {item.createdAt}
                  {item.sentAt ? ` · sent ${item.sentAt}` : ''}
                  {item.failureReason ? ` · failure: ${item.failureReason}` : ''}
                </p>
              </div>
              <div className="showcase-rail">
                <div className="showcase-meter">
                  <label>Consumer state</label>
                  <strong>{item.status === 'FAILED' ? 'Needs retry path' : item.status === 'READ' ? 'Delivered and acknowledged' : 'Delivered and waiting'}</strong>
                </div>
                <div className="showcase-meter">
                  <label>Read lifecycle</label>
                  <strong>{item.readAt ? 'Read by user' : 'Unread'}</strong>
                </div>
                {item.status !== 'READ' ? (
                  <button className="secondary-button" type="button" disabled={loading} onClick={() => onMarkRead(item.notificationId)}>
                    Mark read
                  </button>
                ) : null}
              </div>
            </article>
          ))}
          {!notifications.length ? <p className="empty-state">No notifications yet.</p> : null}
        </div>
      </SectionPanel>

      <SectionPanel eyebrow="Engineering" title="Notification delivery characteristics">
        <div className="insight-grid">
          <article className="insight-card">
            <small>Observer-style fan-out</small>
            <strong>{inAppLane + emailLane} deliveries tracked</strong>
            <p>
              Notification handling is driven by downstream event observation rather than forcing delivery work into the originating booking request path.
            </p>
          </article>
          <article className="insight-card">
            <small>Channel strategy</small>
            <strong>{inAppLane} in-app / {emailLane} email</strong>
            <p>
              Delivery channels remain extensible, with the UI showing how the same business event can fan out into multiple user-facing lanes.
            </p>
          </article>
          <article className="insight-card">
            <small>Idempotent consumption</small>
            <strong>Repeat-safe ingestion</strong>
            <p>
              The notification service is designed to tolerate duplicate event delivery without multiplying visible notification records.
            </p>
          </article>
          <article className="insight-card">
            <small>Read-model persistence</small>
            <strong>{readLane} read records</strong>
            <p>
              Read state is persisted instead of treated as transient UI-only state, which makes notification history demonstrable and auditable.
            </p>
          </article>
        </div>
      </SectionPanel>
    </div>
  );
}
