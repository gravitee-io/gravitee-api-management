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
import { useCallback, useEffect, useState } from 'react';

import {
    createPageTemplate,
    deletePageTemplate,
    ensureDefaultPageTemplates,
    updatePageTemplate,
} from '../storage/page-templates.storage';
import type { PageTemplate, PageTemplateInput, PageTemplatePatch } from '../types';

export function usePageTemplates() {
    const [templates, setTemplates] = useState<PageTemplate[]>([]);
    const [loading, setLoading] = useState(true);

    const refresh = useCallback(async () => {
        setLoading(true);
        try {
            setTemplates(await ensureDefaultPageTemplates());
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        void refresh();
    }, [refresh]);

    const addTemplate = useCallback(
        async (input: PageTemplateInput) => {
            const created = await createPageTemplate(input);
            await refresh();
            return created;
        },
        [refresh],
    );

    const updateTemplate = useCallback(
        async (id: string, patch: PageTemplatePatch) => {
            const updated = await updatePageTemplate(id, patch);
            await refresh();
            return updated;
        },
        [refresh],
    );

    const removeTemplate = useCallback(
        async (id: string) => {
            await deletePageTemplate(id);
            await refresh();
        },
        [refresh],
    );

    return {
        templates,
        loading,
        refresh,
        addTemplate,
        updateTemplate,
        removeTemplate,
    };
}
