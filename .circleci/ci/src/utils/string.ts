export function isBlank(value?: string) {
  return value == null || value.length <= 0;
}

export function isNotBlank(value?: string): value is string {
  return value != null && value.length > 0;
}
