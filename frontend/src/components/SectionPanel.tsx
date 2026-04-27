import type { ReactNode } from 'react';

interface SectionPanelProps {
  eyebrow?: string;
  title: string;
  actions?: ReactNode;
  children: ReactNode;
}

export function SectionPanel({ eyebrow, title, actions, children }: SectionPanelProps) {
  return (
    <section className="panel section-panel">
      <header className="section-header">
        <div>
          {eyebrow ? <p className="eyebrow">{eyebrow}</p> : null}
          <h3>{title}</h3>
        </div>
        {actions ? <div className="section-actions">{actions}</div> : null}
      </header>
      {children}
    </section>
  );
}
