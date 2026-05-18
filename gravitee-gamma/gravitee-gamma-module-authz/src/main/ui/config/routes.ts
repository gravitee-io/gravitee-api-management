import type { NavigateFunction } from 'react-router-dom';

export const ROUTE_KEYS = [
    // Policy Management
    'mcps',
    'agents',
    'llms',
    'apis',
    'events',
    'custom-policies',

    // Policy structure (mockup ordering: entities → actions → schema)
    'entities',
    'actions',
    'schema',
] as const;
export type RouteKey = (typeof ROUTE_KEYS)[number];

const ROUTE_KEY_SET = new Set<string>(ROUTE_KEYS);
const DEFAULT_ROUTE_KEY: RouteKey = 'mcps';

export const ROUTES: Record<RouteKey, { readonly path: string; readonly label: string }> = {
    mcps: { path: 'mcps', label: 'MCPs' },
    agents: { path: 'agents', label: 'Agents' },
    llms: { path: 'llms', label: 'AI Models' },
    apis: { path: 'apis', label: 'APIs' },
    events: { path: 'events', label: 'Events' },
    'custom-policies': { path: 'custom-policies', label: 'Custom Policies' },
    entities: { path: 'entities', label: 'Entities' },
    actions: { path: 'actions', label: 'Actions' },
    schema: { path: 'schema', label: 'Schema' },
};

export function isRouteKey(segment: string): segment is RouteKey {
    return ROUTE_KEY_SET.has(segment);
}

/**
 * Resolves the active sidebar key and optional host module prefix from the URL.
 * When embedded under the Gamma console host the path is
 * `/environments/{envHrid}/{moduleId}/{routeKey}` — strip the env wrapper so
 * the sidebar highlights the real sub-route instead of always defaulting.
 */
export function resolveModulePath(pathname: string): { modulePrefix: string; activeNavKey: RouteKey } {
    let segments = pathname.split('/').filter(Boolean);
    if (segments[0] === 'environments' && segments.length >= 2) {
        segments = segments.slice(2);
    }
    if (segments.length === 0) {
        return { modulePrefix: '', activeNavKey: DEFAULT_ROUTE_KEY };
    }
    if (isRouteKey(segments[0])) {
        return { modulePrefix: '', activeNavKey: segments[0] };
    }
    const moduleId = segments[0];
    const sub = segments[1] ?? DEFAULT_ROUTE_KEY;
    const activeNavKey = isRouteKey(sub) ? sub : DEFAULT_ROUTE_KEY;
    return { modulePrefix: moduleId, activeNavKey };
}

/**
 * Navigates to the URL for a sidebar item key.
 * `currentPath` is the live `useLocation().pathname` so we can preserve the
 * host's `/environments/{envHrid}` wrapper when the module is embedded.
 */
export function navigateToNavKey(navigate: NavigateFunction, modulePrefix: string, key: string, currentPath: string = ''): void {
    const segments = currentPath.split('/').filter(Boolean);
    if (segments[0] === 'environments' && segments.length >= 3) {
        navigate(`/${segments[0]}/${segments[1]}/${segments[2]}/${key}`);
        return;
    }
    if (modulePrefix) {
        navigate(`/${modulePrefix}/${key}`);
        return;
    }
    navigate(`/${key}`);
}
