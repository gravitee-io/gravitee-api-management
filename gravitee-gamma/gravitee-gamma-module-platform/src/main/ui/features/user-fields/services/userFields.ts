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
import type { UserField, UserFieldPayload } from '../types/userField';

const CUSTOM_USER_FIELDS_PATH = '/configuration/custom-user-fields';

export async function listUserFields(): Promise<UserField[]> {
    return apimFetchJsonOrg<UserField[]>(CUSTOM_USER_FIELDS_PATH);
}

export async function createUserField(payload: UserFieldPayload): Promise<UserField> {
    return apimFetchJsonOrg<UserField>(CUSTOM_USER_FIELDS_PATH, {
        method: 'POST',
        body: JSON.stringify(payload),
    });
}

export async function updateUserField(payload: UserFieldPayload): Promise<UserField> {
    return apimFetchJsonOrg<UserField>(`${CUSTOM_USER_FIELDS_PATH}/${encodeURIComponent(payload.key)}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
    });
}

export async function deleteUserField(key: string): Promise<void> {
    await apimFetchJsonOrg<void>(`${CUSTOM_USER_FIELDS_PATH}/${encodeURIComponent(key)}`, { method: 'DELETE' });
}
