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
import { resetAllStores } from '../../../testing/helpers';
import { notify } from '../../../shared/notify/notify';
import { useEditorStore } from './editor.store';

const portal = {
    id: 'portal-1',
    name: 'Test Portal',
    screenshotDataUrl: '',
    updatedAt: new Date().toISOString(),
    layout: 'sidebar-content' as const,
    showFooter: true,
    pageWidth: 'narrow' as const,
    portalIconUrl: '',
    portalLabel: 'Developer Portal',
    footerLinks: [],
    userMenuItems: [],
};

describe('editorStore', () => {
    beforeEach(() => {
        resetAllStores();
        jest.spyOn(notify, 'success').mockImplementation(() => undefined);
        jest.spyOn(notify, 'error').mockImplementation(() => undefined);
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('should initialize from portal data', () => {
        useEditorStore.getState().initialize(portal);

        const state = useEditorStore.getState();
        expect(state.portalId).toBe('portal-1');
        expect(state.layout).toBe('sidebar-content');
        expect(state.showFooter).toBe(true);
        expect(state.pageWidth).toBe('narrow');
        expect(state.mode).toBe('edit');
        expect(state.isDirty).toBe(false);
    });

    it('should toggle between edit and preview modes', () => {
        expect(useEditorStore.getState().mode).toBe('edit');

        useEditorStore.getState().toggleMode();
        expect(useEditorStore.getState().mode).toBe('preview');

        useEditorStore.getState().toggleMode();
        expect(useEditorStore.getState().mode).toBe('edit');
    });

    it('should load page width from portal on initialize', () => {
        useEditorStore.getState().initialize({ ...portal, pageWidth: 'wide' });

        expect(useEditorStore.getState().pageWidth).toBe('wide');
    });

    it('should mark page width changes as dirty', () => {
        useEditorStore.getState().setPageWidth('wide');

        expect(useEditorStore.getState().pageWidth).toBe('wide');
        expect(useEditorStore.getState().isDirty).toBe(true);
    });

    it('should persist preview viewport to localStorage', () => {
        useEditorStore.getState().setPreviewViewport('mobile');

        expect(useEditorStore.getState().previewViewport).toBe('mobile');
        expect(localStorage.getItem('gravitee-portal-gamma-preview-viewport')).toBe('mobile');
    });

    it('should load show footer from portal on initialize', () => {
        useEditorStore.getState().initialize({ ...portal, showFooter: false });

        expect(useEditorStore.getState().showFooter).toBe(false);
    });

    it('should mark show footer changes as dirty', () => {
        useEditorStore.getState().setShowFooter(false);

        expect(useEditorStore.getState().showFooter).toBe(false);
        expect(useEditorStore.getState().isDirty).toBe(true);
    });

    it('should mark layout changes as dirty', () => {
        useEditorStore.getState().setLayout('sidebar-content');

        expect(useEditorStore.getState().layout).toBe('sidebar-content');
        expect(useEditorStore.getState().isDirty).toBe(true);
    });

    it('should run save flow and clear dirty state on success', async () => {
        useEditorStore.getState().setLayout('sidebar-content');
        const saveFn = jest.fn().mockResolvedValue(undefined);

        await useEditorStore.getState().save(saveFn);

        expect(saveFn).toHaveBeenCalled();
        expect(useEditorStore.getState().isSaving).toBe(false);
        expect(useEditorStore.getState().isDirty).toBe(false);
        expect(notify.success).toHaveBeenCalledWith('Changes saved');
    });

    it('should show error toast when save fails', async () => {
        const saveFn = jest.fn().mockRejectedValue(new Error('failed'));

        await expect(useEditorStore.getState().save(saveFn)).rejects.toThrow('failed');
        expect(notify.error).toHaveBeenCalledWith(expect.any(Error), 'Failed to save changes.');
        expect(useEditorStore.getState().isSaving).toBe(false);
    });
});
