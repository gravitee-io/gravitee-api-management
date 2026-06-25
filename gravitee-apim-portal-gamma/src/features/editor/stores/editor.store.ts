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
import { create } from 'zustand';
import { devtools } from 'zustand/middleware';

import type { DeveloperPortal, PortalLayout } from '../../portals/types';
import { notify } from '../../../shared/notify/notify';
import type { PageWidth } from '../constants/page-width';

export type EditorMode = 'edit' | 'preview';

const PAGE_WIDTH_STORAGE_KEY = 'gravitee-portal-gamma-page-width';

function readStoredPageWidth(): PageWidth {
    const stored = localStorage.getItem(PAGE_WIDTH_STORAGE_KEY);
    if (stored === 'narrow' || stored === 'medium' || stored === 'wide') {
        return stored;
    }
    return 'narrow';
}

interface EditorState {
    mode: EditorMode;
    pageWidth: PageWidth;
    layout: PortalLayout;
    portalId: string | null;
    isDirty: boolean;
    isSaving: boolean;
    initialize: (portal: DeveloperPortal) => void;
    reset: () => void;
    toggleMode: () => void;
    setMode: (mode: EditorMode) => void;
    setPageWidth: (pageWidth: PageWidth) => void;
    setLayout: (layout: PortalLayout) => void;
    markDirty: () => void;
    clearDirty: () => void;
    save: (saveFn: () => Promise<void>) => Promise<void>;
}

const initialState = {
    mode: 'edit' as EditorMode,
    pageWidth: readStoredPageWidth(),
    layout: 'header-content-footer' as PortalLayout,
    portalId: null as string | null,
    isDirty: false,
    isSaving: false,
};

export const useEditorStore = create<EditorState>()(
    devtools(
        (set, get) => ({
            ...initialState,

            initialize: portal => {
                set({
                    layout: portal.layout,
                    portalId: portal.id,
                    mode: 'edit',
                    isDirty: false,
                    isSaving: false,
                });
            },

            reset: () => {
                set({ ...initialState, pageWidth: readStoredPageWidth() });
            },

            toggleMode: () => {
                const nextMode = get().mode === 'edit' ? 'preview' : 'edit';
                set({ mode: nextMode });
            },

            setMode: mode => set({ mode }),

            setPageWidth: pageWidth => {
                localStorage.setItem(PAGE_WIDTH_STORAGE_KEY, pageWidth);
                set({ pageWidth });
            },

            setLayout: layout => {
                set({ layout, isDirty: true });
            },

            markDirty: () => set({ isDirty: true }),

            clearDirty: () => set({ isDirty: false }),

            save: async saveFn => {
                set({ isSaving: true });
                try {
                    await saveFn();
                    set({ isSaving: false, isDirty: false });
                    notify.success('Changes saved');
                } catch (error) {
                    set({ isSaving: false });
                    notify.error(error, 'Failed to save changes.');
                    throw error;
                }
            },
        }),
        { name: 'editor' },
    ),
);
