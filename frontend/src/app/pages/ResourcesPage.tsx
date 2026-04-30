import { useNavigate } from 'react-router-dom';
import { Notice } from '../../components/Notice';
import { ResourcesPanel } from '../../modules/resources/ResourcesPanel';
import { useAppContext } from '../state/AppContext';

export function ResourcesPage() {
  const navigate = useNavigate();
  const { error, message, resources, reloadWorkspace, seedBookingResource } = useAppContext();

  return (
    <>
      <Notice message={message} tone="success" />
      <Notice message={error} tone="error" />
      <ResourcesPanel
        resources={resources}
        onReload={() => void reloadWorkspace()}
        onUseResource={(resourceId) => {
          seedBookingResource(resourceId);
          navigate('/bookings');
        }}
      />
    </>
  );
}
