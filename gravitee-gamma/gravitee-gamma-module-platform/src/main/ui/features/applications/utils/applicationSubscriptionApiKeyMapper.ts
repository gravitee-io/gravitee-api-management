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
import type { ApplicationSubscriptionApiKeyEntity, ApplicationSubscriptionApiKeyRow } from '../types/applicationSubscription';

export function maskApiKey(key: string): string {
    if (key.length <= 4) {
        return key;
    }
    return `${'•'.repeat(8)}${key.slice(-4)}`;
}

export function mapApiKeyEntityToRow(entity: ApplicationSubscriptionApiKeyEntity): ApplicationSubscriptionApiKeyRow | null {
    if (!entity.id || !entity.key) {
        return null;
    }

    return {
        id: entity.id,
        key: entity.key,
        maskedKey: maskApiKey(entity.key),
        isValid: !entity.revoked && !entity.expired,
        createdAt: entity.created_at,
        endDate: entity.revoked ? entity.revoked_at : entity.expire_at,
        expireAt: entity.expire_at,
    };
}

export function mapApiKeysToRows(entities: ApplicationSubscriptionApiKeyEntity[]): ApplicationSubscriptionApiKeyRow[] {
    return entities.map(mapApiKeyEntityToRow).filter((row): row is ApplicationSubscriptionApiKeyRow => row !== null);
}
