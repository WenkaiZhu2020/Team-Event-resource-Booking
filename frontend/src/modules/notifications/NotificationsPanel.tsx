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

  return (
    <SectionPanel
      eyebrow="Notifications"
      title={`Inbox (${unreadCount} unread)`}
      actions={<button className="secondary-button" type="button" onClick={onReload}>Reload</button>}
    >
      <div className="card-list">
        {notifications.map((item) => (
          <article className="list-card action-card" key={item.notificationId}>
            <div>
              <strong>{item.subject}</strong>
              <p>{item.body}</p>
              <p>{item.channel} · {item.status}</p>
            </div>
            <div className="button-row">
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
  );
}
