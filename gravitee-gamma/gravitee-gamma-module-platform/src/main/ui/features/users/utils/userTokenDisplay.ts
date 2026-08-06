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
import { loadApimBootstrap } from '../../../shared/api/apimClient';

export const TOKEN_NAME_MAX_LENGTH = 64;
export const TOKEN_NAME_MIN_LENGTH = 2;

export function validateTokenName(name: string): string | null {
    const trimmed = name.trim();
    if (!trimmed) {
        return 'Name is required.';
    }
    if (trimmed.length < TOKEN_NAME_MIN_LENGTH) {
        return `Name has to be at least ${TOKEN_NAME_MIN_LENGTH} characters long.`;
    }
    if (trimmed.length > TOKEN_NAME_MAX_LENGTH) {
        return `Name has to be at most ${TOKEN_NAME_MAX_LENGTH} characters long.`;
    }
    return null;
}

export function isDuplicateTokenError(message: string): boolean {
    const lower = message.toLowerCase();
    return lower.includes('token.alreadyexists') || lower.includes('a token with the name');
}

export function formatTokenTimestamp(timestamp: number | undefined): string {
    if (!timestamp) {
        return 'never';
    }
    return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'medium' }).format(new Date(timestamp));
}

export async function buildTokenUsageExample(token: string, environmentId: string): Promise<string> {
    const { managementBaseURL, organizationId } = await loadApimBootstrap();
    const environmentUrl = `${managementBaseURL}/organizations/${organizationId}/environments/${environmentId}`;
    return `curl -H "Authorization: Bearer ${token}" "${environmentUrl}"`;
}
