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

import type { ApiMetadata, MetadataFormat } from '../types/metadata';

export const METADATA_FORMATS: MetadataFormat[] = ['STRING', 'NUMERIC', 'BOOLEAN', 'DATE', 'MAIL', 'URL'];

export const METADATA_FORMAT_LABELS: Record<MetadataFormat, string> = {
    STRING: 'String',
    NUMERIC: 'Numeric',
    BOOLEAN: 'Boolean',
    DATE: 'Date',
    MAIL: 'Mail',
    URL: 'URL',
};

export const METADATA_PAGE_SIZE_OPTIONS = [10, 25, 50, 100];
export const DEFAULT_METADATA_PAGE_SIZE = 10;

/** Default source-filter option. Graphene Select is controlled and requires a value for every item. */
export const METADATA_SOURCE_FILTER_ALL = 'ALL';

export function getMetadataValuePlaceholder(format: MetadataFormat): string | undefined {
    if (format === 'NUMERIC') return 'e.g. 123';
    if (format === 'MAIL') return 'e.g. john@doe.com';
    if (format === 'URL') return 'e.g. https://gravitee.io';
    return undefined;
}

/** Allows FreeMarker/templated values (`${...}`) as well as literal emails. */
export const MAIL_PATTERN =
    /^((\${.+})|(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,})))$/;

export const URL_PATTERN = /^((\$\{.+\})|(https?:\/\/)?([\da-z.-]+)\.([a-z.]{2,6})\b([-a-zA-Z0-9()@:%_+.~#?&//=!]*))$/;

export function displayMetadataValue(metadata: ApiMetadata): string {
    if (metadata.defaultValue && !metadata.value) {
        return metadata.defaultValue;
    }
    return metadata.value ?? '';
}

export function isInheritedGlobal(metadata: ApiMetadata): boolean {
    return Boolean(metadata.defaultValue);
}

/** API-level override exists, so the row can be deleted or reset to the global default. */
export function canDeleteOrResetMetadata(metadata: ApiMetadata): boolean {
    return metadata.value !== undefined;
}

export function isResettableMetadata(metadata: ApiMetadata): boolean {
    return Boolean(metadata.defaultValue) && metadata.value !== undefined;
}

export function toMetadataSortBy(sorting: { id: string; desc: boolean }[] | undefined): string | undefined {
    const sort = sorting?.[0];
    if (!sort?.id) return undefined;
    return sort.desc ? `-${sort.id}` : sort.id;
}

export function isMetadataValueValid(format: MetadataFormat, value: string): boolean {
    if (format === 'BOOLEAN') return value === 'true' || value === 'false';
    if (!value.trim()) return false;
    if (format === 'NUMERIC') return !Number.isNaN(Number(value));
    if (format === 'MAIL') return MAIL_PATTERN.test(value);
    if (format === 'URL') return URL_PATTERN.test(value);
    return true;
}

export function getMetadataValueFormatError(format: MetadataFormat, value: string): string | null {
    if (!value.trim()) return null;
    if (format === 'MAIL' && !MAIL_PATTERN.test(value)) return 'Invalid email';
    if (format === 'URL' && !URL_PATTERN.test(value)) return 'Invalid URL';
    return null;
}

export function getMetadataValueInputType(format: MetadataFormat): 'number' | 'date' | 'email' | 'url' | 'text' {
    if (format === 'NUMERIC') return 'number';
    if (format === 'DATE') return 'date';
    if (format === 'MAIL') return 'email';
    if (format === 'URL') return 'url';
    return 'text';
}

export function editMetadataValue(metadata: ApiMetadata): string {
    const value = displayMetadataValue(metadata);
    if (metadata.format === 'DATE') {
        return value.slice(0, 10);
    }
    return value;
}
