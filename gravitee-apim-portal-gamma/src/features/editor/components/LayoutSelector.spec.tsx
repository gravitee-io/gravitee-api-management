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
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { LayoutSelector } from './LayoutSelector';

describe('LayoutSelector', () => {
    it('should open the layout dialog when the Layout button is clicked', async () => {
        const user = userEvent.setup();

        renderWithGraphene(<LayoutSelector value="sidebar-content" onChange={jest.fn()} />);

        await user.click(screen.getByRole('button', { name: 'Portal layout' }));

        expect(screen.getByRole('heading', { name: 'Choose portal layout' })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: /Header layout/i })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: /Sidebar layout/i })).toBeInTheDocument();
    });

    it('should call onChange with header-content-footer layout and close the dialog', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();

        renderWithGraphene(<LayoutSelector value="sidebar-content" onChange={onChange} />);

        await user.click(screen.getByRole('button', { name: 'Portal layout' }));
        await user.click(screen.getByRole('option', { name: /Header layout/i }));

        expect(onChange).toHaveBeenCalledWith('header-content-footer');
        await waitFor(() => {
            expect(screen.queryByRole('heading', { name: 'Choose portal layout' })).not.toBeInTheDocument();
        });
    });

    it('should call onChange with sidebar-content layout and close the dialog', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();

        renderWithGraphene(<LayoutSelector value="header-content-footer" onChange={onChange} />);

        await user.click(screen.getByRole('button', { name: 'Portal layout' }));
        await user.click(screen.getByRole('option', { name: /Sidebar layout/i }));

        expect(onChange).toHaveBeenCalledWith('sidebar-content');
        await waitFor(() => {
            expect(screen.queryByRole('heading', { name: 'Choose portal layout' })).not.toBeInTheDocument();
        });
    });

    it('should mark the current layout as selected in the dialog', async () => {
        const user = userEvent.setup();

        renderWithGraphene(<LayoutSelector value="header-content-footer" onChange={jest.fn()} />);

        await user.click(screen.getByRole('button', { name: 'Portal layout' }));

        expect(screen.getByRole('option', { name: /Header layout/i })).toHaveAttribute('aria-selected', 'true');
        expect(screen.getByRole('option', { name: /Sidebar layout/i })).toHaveAttribute('aria-selected', 'false');
    });
});
