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
// FIXME: Replace module-level mutable state with proper DI/context when Gamma CP has its own backend
let managementBase = '/management/organizations/DEFAULT'; // fallback

export function setManagementBase(base: string) {
    managementBase = base;
}

function getCsrfToken(): string | null {
    return localStorage.getItem('XSRF-TOKEN');
}

function setCsrfToken(value: string) {
    localStorage.setItem('XSRF-TOKEN', value);
}

async function apiFetch(path: string, init?: RequestInit): Promise<Response> {
    const headers = new Headers(init?.headers);
    headers.set('X-Requested-With', 'XMLHttpRequest');
    const csrf = getCsrfToken();
    if (csrf) headers.set('X-Xsrf-Token', csrf);

    const res = await fetch(`${managementBase}${path}`, {
        ...init,
        headers,
        credentials: 'include',
    });

    const newCsrf = res.headers.get('X-Xsrf-Token');
    if (newCsrf) setCsrfToken(newCsrf);

    return res;
}

export interface User {
    displayName: string;
    email: string;
    firstname: string;
    lastname: string;
}

export async function login(username: string, password: string): Promise<void> {
    const res = await apiFetch('/user/login', {
        method: 'POST',
        headers: { Authorization: `Basic ${btoa(`${username}:${password}`)}` },
    });
    if (!res.ok) throw new Error('Login failed');
}

export async function logout(): Promise<void> {
    await apiFetch('/user/logout', { method: 'POST' }).catch(() => {});
    localStorage.removeItem('XSRF-TOKEN');
}

export async function fetchCurrentUser(): Promise<User> {
    const res = await apiFetch('/user');
    if (!res.ok) throw new Error('Not authenticated');
    return res.json();
}
