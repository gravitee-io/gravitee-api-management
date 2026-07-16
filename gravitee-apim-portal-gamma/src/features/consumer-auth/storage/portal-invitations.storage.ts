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
import { PORTAL_INVITATIONS_STORE_NAME, runTransaction } from '../../portals/storage/db';
import type { PortalInvitation } from '../types/consumer-auth.types';

export const STORE_NAME = PORTAL_INVITATIONS_STORE_NAME;

export async function getInvitationByToken(token: string): Promise<PortalInvitation | undefined> {
    return runTransaction<PortalInvitation | undefined>(PORTAL_INVITATIONS_STORE_NAME, 'readonly', store => {
        const index = store.index('token');
        return index.get(token);
    });
}

export async function getInvitationsByTenantId(tenantId: string): Promise<PortalInvitation[]> {
    return runTransaction<PortalInvitation[]>(PORTAL_INVITATIONS_STORE_NAME, 'readonly', store => {
        const index = store.index('tenantId');
        return index.getAll(tenantId);
    });
}

export async function savePortalInvitation(invitation: PortalInvitation): Promise<void> {
    await runTransaction(PORTAL_INVITATIONS_STORE_NAME, 'readwrite', store => store.put(invitation));
}
