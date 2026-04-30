import { Notice } from '../../components/Notice';
import { NotificationsPanel } from '../../modules/notifications/NotificationsPanel';
import { useAppContext } from '../state/AppContext';

export function NotificationsPage() {
  const { error, loading, markNotificationReadAction, message, notifications, reloadWorkspace, unreadCount } = useAppContext();

  return (
    <>
      <Notice message={message} tone="success" />
      <Notice message={error} tone="error" />
      <NotificationsPanel
        notifications={notifications}
        unreadCount={unreadCount}
        loading={loading}
        onReload={() => void reloadWorkspace()}
        onMarkRead={(notificationId) => void markNotificationReadAction(notificationId)}
      />
    </>
  );
}
