/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { useCallback, useMemo } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

/**
 * Routing domain — generic helpers for federated modules mounted under the
 * gamma host URL scheme `/environments/:envHrid/:moduleId/<routeKey>`.
 */

// ── Types ────────────────────────────────────────────────────────────────────

export interface ModuleRouteDefinition {
    readonly path: string;
    readonly label: string;
}

export interface ModuleRouteConfig<K extends string = string> {
    readonly routeKeys: readonly K[];
    readonly routes: Record<K, ModuleRouteDefinition>;
    readonly defaultRouteKey: K;
}

// ── Pure helpers (no React) ──────────────────────────────────────────────────

/** Type-guard: is `segment` one of the declared route keys? */
export function isRouteKey<K extends string>(segment: string, routeKeys: readonly K[]): segment is K {
    return (routeKeys as readonly string[]).includes(segment);
}

/**
 * Resolves the active sidebar key and optional host module prefix from the URL.
 *
 * Handles three mounting modes:
 *   1. **Federated** — `/environments/:envHrid/:moduleId/<routeKey>`
 *   2. **Standalone with prefix** — `/:moduleId/<routeKey>`
 *   3. **Legacy standalone** — `/<routeKey>` (first segment is itself a key)
 */
export function resolveModulePath<K extends string>(
    pathname: string,
    config: ModuleRouteConfig<K>,
): { modulePrefix: string; activeNavKey: K } {
    const segments = pathname.split('/').filter(Boolean);
    const { routeKeys, defaultRouteKey } = config;

    if (segments.length === 0) {
        return { modulePrefix: '', activeNavKey: defaultRouteKey };
    }

    if (segments[0] === 'environments' && segments.length >= 3) {
        const moduleId = segments[2]!;
        const sub = segments[3] ?? defaultRouteKey;
        return { modulePrefix: moduleId, activeNavKey: isRouteKey(sub, routeKeys) ? sub : defaultRouteKey };
    }

    if (isRouteKey(segments[0], routeKeys)) {
        return { modulePrefix: '', activeNavKey: segments[0] };
    }

    const moduleId = segments[0]!;
    const sub = segments[1] ?? defaultRouteKey;
    return { modulePrefix: moduleId, activeNavKey: isRouteKey(sub, routeKeys) ? sub : defaultRouteKey };
}

/**
 * Pure string builder: returns the target pathname for navigating to a route
 * key inside a module.
 *
 * When federated, the environment prefix is extracted from `currentPathname`.
 */
export function buildModuleNavPath(modulePrefix: string, key: string, currentPathname?: string): string {
    if (modulePrefix) {
        const pathname = currentPathname ?? (typeof window !== 'undefined' ? window.location.pathname : '');
        const envEnd = pathname.indexOf('/', '/environments/'.length);
        if (pathname.startsWith('/environments/') && envEnd > 0) {
            return `${pathname.slice(0, envEnd)}/${modulePrefix}/${key}`;
        }
        return `/${modulePrefix}/${key}`;
    }
    return `/${key}`;
}

/**
 * Returns the "root" pathname for breadcrumbs (e.g. the module's landing page
 * under the current environment).
 */
export function buildModuleRootPath(pathname: string, modulePrefix: string, defaultRouteKey: string): string {
    if (!modulePrefix) {
        return '/';
    }
    const envEnd = pathname.indexOf('/', '/environments/'.length);
    if (pathname.startsWith('/environments/') && envEnd > 0) {
        return `${pathname.slice(0, envEnd)}/${modulePrefix}/${defaultRouteKey}`;
    }
    return `/${modulePrefix}/${defaultRouteKey}`;
}

// ── React hook (react + react-router-dom only) ──────────────────────────────

export interface ModuleRoutingResult<K extends string = string> {
    modulePrefix: string;
    activeNavKey: K;
    navigateToKey: (key: string) => void;
    rootPath: string;
}

/**
 * Convenience hook that composes the pure routing helpers for a federated module.
 *
 * Returns raw data — no JSX, no graphene types. Modules use the returned values
 * to wire graphene layout components in their own `ModuleLayout`.
 */
export function useModuleRouting<K extends string>(config: ModuleRouteConfig<K>): ModuleRoutingResult<K> {
    const location = useLocation();
    const navigate = useNavigate();

    const { modulePrefix, activeNavKey } = useMemo(() => resolveModulePath(location.pathname, config), [location.pathname, config]);

    const rootPath = useMemo(
        () => buildModuleRootPath(location.pathname, modulePrefix, config.defaultRouteKey),
        [location.pathname, modulePrefix, config.defaultRouteKey],
    );

    const navigateToKey = useCallback(
        (key: string) => {
            navigate(buildModuleNavPath(modulePrefix, key, location.pathname));
        },
        [navigate, modulePrefix, location.pathname],
    );

    return { modulePrefix, activeNavKey, navigateToKey, rootPath };
}
