export function readError(value: unknown, fallback: string) {
  if (value instanceof Error) {
    return value.message;
  }
  return fallback;
}
