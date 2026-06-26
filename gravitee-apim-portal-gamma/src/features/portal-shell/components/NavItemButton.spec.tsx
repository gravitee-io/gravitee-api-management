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
import { fireEvent, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { NavItemButton } from './NavItemButton';

describe('NavItemButton', () => {
    it('should call onSelect when clicked', () => {
        const onSelect = jest.fn();
        renderWithGraphene(
            <NavItemButton
                label="Home"
                selected={false}
                showDelete={false}
                onSelect={onSelect}
                onDelete={jest.fn()}
            />,
        );

        fireEvent.click(screen.getByRole('button', { name: 'Home' }));
        expect(onSelect).toHaveBeenCalled();
    });

    it('should call onDelete without selecting when delete button is clicked', async () => {
        const user = userEvent.setup();
        const onSelect = jest.fn();
        const onDelete = jest.fn();

        renderWithGraphene(
            <NavItemButton
                label="Guides"
                selected={false}
                showDelete
                onSelect={onSelect}
                onDelete={onDelete}
            />,
        );

        await user.click(screen.getByRole('button', { name: 'Delete Guides' }));
        expect(onDelete).toHaveBeenCalled();
        expect(onSelect).not.toHaveBeenCalled();
    });

    it('should call onSelect when clicked in edit mode without entering rename', async () => {
        const user = userEvent.setup();
        const onSelect = jest.fn();
        const onLabelChange = jest.fn();

        renderWithGraphene(
            <NavItemButton
                label="Home"
                selected={false}
                showDelete
                onSelect={onSelect}
                onDelete={jest.fn()}
                onLabelChange={onLabelChange}
            />,
        );

        await user.click(screen.getByLabelText('Edit Home'));

        expect(onSelect).toHaveBeenCalled();
        expect(screen.queryByRole('textbox', { name: 'Edit Home' })).not.toBeInTheDocument();
    });

    it('should enter rename on double click in edit mode', async () => {
        const user = userEvent.setup();
        const onSelect = jest.fn();
        const onLabelChange = jest.fn();

        renderWithGraphene(
            <NavItemButton
                label="Home"
                selected={false}
                showDelete
                onSelect={onSelect}
                onDelete={jest.fn()}
                onLabelChange={onLabelChange}
            />,
        );

        await user.dblClick(screen.getByLabelText('Edit Home'));

        expect(screen.getByRole('textbox', { name: 'Edit Home' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Delete Home' })).not.toBeInTheDocument();
    });

    it('should allow spaces in rename input without selecting the nav item', async () => {
        const user = userEvent.setup();
        const onSelect = jest.fn();
        const onLabelChange = jest.fn();

        renderWithGraphene(
            <NavItemButton
                label="Home"
                selected={false}
                showDelete
                onSelect={onSelect}
                onDelete={jest.fn()}
                onLabelChange={onLabelChange}
            />,
        );

        await user.dblClick(screen.getByLabelText('Edit Home'));
        const input = screen.getByRole('textbox', { name: 'Edit Home' });
        onSelect.mockClear();
        await user.type(input, ' My Page');

        expect(input).toHaveValue('Home My Page');
    });
});
