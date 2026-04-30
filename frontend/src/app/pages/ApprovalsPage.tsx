import { Notice } from '../../components/Notice';
import { ApprovalsPanel } from '../../modules/workflows/ApprovalsPanel';
import { useAppContext } from '../state/AppContext';

export function ApprovalsPage() {
  const { approveAction, approvals, canReviewApprovals, error, loading, message, rejectAction, reloadWorkspace } = useAppContext();

  return (
    <>
      <Notice message={message} tone="success" />
      <Notice message={error} tone="error" />
      {canReviewApprovals ? (
        <ApprovalsPanel
          approvals={approvals}
          loading={loading}
          onReload={() => void reloadWorkspace()}
          onApprove={(approvalId) => void approveAction(approvalId)}
          onReject={(approvalId) => void rejectAction(approvalId)}
        />
      ) : (
        <section className="panel">
          <p className="eyebrow">Workflow</p>
          <h3>Access restricted</h3>
          <p className="helper-copy">Approvals are visible only to resource managers and administrators.</p>
        </section>
      )}
    </>
  );
}
