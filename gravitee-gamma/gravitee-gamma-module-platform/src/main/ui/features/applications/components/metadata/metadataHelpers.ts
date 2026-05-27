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
import type { ApplicationMetadata, ApplicationMetadataFormat } from '../../types/applicationNotification';

export function deriveMetadataKey(name: string): string {
    return name
        .trim()
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-|-$/g, '');
}

export function metadataFormat(metadata: ApplicationMetadata | null): ApplicationMetadataFormat {
    return metadata?.format ?? 'STRING';
}

export function metadataValue(metadata: ApplicationMetadata | null): string {
    if (!metadata) {
        return '';
    }
    const value = metadata.value ?? metadata.defaultValue ?? '';
    if (metadataFormat(metadata) === 'DATE') {
        return value.slice(0, 10);
    }
    return String(value);
}
