/**
 * @deprecated Story G1: replaced by `useEnvironment()` hook from
 * `app/lib/env/EnvironmentContext`. The constant survives as the resolution
 * fallback inside `resolveEnvironmentId` only — production code paths now
 * read the env id from the host-injected provider, not from this literal.
 *
 * Eslint guard (`no-restricted-syntax`) blocks any new `'DEFAULT'` literal in
 * `app/**` so this fallback can't be re-introduced by accident.
 */
export const DEFAULT_ENVIRONMENT_ID = 'DEFAULT';
