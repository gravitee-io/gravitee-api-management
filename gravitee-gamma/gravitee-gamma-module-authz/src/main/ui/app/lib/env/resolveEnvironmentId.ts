/**
 * Story G1 — resolves the current environment id at federation-entry time.
 *
 * <p>Resolution order (most authoritative first):
 * <ol>
 *   <li><b>Host prop</b> — Gravitee Console passes <code>environmentId</code> to the
 *       federation entry component. This is the canonical, intended path.</li>
 *   <li><b><code>?env=&lt;id&gt;</code> URL param</b> — convenience for local dev,
 *       support, and ad-hoc debugging. Easy to inspect and override without
 *       restarting the host.</li>
 *   <li><b><code>'DEFAULT'</code> fallback</b> — Phase 1 only. Logs a one-time
 *       WARN so a deployment that forgot to wire the host prop is visible in
 *       browser logs without crashing the module. Phase 2 should remove this
 *       fallback once host wiring is mandatory across all consumers.</li>
 * </ol>
 *
 * <p>Empty / blank values from any source are skipped (treated as "not provided")
 * so a host accidentally passing <code>environmentId=""</code> doesn't lock the
 * module into a no-op state.
 */
export const FALLBACK_ENVIRONMENT_ID = 'DEFAULT';
const WARN_PREFIX = '[gravitee-gamma-module-authz/G1]';

let fallbackWarned = false;

interface ResolveOptions {
    readonly hostProp?: string | null;
    readonly url?: string;
}

export function resolveEnvironmentId(opts: ResolveOptions = {}): string {
    const fromHost = pickNonBlank(opts.hostProp);
    if (fromHost) return fromHost;

    const fromUrl = pickNonBlank(readEnvFromUrl(opts.url));
    if (fromUrl) return fromUrl;

    warnFallbackOnce();
    return FALLBACK_ENVIRONMENT_ID;
}

function pickNonBlank(value: string | null | undefined): string | null {
    if (value === null || value === undefined) return null;
    const trimmed = value.trim();
    return trimmed.length > 0 ? trimmed : null;
}

function readEnvFromUrl(url?: string): string | null {
    // SSR / non-browser test paths skip this; URL has to come from somewhere.
    const candidate = url ?? (typeof window !== 'undefined' ? window.location?.search : undefined);
    if (!candidate) return null;
    try {
        const params = new URLSearchParams(candidate.startsWith('?') ? candidate : `?${candidate.split('?')[1] ?? ''}`);
        return params.get('env');
    } catch {
        // Malformed URL — degrade silently to "no override". Worst case we hit fallback.
        return null;
    }
}

function warnFallbackOnce(): void {
    if (fallbackWarned) return;
    fallbackWarned = true;
    if (typeof console !== 'undefined' && typeof console.warn === 'function') {
        console.warn(
            `${WARN_PREFIX} no environmentId provided by host or ?env URL param; ` +
                `falling back to '${FALLBACK_ENVIRONMENT_ID}'. Wire environmentId from the ` +
                'Console federation host before production use.',
        );
    }
}

/** Test-only helper. Resets the once-per-process WARN guard so each test sees a fresh warning. */
export function __resetFallbackWarnedForTests(): void {
    fallbackWarned = false;
}
