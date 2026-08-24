/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { fireEvent, render, screen } from '@testing-library/react';
import type { ComponentProps } from 'react';

import { OrgSettingsFormShell } from './OrgSettingsFormShell';

type ShellProps = ComponentProps<typeof OrgSettingsFormShell>;

function renderShell(overrides: Partial<ShellProps> = {}) {
    const onSave = jest.fn();
    const onDiscard = jest.fn();
    render(
        <OrgSettingsFormShell
            title="CORS"
            description="Configure CORS for the Management Console."
            canEdit
            isDirty
            isValid
            isSaving={false}
            isLoading={false}
            isError={false}
            onSave={onSave}
            onDiscard={onDiscard}
            {...overrides}
        >
            {overrides.children ?? <div data-testid="form-content">Form fields</div>}
        </OrgSettingsFormShell>,
    );
    return { onSave, onDiscard };
}

describe('OrgSettingsFormShell', () => {
    it('places Discard and Save after the form content in a sticky bottom bar when dirty', () => {
        renderShell();

        const content = screen.getByTestId('form-content');
        const discard = screen.getByRole('button', { name: 'Discard' });
        const save = screen.getByRole('button', { name: /Save changes/i });

        expect(content.compareDocumentPosition(discard) & Node.DOCUMENT_POSITION_FOLLOWING).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
        expect(content.compareDocumentPosition(save) & Node.DOCUMENT_POSITION_FOLLOWING).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
        expect(save.closest('.sticky.bottom-0')).not.toBeNull();
        expect(screen.getByRole('heading', { name: 'CORS' }).closest('.flex.items-start.justify-between')).toBeNull();
    });

    it('does not show Discard or Save when there are no unsaved changes', () => {
        renderShell({ isDirty: false });

        expect(screen.queryByRole('button', { name: 'Discard' })).toBeNull();
        expect(screen.queryByRole('button', { name: /Save changes/i })).toBeNull();
    });

    it('does not show Discard or Save when the user cannot edit', () => {
        renderShell({ canEdit: false });

        expect(screen.queryByRole('button', { name: 'Discard' })).toBeNull();
        expect(screen.queryByRole('button', { name: /Save changes/i })).toBeNull();
    });

    it('invokes discard and save handlers', () => {
        const { onSave, onDiscard } = renderShell();

        fireEvent.click(screen.getByRole('button', { name: 'Discard' }));
        fireEvent.click(screen.getByRole('button', { name: /Save changes/i }));

        expect(onDiscard).toHaveBeenCalledTimes(1);
        expect(onSave).toHaveBeenCalledTimes(1);
    });

    it('disables Save when the form is invalid', () => {
        renderShell({ isValid: false });

        expect(screen.getByRole('button', { name: /Save changes/i })).toHaveProperty('disabled', true);
        expect(screen.getByRole('button', { name: 'Discard' })).toHaveProperty('disabled', false);
    });
});
