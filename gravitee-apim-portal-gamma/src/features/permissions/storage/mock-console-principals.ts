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
import type { ConsolePrincipal } from '../types/permissions.types';

/** Console users and teams are a separate directory from portal consumers. */
export const MOCK_CONSOLE_PRINCIPALS: readonly ConsolePrincipal[] = [
    { id: 'team-payments-api', type: 'TEAM', name: 'Payments API team' },
    { id: 'team-accounts-api', type: 'TEAM', name: 'Accounts API team' },
    { id: 'team-portal-managers', type: 'TEAM', name: 'Portal managers' },
    { id: 'team-ai-platform', type: 'TEAM', name: 'AI platform team' },
    { id: 'console-user-nina', type: 'USER', name: 'Nina Alvarez', email: 'nina.alvarez@gravitee.io' },
    { id: 'console-user-omar', type: 'USER', name: 'Omar Haddad', email: 'omar.haddad@gravitee.io' },
    { id: 'console-user-priya', type: 'USER', name: 'Priya Raman', email: 'priya.raman@gravitee.io' },
    { id: 'console-user-sven', type: 'USER', name: 'Sven Olsen', email: 'sven.olsen@gravitee.io' },
];

export function findConsolePrincipal(principalId: string): ConsolePrincipal | undefined {
    return MOCK_CONSOLE_PRINCIPALS.find(principal => principal.id === principalId);
}
