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
import { useBootstrapStore } from '../../../shared/config/bootstrap.store';

export interface FinalizeResetPasswordPayload {
    token: string;
    password: string;
    firstname: string;
    lastname: string;
}

const RESET_PASSWORD_FALLBACK_ERROR = 'An error occurred while resetting your password.';

function getCsrfToken(): string | null {
    return localStorage.getItem('XSRF-TOKEN');
}

function setCsrfToken(value: string) {
    localStorage.setItem('XSRF-TOKEN', value);
}

function resolveManagementErrorMessage(text: string, fallback: string): string {
    if (!text) {
        return fallback;
    }

    try {
        const parsed = JSON.parse(text) as { message?: string };
        return parsed.message?.trim() || fallback;
    } catch {
        return text.trim() || fallback;
    }
}

export async function finalizeResetPassword(userId: string, payload: FinalizeResetPasswordPayload): Promise<void> {
    const config = useBootstrapStore.getState().config;
    if (!config) {
        throw new Error('Bootstrap not initialized');
    }

    const path = `/users/${encodeURIComponent(userId)}/changePassword`;
    const url = `${config.managementBaseURL}/organizations/${config.organizationId}${path}`;
    const headers = new Headers({
        'Content-Type': 'application/json',
        'X-Requested-With': 'XMLHttpRequest',
    });
    const csrf = getCsrfToken();
    if (csrf) {
        headers.set('X-Xsrf-Token', csrf);
    }

    const res = await fetch(url, {
        method: 'POST',
        headers,
        credentials: 'include',
        body: JSON.stringify({
            token: payload.token,
            password: payload.password,
            firstname: payload.firstname,
            lastname: payload.lastname,
        }),
    });

    const newCsrf = res.headers.get('X-Xsrf-Token');
    if (newCsrf) {
        setCsrfToken(newCsrf);
    }

    if (!res.ok) {
        const text = await res.text().catch(() => '');
        throw new Error(resolveManagementErrorMessage(text, RESET_PASSWORD_FALLBACK_ERROR));
    }
}
