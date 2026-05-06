/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { ALL_ENV_PERMISSION_KEYS, ALL_ORG_PERMISSION_KEYS } from './navigation-items';

const CRUD = ['c', 'r', 'u', 'd'] as const;

function orgPerms(keys: readonly string[]): string[] {
    return keys.flatMap(k => CRUD.map(c => `organization-${k}-${c}`));
}

function envPerms(keys: readonly string[]): string[] {
    return keys.flatMap(k => CRUD.map(c => `environment-${k}-${c}`));
}

export interface MockEnvPermissions {
    readonly permissions: readonly string[];
}

export interface MockPersona {
    readonly id: string;
    readonly name: string;
    readonly description: string;
    readonly orgPermissions: readonly string[];
    readonly envPermissions: Readonly<Record<string, MockEnvPermissions>>;
}

const ALICE: MockPersona = {
    id: 'alice',
    name: 'Alice',
    description: 'Super Admin',
    orgPermissions: orgPerms(ALL_ORG_PERMISSION_KEYS),
    envPermissions: {
        dev: { permissions: envPerms(ALL_ENV_PERMISSION_KEYS) },
        staging: { permissions: envPerms(ALL_ENV_PERMISSION_KEYS) },
        prod: { permissions: envPerms(ALL_ENV_PERMISSION_KEYS) },
    },
};

const BOB: MockPersona = {
    id: 'bob',
    name: 'Bob',
    description: 'Dev Manager',
    orgPermissions: [],
    envPermissions: {
        dev: { permissions: envPerms(ALL_ENV_PERMISSION_KEYS) },
    },
};

const charlieBaseEnvKeys = ALL_ENV_PERMISSION_KEYS;

const charlieStagingPerms = envPerms(charlieBaseEnvKeys.filter(k => k !== 'api_behavior')).concat(
    ['environment-api_behavior-r'],
);

const charlieProdPerms = envPerms(charlieBaseEnvKeys.filter(k => k !== 'api_logging'));

const CHARLIE: MockPersona = {
    id: 'charlie',
    name: 'Charlie',
    description: 'QA Tester',
    orgPermissions: [],
    envPermissions: {
        staging: { permissions: charlieStagingPerms },
        prod: { permissions: charlieProdPerms },
    },
};

export const PERSONAS: readonly MockPersona[] = [ALICE, BOB, CHARLIE] as const;

export const DEFAULT_PERSONA = ALICE;
