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
import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { TreeAddButton } from './TreeAddButton';

describe('TreeAddButton', () => {
    it('should call onAdd for non-API types', async () => {
        const user = userEvent.setup();
        const onAdd = jest.fn();
        const onRequestApi = jest.fn();

        renderWithGraphene(
            <TreeAddButton parentId="folder-1" depth={2} onAdd={onAdd} onRequestApi={onRequestApi} />,
        );

        await user.click(screen.getByLabelText('Add navigation item'));
        await user.click(screen.getByRole('menuitem', { name: 'Page' }));

        expect(onAdd).toHaveBeenCalledWith('PAGE', 'folder-1');
        expect(onRequestApi).not.toHaveBeenCalled();
    });

    it('should call onRequestApi when API type is chosen', async () => {
        const user = userEvent.setup();
        const onAdd = jest.fn();
        const onRequestApi = jest.fn();

        renderWithGraphene(
            <TreeAddButton parentId="folder-1" depth={1} onAdd={onAdd} onRequestApi={onRequestApi} />,
        );

        await user.click(screen.getByLabelText('Add navigation item'));
        await user.click(screen.getByRole('menuitem', { name: 'API' }));

        expect(onRequestApi).toHaveBeenCalledWith('folder-1');
        expect(onAdd).not.toHaveBeenCalled();
    });

    it('should indent the add button based on depth', () => {
        const { container } = renderWithGraphene(
            <TreeAddButton parentId={null} depth={3} onAdd={jest.fn()} onRequestApi={jest.fn()} />,
        );

        const row = container.querySelector('[class*="addButtonRow"]');
        expect(row).toHaveStyle({ '--tree-depth': '3' });
    });

    it('should align the add button with nav item icons using a chevron spacer', () => {
        const { container } = renderWithGraphene(
            <TreeAddButton parentId="folder-1" depth={1} onAdd={jest.fn()} onRequestApi={jest.fn()} />,
        );

        const row = container.querySelector('[class*="addButtonRow"]');
        expect(row?.querySelector('[class*="chevronSpacer"]')).toBeInTheDocument();
    });
});
