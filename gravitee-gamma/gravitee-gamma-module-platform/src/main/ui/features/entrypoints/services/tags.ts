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

import { apimFetchJsonOrg } from '../../../shared/api/apimClient';
import type { NewOrgTagPayload, OrgTag, UpdateOrgTagPayload } from '../types/entrypoint';

export async function listOrgTags(): Promise<OrgTag[]> {
    return apimFetchJsonOrg<OrgTag[]>('/configuration/tags');
}

export async function createOrgTag(payload: NewOrgTagPayload): Promise<OrgTag> {
    return apimFetchJsonOrg<OrgTag>('/configuration/tags', {
        method: 'POST',
        body: JSON.stringify(payload),
    });
}

export async function updateOrgTag(tagKey: string, payload: UpdateOrgTagPayload): Promise<OrgTag> {
    return apimFetchJsonOrg<OrgTag>(`/configuration/tags/${encodeURIComponent(tagKey)}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
    });
}

export async function deleteOrgTag(tagKey: string): Promise<void> {
    return apimFetchJsonOrg<void>(`/configuration/tags/${encodeURIComponent(tagKey)}`, { method: 'DELETE' });
}
