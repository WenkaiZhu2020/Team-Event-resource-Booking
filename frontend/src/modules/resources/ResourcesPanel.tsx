import { SectionPanel } from '../../components/SectionPanel';
import type { ResourceItem } from '../../types';

interface ResourcesPanelProps {
  resources: ResourceItem[];
  onReload: () => void;
  onUseResource: (resourceId: string) => void;
}

export function ResourcesPanel({ resources, onReload, onUseResource }: ResourcesPanelProps) {
  return (
    <SectionPanel
      eyebrow="Catalog"
      title="Resources"
      actions={<button className="secondary-button" type="button" onClick={onReload}>Reload</button>}
    >
      <div className="card-list">
        {resources.map((resource) => (
          <article className="list-card action-card" key={resource.resourceId}>
            <div>
              <strong>{resource.name}</strong>
              <p>{resource.type} · {resource.location} · {resource.approvalMode}</p>
            </div>
            <div className="button-row">
              <span className="status-pill">{resource.status}</span>
              <button className="secondary-button" type="button" onClick={() => onUseResource(resource.resourceId)}>
                Use in booking form
              </button>
            </div>
          </article>
        ))}
        {!resources.length ? <p className="empty-state">No resources found.</p> : null}
      </div>
    </SectionPanel>
  );
}
