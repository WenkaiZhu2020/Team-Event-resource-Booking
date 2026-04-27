interface NoticeProps {
  message: string | null;
  tone: 'success' | 'error';
}

export function Notice({ message, tone }: NoticeProps) {
  if (!message) {
    return null;
  }

  return <div className={`notice ${tone}`}>{message}</div>;
}
