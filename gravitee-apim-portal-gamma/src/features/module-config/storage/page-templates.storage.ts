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
import { PAGE_TEMPLATES_STORE_NAME, runTransaction } from '../../portals/storage/db';
import { DEFAULT_PAGE_TEMPLATES, toSystemPageTemplate } from '../defaults/default-page-templates';
import {
    normalizePageTemplate,
    type PageTemplate,
    type PageTemplateInput,
    type PageTemplatePatch,
} from '../types';

function createTemplateId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID();
    }
    return `tpl-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

export async function listPageTemplates(): Promise<PageTemplate[]> {
    const templates = await runTransaction<PageTemplate[]>(PAGE_TEMPLATES_STORE_NAME, 'readonly', store =>
        store.getAll(),
    );

    return templates
        .map(normalizePageTemplate)
        .sort((a, b) => {
            if (a.system !== b.system) {
                return a.system ? -1 : 1;
            }
            return a.name.localeCompare(b.name);
        });
}

export async function getPageTemplate(id: string): Promise<PageTemplate | undefined> {
    const template = await runTransaction<PageTemplate | undefined>(PAGE_TEMPLATES_STORE_NAME, 'readonly', store =>
        store.get(id),
    );
    return template ? normalizePageTemplate(template) : undefined;
}

export async function savePageTemplate(template: PageTemplate): Promise<void> {
    await runTransaction(PAGE_TEMPLATES_STORE_NAME, 'readwrite', store =>
        store.put(normalizePageTemplate(template)),
    );
}

/** Ensures system default templates exist; safe to call on every page load. */
export async function ensureDefaultPageTemplates(): Promise<PageTemplate[]> {
    const existing = await listPageTemplates();
    const existingIds = new Set(existing.map(template => template.id));
    const now = Date.now();

    const missing = DEFAULT_PAGE_TEMPLATES.filter(definition => !existingIds.has(definition.id)).map(definition =>
        toSystemPageTemplate(definition, now),
    );

    await Promise.all(missing.map(template => savePageTemplate(template)));

    if (missing.length === 0) {
        return existing;
    }

    return listPageTemplates();
}

export async function createPageTemplate(input: PageTemplateInput): Promise<PageTemplate> {
    const now = Date.now();
    const template = normalizePageTemplate({
        id: createTemplateId(),
        name: input.name.trim(),
        description: (input.description ?? '').trim(),
        contentType: input.contentType,
        kind: input.kind ?? 'custom',
        system: false,
        bodyStub: (input.bodyStub ?? '').trim() || '<!-- Custom template stub -->',
        createdAt: now,
        updatedAt: now,
    });
    await savePageTemplate(template);
    return template;
}

export async function updatePageTemplate(
    templateId: string,
    patch: PageTemplatePatch,
): Promise<PageTemplate | undefined> {
    const existing = await getPageTemplate(templateId);
    if (!existing) {
        return undefined;
    }

    const updated = normalizePageTemplate({
        ...existing,
        ...patch,
        name: patch.name !== undefined ? patch.name.trim() : existing.name,
        description: patch.description !== undefined ? patch.description.trim() : existing.description,
        bodyStub: patch.bodyStub !== undefined ? patch.bodyStub : existing.bodyStub,
        updatedAt: Date.now(),
    });
    await savePageTemplate(updated);
    return updated;
}

export async function deletePageTemplate(id: string): Promise<void> {
    const existing = await getPageTemplate(id);
    if (existing?.system) {
        throw new Error('System page templates cannot be deleted');
    }
    await runTransaction(PAGE_TEMPLATES_STORE_NAME, 'readwrite', store => store.delete(id));
}
