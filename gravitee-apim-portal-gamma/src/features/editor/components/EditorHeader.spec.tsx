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
import { MemoryRouter } from 'react-router-dom';

import { EditorHeader } from './EditorHeader';

const defaultProps = {
    portalName: 'Payments Portal',
    mode: 'edit' as const,
    pageWidth: 'narrow' as const,
    layout: 'header-content-footer' as const,
    isSaving: false,
    onModeChange: jest.fn(),
    onPageWidthChange: jest.fn(),
    onLayoutChange: jest.fn(),
    onPortalNameChange: jest.fn(),
    onSave: jest.fn(),
};

function renderHeader(props: Partial<typeof defaultProps> = {}) {
    return renderWithGraphene(
        <MemoryRouter>
            <EditorHeader {...defaultProps} {...props} />
        </MemoryRouter>,
    );
}

describe('EditorHeader', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('should render portal designer title with product icon', () => {
        renderHeader();

        expect(screen.getByText('Portal designer')).toBeInTheDocument();
        expect(screen.queryByRole('link', { name: 'Back to dashboards' })).not.toBeInTheDocument();
    });

    it('should render edit mode controls', () => {
        renderHeader();

        expect(screen.getByText('Payments Portal')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Save' })).toBeInTheDocument();
        expect(screen.getByLabelText('Page width')).toBeInTheDocument();
        expect(screen.getByLabelText('Portal layout')).toBeInTheDocument();
    });

    it('should hide save and selectors in preview mode', () => {
        renderHeader({ mode: 'preview' });

        expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument();
        expect(screen.queryByLabelText('Page width')).not.toBeInTheDocument();
        expect(screen.queryByLabelText('Portal layout')).not.toBeInTheDocument();
        expect(screen.getByLabelText('Editor mode')).toBeInTheDocument();
    });

    it('should switch to preview mode when preview toggle is clicked', async () => {
        const user = userEvent.setup();
        const onModeChange = jest.fn();

        renderHeader({ onModeChange });

        await user.click(screen.getByRole('radio', { name: 'Preview mode' }));

        expect(onModeChange).toHaveBeenCalledWith('preview');
    });

    it('should trigger save callback when save is clicked', async () => {
        const user = userEvent.setup();
        const onSave = jest.fn();

        renderHeader({ onSave });

        await user.click(screen.getByRole('button', { name: 'Save' }));

        expect(onSave).toHaveBeenCalled();
    });

    it('should show saving label and disable save button while saving', () => {
        renderHeader({ isSaving: true });

        expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
    });

    it('should rename portal on double click in edit mode', async () => {
        const user = userEvent.setup();
        const onPortalNameChange = jest.fn();

        renderHeader({ onPortalNameChange });

        await user.dblClick(screen.getByLabelText('Portal name'));
        const input = screen.getByRole('textbox', { name: 'Portal name' });
        await user.clear(input);
        await user.type(input, 'Partner Portal{Enter}');

        expect(onPortalNameChange).toHaveBeenCalledWith('Partner Portal');
    });

    it('should not allow renaming portal name in preview mode', async () => {
        const user = userEvent.setup();

        renderHeader({ mode: 'preview' });

        expect(screen.getByText('Payments Portal')).toBeInTheDocument();
        await user.dblClick(screen.getByText('Payments Portal'));

        expect(screen.queryByRole('textbox', { name: 'Portal name' })).not.toBeInTheDocument();
    });
});
