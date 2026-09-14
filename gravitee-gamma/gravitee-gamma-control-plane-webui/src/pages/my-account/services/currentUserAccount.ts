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
import type { CurrentUser } from '../../../features/auth/auth.types';
import { normalizeCurrentUser } from '../../../features/auth/normalizeCurrentUser';
import { managementApi } from '../../../shared/api/api-client';
import { authenticationFromConsole } from '../myAccount.mapping';
import type { ConsoleAuthenticationSettings, UpdateCurrentUserPayload } from '../myAccount.types';

export async function fetchCurrentUser(): Promise<CurrentUser> {
    return normalizeCurrentUser(await managementApi.get<unknown>('/user'));
}

export async function updateCurrentUser(payload: UpdateCurrentUserPayload): Promise<CurrentUser> {
    return normalizeCurrentUser(await managementApi.put<unknown>('/user', payload));
}

export async function deleteCurrentUser(): Promise<void> {
    await managementApi.delete<void>('/user');
}

export async function fetchConsoleAuthentication(): Promise<ConsoleAuthenticationSettings | undefined> {
    const consoleJson = await managementApi.get<unknown>('/console');
    return authenticationFromConsole(consoleJson);
}
