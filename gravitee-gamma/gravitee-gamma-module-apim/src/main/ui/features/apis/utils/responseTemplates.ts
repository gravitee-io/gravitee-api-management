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

import type { ResponseTemplateRow, ResponseTemplatesMap } from '../types/responseTemplate';

export type ResponseTemplateIdentity = Pick<ResponseTemplateRow, 'key' | 'contentType'>;

function encodePathSegment(value: string): string {
    return encodeURIComponent(value).replaceAll('*', '%2A');
}

export function toResponseTemplatePath(key: string, contentType: string): string {
    return `${encodePathSegment(key)}/${encodePathSegment(contentType)}`;
}

export function parseResponseTemplatePath(
    templateKey: string | undefined,
    contentType: string | undefined,
): ResponseTemplateIdentity | undefined {
    if (templateKey === undefined || contentType === undefined) return undefined;
    return { key: templateKey, contentType };
}

export function sameResponseTemplate(a: ResponseTemplateIdentity, b: ResponseTemplateIdentity): boolean {
    return a.key === b.key && a.contentType === b.contentType;
}

export function toResponseTemplates(responseTemplates: ResponseTemplatesMap | undefined | null): ResponseTemplateRow[] {
    if (!responseTemplates) {
        return [];
    }

    return Object.entries(responseTemplates).flatMap(([key, byContentType]) =>
        Object.entries(byContentType).map(([contentType, responseTemplate]) => ({
            id: toResponseTemplatePath(key, contentType),
            key,
            contentType,
            statusCode: responseTemplate.statusCode,
            body: responseTemplate.body,
            headers: responseTemplate.headers,
            propagateErrorKeyToLogs: responseTemplate.propagateErrorKeyToLogs,
        })),
    );
}

export function fromResponseTemplates(responseTemplates: ResponseTemplateRow[]): ResponseTemplatesMap {
    return responseTemplates.reduce<ResponseTemplatesMap>((acc, responseTemplate) => {
        const { key, contentType, statusCode, body, headers, propagateErrorKeyToLogs } = responseTemplate;
        if (!acc[key]) {
            acc[key] = {};
        }
        acc[key][contentType] = {
            statusCode,
            body,
            headers,
            propagateErrorKeyToLogs,
        };
        return acc;
    }, {});
}

export function isDuplicateKeyAccept(
    templates: ResponseTemplateRow[],
    key: string,
    contentType: string,
    exclude?: ResponseTemplateIdentity,
): boolean {
    return templates.some(rt => !(exclude && sameResponseTemplate(rt, exclude)) && rt.key === key && rt.contentType === contentType);
}

export function matchesResponseTemplateSearch(row: ResponseTemplateRow, query: string): boolean {
    return (
        row.key.toLowerCase().includes(query) ||
        row.contentType.toLowerCase().includes(query) ||
        String(row.statusCode ?? '').includes(query)
    );
}

export function filterResponseTemplates(templates: ResponseTemplateRow[], query: string): ResponseTemplateRow[] {
    const q = query.trim().toLowerCase();
    if (!q) return templates;
    return templates.filter(rt => matchesResponseTemplateSearch(rt, q));
}

export function upsertResponseTemplate(
    current: ResponseTemplatesMap,
    toSave: ResponseTemplateRow,
    editing?: ResponseTemplateIdentity,
): ResponseTemplatesMap {
    const flat = toResponseTemplates(current);

    if (editing) {
        const index = flat.findIndex(rt => sameResponseTemplate(rt, editing));
        if (index === -1) {
            throw new Error('Response template no longer exists. It may have been deleted. Refresh and try again.');
        }
    }

    if (isDuplicateKeyAccept(flat, toSave.key, toSave.contentType, editing)) {
        throw new Error(`Response template with key '${toSave.key}' and accept header '${toSave.contentType}' already exists.`);
    }

    const next = editing
        ? flat.map(rt =>
              sameResponseTemplate(rt, editing) ? { ...toSave, id: toResponseTemplatePath(toSave.key, toSave.contentType) } : rt,
          )
        : [...flat, { ...toSave, id: toResponseTemplatePath(toSave.key, toSave.contentType) }];

    return fromResponseTemplates(next);
}

export function removeResponseTemplate(current: ResponseTemplatesMap, identity: ResponseTemplateIdentity): ResponseTemplatesMap {
    const flat = toResponseTemplates(current);
    const next = flat.filter(rt => !sameResponseTemplate(rt, identity));
    if (next.length === flat.length) {
        throw new Error('Response template no longer exists. It may have been deleted. Refresh and try again.');
    }
    return fromResponseTemplates(next);
}
