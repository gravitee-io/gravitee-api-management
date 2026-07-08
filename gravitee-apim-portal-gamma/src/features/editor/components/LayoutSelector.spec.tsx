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

import { LayoutSelector } from './LayoutSelector';

const defaultProps = {
    value: 'sidebar-content' as const,
    onChange: jest.fn(),
    pageWidth: 'medium' as const,
    onPageWidthChange: jest.fn(),
};

describe('LayoutSelector', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('should open the layout dialog when the Layout button is clicked', async () => {
        const user = userEvent.setup();

        renderWithGraphene(<LayoutSelector {...defaultProps} />);

        await user.click(screen.getByRole('button', { name: 'Portal layout' }));

        expect(screen.getByRole('heading', { name: 'Layout settings' })).toBeInTheDocument();
        expect(screen.getByLabelText('Page width')).toBeInTheDocument();
        expect(screen.getByRole('option', { name: /Header layout/i })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: /Sidebar layout/i })).toBeInTheDocument();
    });

    it('should call onPageWidthChange when width is changed', async () => {
        const user = userEvent.setup();
        const onPageWidthChange = jest.fn();

        renderWithGraphene(<LayoutSelector {...defaultProps} onPageWidthChange={onPageWidthChange} />);

        await user.click(screen.getByRole('button', { name: 'Portal layout' }));
        await user.click(screen.getByRole('radio', { name: 'Wide' }));

        expect(onPageWidthChange).toHaveBeenCalledWith('wide');
    });

    it('should call onChange with header-content-footer layout and keep the dialog open', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();

        renderWithGraphene(<LayoutSelector {...defaultProps} onChange={onChange} />);

        await user.click(screen.getByRole('button', { name: 'Portal layout' }));
        await user.click(screen.getByRole('option', { name: /Header layout/i }));

        expect(onChange).toHaveBeenCalledWith('header-content-footer');
        expect(screen.getByRole('heading', { name: 'Layout settings' })).toBeInTheDocument();
    });

    it('should call onChange with sidebar-content layout and keep the dialog open', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();

        renderWithGraphene(
            <LayoutSelector {...defaultProps} value="header-content-footer" onChange={onChange} />,
        );

        await user.click(screen.getByRole('button', { name: 'Portal layout' }));
        await user.click(screen.getByRole('option', { name: /Sidebar layout/i }));

        expect(onChange).toHaveBeenCalledWith('sidebar-content');
        expect(screen.getByRole('heading', { name: 'Layout settings' })).toBeInTheDocument();
    });

    it('should mark the current layout as selected in the dialog', async () => {
        const user = userEvent.setup();

        renderWithGraphene(<LayoutSelector {...defaultProps} value="header-content-footer" />);

        await user.click(screen.getByRole('button', { name: 'Portal layout' }));

        expect(screen.getByRole('option', { name: /Header layout/i })).toHaveAttribute('aria-selected', 'true');
        expect(screen.getByRole('option', { name: /Sidebar layout/i })).toHaveAttribute('aria-selected', 'false');
    });
});
