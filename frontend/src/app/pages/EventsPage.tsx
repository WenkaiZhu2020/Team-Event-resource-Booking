import { Notice } from '../../components/Notice';
import { EventsPanel } from '../../modules/events/EventsPanel';
import { useAppContext } from '../state/AppContext';

export function EventsPage() {
  const {
    cancelEventAction,
    cancelEventRegistrationAction,
    checkInRegistrationAction,
    changeManagedEvent,
    createEventAction,
    error,
    eventDraft,
    loading,
    managedRegistrations,
    managedEventId,
    message,
    myEventRegistrations,
    myEvents,
    publishEventAction,
    publishedEvents,
    registerForEventAction,
    reloadWorkspace,
    setEventDraft
  } = useAppContext();

  return (
    <>
      <Notice message={message} tone="success" />
      <Notice message={error} tone="error" />
      <EventsPanel
        publishedEvents={publishedEvents}
        myEvents={myEvents}
        myRegistrations={myEventRegistrations}
        managedRegistrations={managedRegistrations}
        selectedManagedEventId={managedEventId}
        draft={eventDraft}
        loading={loading}
        onDraftChange={setEventDraft}
        onSubmit={(event) => {
          event.preventDefault();
          void createEventAction();
        }}
        onReload={() => void reloadWorkspace()}
        onPublish={(eventId) => void publishEventAction(eventId)}
        onCancel={(eventId) => void cancelEventAction(eventId)}
        onRegister={(eventId) => void registerForEventAction(eventId)}
        onCancelRegistration={(eventId) => void cancelEventRegistrationAction(eventId)}
        onManagedEventChange={(eventId) => {
          void changeManagedEvent(eventId);
        }}
        onCheckIn={(eventId, registrationId) => void checkInRegistrationAction(eventId, registrationId)}
      />
    </>
  );
}
