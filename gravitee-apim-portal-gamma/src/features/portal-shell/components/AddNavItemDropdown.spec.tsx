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

import { USER_MENU_PAGE_TYPE_OPTIONS } from '../utils/page-type-options';
import { AddNavItemDropdown } from './AddNavItemDropdown';

describe('AddNavItemDropdown', () => {
    it('should open the page type dialog when Page is selected', async () => {
        const user = userEvent.setup();
        const onAdd = jest.fn();

        renderWithGraphene(<AddNavItemDropdown parentId={null} onAdd={onAdd} />);

        await user.click(screen.getByRole('button', { name: 'Add navigation item' }));
        await user.click(screen.getByRole('menuitem', { name: 'Page' }));

        expect(screen.getByRole('heading', { name: 'Choose page type' })).toBeInTheDocument();
        expect(onAdd).not.toHaveBeenCalled();
    });

    it('should create a block page from the dialog', async () => {
        const user = userEvent.setup();
        const onAdd = jest.fn();

        renderWithGraphene(<AddNavItemDropdown parentId={null} onAdd={onAdd} />);

        await user.click(screen.getByRole('button', { name: 'Add navigation item' }));
        await user.click(screen.getByRole('menuitem', { name: 'Page' }));
        await user.click(screen.getByRole('option', { name: /Block/i }));

        expect(onAdd).toHaveBeenCalledWith('PAGE', null, { contentType: 'BLOCK' });
    });

    it('should create an OpenAPI page without choosing a renderer upfront', async () => {
        const user = userEvent.setup();
        const onAdd = jest.fn();

        renderWithGraphene(<AddNavItemDropdown parentId={null} onAdd={onAdd} />);

        await user.click(screen.getByRole('button', { name: 'Add navigation item' }));
        await user.click(screen.getByRole('menuitem', { name: 'Page' }));
        await user.click(screen.getByRole('option', { name: /OpenAPI/i }));

        expect(onAdd).toHaveBeenCalledWith('PAGE', null, { contentType: 'OPENAPI' });
    });

    it('should respect a restricted page type option list', async () => {
        const user = userEvent.setup();
        const onAdd = jest.fn();

        renderWithGraphene(
            <AddNavItemDropdown
                parentId={null}
                onAdd={onAdd}
                pageTypeOptions={USER_MENU_PAGE_TYPE_OPTIONS}
            />,
        );

        await user.click(screen.getByRole('button', { name: 'Add navigation item' }));
        await user.click(screen.getByRole('menuitem', { name: 'Page' }));

        expect(screen.getByRole('option', { name: /Block/i })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: /HTML/i })).toBeInTheDocument();
        expect(screen.queryByRole('option', { name: /OpenAPI/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('option', { name: /AsyncAPI/i })).not.toBeInTheDocument();
    });
});
