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

import {
    createApiFromRequest,
    createPlan,
    getApi,
    isPathTaken,
    listApis,
    normalizeContextPath,
    publishPlan,
    startApi,
    toListItem,
} from './pocApiStore';

const API_PERMISSIONS: Record<string, string[]> = {
    DEFINITION: ['R', 'U'],
    DOCUMENTATION: ['R', 'C', 'U', 'D'],
    PLAN: ['R', 'C', 'U', 'D'],
    SUBSCRIPTION: ['R', 'C', 'U', 'D'],
    MEMBER: ['R', 'C', 'U', 'D'],
};

function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), {
        status,
        headers: { 'Content-Type': 'application/json' },
    });
}

function emptyResponse(status = 204): Response {
    return new Response(null, { status });
}

async function readJsonBody(request: Request): Promise<Record<string, unknown>> {
    try {
        const text = await request.text();
        if (!text) {
            return {};
        }
        return JSON.parse(text) as Record<string, unknown>;
    } catch {
        return {};
    }
}

function match(
    pathname: string,
    pattern: RegExp,
): RegExpMatchArray | null {
    return pathname.match(pattern);
}

/**
 * Handles POC management API routes that need mutable localStorage state.
 * Returns null to fall through to the network (static mock / server.mjs).
 */
export async function handlePocApiRequest(request: Request, pathname: string, searchParams: URLSearchParams): Promise<Response | null> {
    const method = request.method.toUpperCase();

    // POST /management/v2/environments/:env/apis/_verify/paths
    if (method === 'POST' && match(pathname, /^\/management\/v2\/environments\/[^/]+\/apis\/_verify\/paths$/)) {
        const body = await readJsonBody(request);
        const paths = Array.isArray(body.paths) ? (body.paths as Array<{ path?: string }>) : [];
        const apiId = typeof body.apiId === 'string' ? body.apiId : undefined;
        for (const entry of paths) {
            const path = entry?.path;
            if (path && isPathTaken(path, apiId)) {
                return jsonResponse({
                    ok: false,
                    reason: `Path [${normalizeContextPath(path)}] already exists`,
                });
            }
        }
        return jsonResponse({ ok: true });
    }

    // POST /management/v2/environments/:env/apis/_verify/hosts
    if (method === 'POST' && match(pathname, /^\/management\/v2\/environments\/[^/]+\/apis\/_verify\/hosts$/)) {
        return jsonResponse({ ok: true });
    }

    // POST /management/v2/environments/:env/apis/_search
    if (method === 'POST' && match(pathname, /^\/management\/v2\/environments\/[^/]+\/apis\/_search$/)) {
        const page = Math.max(1, Number(searchParams.get('page') ?? '1') || 1);
        const perPage = Math.max(1, Number(searchParams.get('perPage') ?? '10') || 10);
        const all = listApis().map(toListItem);
        const totalCount = all.length;
        const pageCount = Math.max(1, Math.ceil(totalCount / perPage));
        const start = (page - 1) * perPage;
        const data = all.slice(start, start + perPage);
        return jsonResponse({
            data,
            pagination: {
                page,
                perPage,
                pageCount,
                totalCount,
            },
        });
    }

    // POST /management/v2/environments/:env/apis (create — not _search / _verify)
    if (method === 'POST' && match(pathname, /^\/management\/v2\/environments\/[^/]+\/apis$/)) {
        const body = await readJsonBody(request);
        const created = createApiFromRequest(body);
        return jsonResponse(created, 201);
    }

    // POST /management/v2/environments/:env/apis/:apiId/plans/:planId/_publish
    {
        const publishMatch = match(pathname, /^\/management\/v2\/environments\/[^/]+\/apis\/([^/]+)\/plans\/([^/]+)\/_publish$/);
        if (method === 'POST' && publishMatch) {
            const published = publishPlan(publishMatch[1], publishMatch[2]);
            if (!published) {
                return jsonResponse({ message: 'Plan not found' }, 404);
            }
            return emptyResponse(204);
        }
    }

    // POST /management/v2/environments/:env/apis/:apiId/plans
    {
        const planMatch = match(pathname, /^\/management\/v2\/environments\/[^/]+\/apis\/([^/]+)\/plans$/);
        if (method === 'POST' && planMatch) {
            const body = await readJsonBody(request);
            const plan = createPlan(planMatch[1], body);
            if (!plan) {
                return jsonResponse({ message: 'API not found' }, 404);
            }
            return jsonResponse(plan, 201);
        }
    }

    // POST /management/v2/environments/:env/apis/:apiId/_start
    {
        const startMatch = match(pathname, /^\/management\/v2\/environments\/[^/]+\/apis\/([^/]+)\/_start$/);
        if (method === 'POST' && startMatch) {
            const started = startApi(startMatch[1]);
            if (!started) {
                return jsonResponse({ message: 'API not found' }, 404);
            }
            return emptyResponse(204);
        }
    }

    // GET /management/v2/environments/:env/apis/:apiId/members
    {
        const membersMatch = match(pathname, /^\/management\/v2\/environments\/[^/]+\/apis\/([^/]+)\/members$/);
        if (method === 'GET' && membersMatch) {
            if (!getApi(membersMatch[1])) {
                return jsonResponse({ message: 'API not found' }, 404);
            }
            return jsonResponse({
                data: [],
                pagination: { page: 1, perPage: 10, totalCount: 0, pageCount: 0, pageItemsCount: 0 },
            });
        }
    }

    // GET /management/v2/environments/:env/apis/:apiId/exposedEntrypoints
    {
        const entrypointsMatch = match(pathname, /^\/management\/v2\/environments\/[^/]+\/apis\/([^/]+)\/exposedEntrypoints$/);
        if (method === 'GET' && entrypointsMatch) {
            if (!getApi(entrypointsMatch[1])) {
                return jsonResponse({ message: 'API not found' }, 404);
            }
            return jsonResponse([]);
        }
    }

    // GET /management/v2/environments/:env/apis/:apiId/analytics
    {
        const analyticsMatch = match(pathname, /^\/management\/v2\/environments\/[^/]+\/apis\/([^/]+)\/analytics$/);
        if (method === 'GET' && analyticsMatch) {
            if (!getApi(analyticsMatch[1])) {
                return jsonResponse({ message: 'API not found' }, 404);
            }
            return jsonResponse({
                analyticsType: 'STATS',
                avg: 0,
                min: 0,
                max: 0,
                sum: 0,
                count: 0,
                rps: 0,
                rpm: 0,
                rph: 0,
            });
        }
    }

    // GET /management/v2/environments/:env/apis/:apiId (detail)
    {
        const detailMatch = match(pathname, /^\/management\/v2\/environments\/[^/]+\/apis\/([^/]+)$/);
        if (method === 'GET' && detailMatch) {
            const api = getApi(detailMatch[1]);
            if (!api) {
                return jsonResponse({ message: 'API not found' }, 404);
            }
            return jsonResponse(api);
        }
    }

    // GET /management/organizations/:org/environments/:env/apis/:apiId/members/permissions
    {
        const permMatch = match(
            pathname,
            /^\/management\/organizations\/[^/]+\/environments\/[^/]+\/apis\/([^/]+)\/members\/permissions$/,
        );
        if (method === 'GET' && permMatch) {
            if (!getApi(permMatch[1])) {
                return jsonResponse({ message: 'API not found' }, 404);
            }
            return jsonResponse(API_PERMISSIONS);
        }
    }

    // GET /management/organizations/:org/environments/:env/apis/:apiId/alerts
    {
        const alertsMatch = match(pathname, /^\/management\/organizations\/[^/]+\/environments\/[^/]+\/apis\/([^/]+)\/alerts$/);
        if (method === 'GET' && alertsMatch) {
            if (!getApi(alertsMatch[1])) {
                return jsonResponse({ message: 'API not found' }, 404);
            }
            return jsonResponse([]);
        }
    }

    return null;
}
