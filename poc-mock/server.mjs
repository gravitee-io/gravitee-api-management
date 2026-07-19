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
import { createServer } from 'node:http';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

import { POC_DEMO_API_ID, POC_ORGANIZATION_ID, POC_PORT } from './config.mjs';

const __dirname = dirname(fileURLToPath(import.meta.url));
const fixturesDir = join(__dirname, 'fixtures');

function loadFixture(name) {
    return JSON.parse(readFileSync(join(fixturesDir, name), 'utf8'));
}

const fixtures = {
    gammaBootstrap: loadFixture('gamma-bootstrap.json'),
    user: loadFixture('user.json'),
    environments: loadFixture('environments.json'),
    environmentPermissions: loadFixture('environment-permissions.json'),
    modules: loadFixture('modules.json'),
    apiSearch: loadFixture('api-search.json'),
    apiDetail: loadFixture('api-detail.json'),
    apiPermissions: loadFixture('api-permissions.json'),
    socialIdentities: loadFixture('social-identities.json'),
    userTasks: loadFixture('user-tasks.json'),
    settings: loadFixture('settings.json'),
};

const orgBase = `/management/organizations/${POC_ORGANIZATION_ID}`;
const gammaOrgBase = `/gamma/organizations/${POC_ORGANIZATION_ID}`;

/** @type {Array<{ method: string; pattern: RegExp; handler: (req: import('node:http').IncomingMessage, res: import('node:http').ServerResponse, match: RegExpMatchArray) => void }>} */
const routes = [
    {
        method: 'GET',
        pattern: /^\/gamma\/ui\/bootstrap$/,
        handler: (_req, res) => sendJson(res, fixtures.gammaBootstrap),
    },
    {
        method: 'GET',
        pattern: new RegExp(`^${orgBase}/user$`),
        handler: (_req, res) => sendJson(res, fixtures.user),
    },
    {
        method: 'POST',
        pattern: new RegExp(`^${orgBase}/user/login$`),
        handler: (_req, res) => sendEmpty(res, 200),
    },
    {
        method: 'POST',
        pattern: new RegExp(`^${orgBase}/user/logout$`),
        handler: (_req, res) => sendEmpty(res, 200),
    },
    {
        method: 'GET',
        pattern: new RegExp(`^${orgBase}/environments$`),
        handler: (_req, res) => sendJson(res, fixtures.environments),
    },
    {
        method: 'GET',
        pattern: new RegExp(`^${orgBase}/environments/[^/]+/permissions$`),
        handler: (_req, res) => sendJson(res, fixtures.environmentPermissions),
    },
    {
        method: 'GET',
        pattern: new RegExp(`^${gammaOrgBase}/modules$`),
        handler: (_req, res) => sendJson(res, fixtures.modules),
    },
    {
        method: 'GET',
        pattern: new RegExp(`^${orgBase}/social-identities$`),
        handler: (_req, res) => sendJson(res, fixtures.socialIdentities),
    },
    {
        method: 'GET',
        pattern: new RegExp(`^${orgBase}/user/tasks$`),
        handler: (_req, res) => sendJson(res, fixtures.userTasks),
    },
    {
        method: 'GET',
        pattern: new RegExp(`^${orgBase}/settings$`),
        handler: (_req, res) => sendJson(res, fixtures.settings),
    },
    {
        method: 'POST',
        pattern: /^\/management\/v2\/environments\/[^/]+\/apis\/_search/,
        handler: (_req, res) => sendJson(res, fixtures.apiSearch),
    },
    {
        method: 'GET',
        pattern: new RegExp(`^/management/v2/environments/[^/]+/apis/${POC_DEMO_API_ID}$`),
        handler: (_req, res) => sendJson(res, fixtures.apiDetail),
    },
    {
        method: 'GET',
        pattern: new RegExp(`^${orgBase}/environments/[^/]+/apis/${POC_DEMO_API_ID}/members/permissions$`),
        handler: (_req, res) => sendJson(res, fixtures.apiPermissions),
    },
];

function sendJson(res, body, status = 200) {
    const payload = JSON.stringify(body);
    res.writeHead(status, {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(payload),
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Credentials': 'true',
    });
    res.end(payload);
}

function sendEmpty(res, status = 204) {
    res.writeHead(status, {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Credentials': 'true',
    });
    res.end();
}

function handleRequest(req, res) {
    if (req.method === 'OPTIONS') {
        res.writeHead(204, {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
            'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Requested-With, X-Xsrf-Token',
            'Access-Control-Allow-Credentials': 'true',
        });
        res.end();
        return;
    }

    const url = new URL(req.url ?? '/', `http://${req.headers.host ?? 'localhost'}`);
    const pathname = url.pathname;

    for (const route of routes) {
        if (route.method !== req.method) continue;
        const match = pathname.match(route.pattern);
        if (match) {
            route.handler(req, res, match);
            return;
        }
    }

    console.warn(`[poc-mock] Unhandled ${req.method} ${pathname}`);
    sendJson(res, { message: `No POC mock for ${req.method} ${pathname}` }, 404);
}

const server = createServer(handleRequest);

server.on('error', err => {
    if (err.code === 'EADDRINUSE') {
        console.error(
            `[poc-mock] Port ${POC_PORT} is already in use. Stop the other process or set POC_MOCK_PORT to a free port.`,
        );
        process.exit(1);
    }
    throw err;
});

server.listen(POC_PORT, () => {
    console.log(`[poc-mock] Offline API mock listening on http://localhost:${POC_PORT}`);
    console.log(`[poc-mock] Organization: ${POC_ORGANIZATION_ID}, demo API: ${POC_DEMO_API_ID}`);
});
