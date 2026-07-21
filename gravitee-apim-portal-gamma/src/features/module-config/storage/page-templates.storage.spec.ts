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
import { clearPortalsDatabase } from '../../portals/storage/portals.storage.test-utils';
import { DEFAULT_PAGE_TEMPLATES } from '../defaults/default-page-templates';
import {
    createPageTemplate,
    deletePageTemplate,
    ensureDefaultPageTemplates,
    listPageTemplates,
    updatePageTemplate,
} from './page-templates.storage';

describe('page-templates.storage', () => {
    beforeEach(async () => {
        await clearPortalsDatabase();
    });

    afterEach(async () => {
        await clearPortalsDatabase();
    });

    it('should seed default system templates once', async () => {
        const first = await ensureDefaultPageTemplates();
        expect(first).toHaveLength(DEFAULT_PAGE_TEMPLATES.length);
        expect(first.every(template => template.system)).toBe(true);

        const second = await ensureDefaultPageTemplates();
        expect(second).toHaveLength(DEFAULT_PAGE_TEMPLATES.length);
    });

    it('should create custom templates and keep system ones first', async () => {
        await ensureDefaultPageTemplates();
        const custom = await createPageTemplate({
            name: 'Getting started',
            description: 'Onboarding page',
            contentType: 'BLOCK',
            bodyStub: '# Getting started',
        });

        expect(custom.system).toBe(false);
        expect(custom.kind).toBe('custom');

        const listed = await listPageTemplates();
        expect(listed.some(template => template.id === custom.id)).toBe(true);
        expect(listed[0]?.system).toBe(true);
    });

    it('should update custom templates and refuse deleting system templates', async () => {
        await ensureDefaultPageTemplates();
        const custom = await createPageTemplate({
            name: 'Custom',
            contentType: 'HTML',
        });

        await updatePageTemplate(custom.id, {
            name: 'Custom updated',
            description: 'Updated description',
        });

        const updated = (await listPageTemplates()).find(template => template.id === custom.id);
        expect(updated).toMatchObject({
            name: 'Custom updated',
            description: 'Updated description',
        });

        const systemId = DEFAULT_PAGE_TEMPLATES[0]?.id;
        expect(systemId).toBeTruthy();
        await expect(deletePageTemplate(systemId!)).rejects.toThrow('System page templates cannot be deleted');

        await deletePageTemplate(custom.id);
        expect((await listPageTemplates()).some(template => template.id === custom.id)).toBe(false);
    });
});
