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
import type { PortalUser } from '../types/portal-tenant.types';

export const DUMMY_PORTAL_USERS: PortalUser[] = [
    { id: 'user-alice', displayName: 'Alice Smith', email: 'alice@acme.com' },
    { id: 'user-bob', displayName: 'Bob Jones', email: 'bob@acme.com' },
    { id: 'user-carol', displayName: 'Carol White', email: 'carol@example.com' },
    { id: 'user-dan', displayName: 'Dan Green', email: 'dan@example.com' },
    { id: 'user-eve', displayName: 'Eve Black', email: 'eve@example.com' },
    { id: 'user-frank', displayName: 'Frank Miller', email: 'frank@example.com' },
    { id: 'user-grace', displayName: 'Grace Lee', email: 'grace@example.com' },
    { id: 'user-henry', displayName: 'Henry Wilson', email: 'henry@example.com' },
];
