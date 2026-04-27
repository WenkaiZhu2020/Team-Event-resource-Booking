import { SectionPanel } from '../../components/SectionPanel';
import type { ApprovalItem } from '../../types';

interface ApprovalsPanelProps {
  approvals: ApprovalItem[];
  loading: boolean;
  onReload: () => void;
  onApprove: (approvalId: string) => void;
  onReject: (approvalId: string) => void;
}

export function ApprovalsPanel({ approvals, loading, onReload, onApprove, onReject }: ApprovalsPanelProps) {
  return (
    <SectionPanel
      eyebrow="Workflow"
      title="Pending approvals"
      actions={<button className="secondary-button" type="button" onClick={onReload}>Reload</button>}
    >
      <div className="card-list">
        {approvals.map((approval) => (
          <article className="list-card action-card" key={approval.approvalId}>
            <div>
              <strong>{approval.title}</strong>
              <p>{approval.summary ?? approval.targetType}</p>
              <p>{approval.status} · step {approval.currentStep}/{approval.totalSteps}</p>
            </div>
            <div className="button-row">
              <button className="secondary-button" type="button" disabled={loading} onClick={() => onApprove(approval.approvalId)}>
                Approve
              </button>
              <button className="danger-button" type="button" disabled={loading} onClick={() => onReject(approval.approvalId)}>
                Reject
              </button>
            </div>
          </article>
        ))}
        {!approvals.length ? <p className="empty-state">No pending approvals.</p> : null}
      </div>
    </SectionPanel>
  );
}
