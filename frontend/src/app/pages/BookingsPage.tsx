import { Notice } from '../../components/Notice';
import { BookingsPanel } from '../../modules/bookings/BookingsPanel';
import { useAppContext } from '../state/AppContext';

export function BookingsPage() {
  const {
    bookingDraft,
    bookings,
    cancelBookingAction,
    createBookingAction,
    error,
    loading,
    message,
    publishedEvents,
    reloadWorkspace,
    resources,
    setBookingDraft
  } = useAppContext();

  return (
    <>
      <Notice message={message} tone="success" />
      <Notice message={error} tone="error" />
      <BookingsPanel
        bookings={bookings}
        resources={resources}
        events={publishedEvents}
        draft={bookingDraft}
        loading={loading}
        onDraftChange={setBookingDraft}
        onSubmit={(event) => {
          event.preventDefault();
          void createBookingAction();
        }}
        onReload={() => void reloadWorkspace()}
        onCancel={(bookingId) => void cancelBookingAction(bookingId)}
      />
    </>
  );
}
