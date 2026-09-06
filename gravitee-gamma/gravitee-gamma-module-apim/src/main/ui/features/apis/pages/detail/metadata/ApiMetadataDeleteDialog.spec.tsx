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

import { ApiMetadataDeleteDialog } from './ApiMetadataDeleteDialog';
import type { ApiMetadata } from '../../../types/metadata';

const API_ONLY: ApiMetadata = { key: 'team', name: 'Team', format: 'STRING', value: 'Platform' };
const GLOBAL_OVERRIDE: ApiMetadata = {
    key: 'support-email',
    name: 'Support Email',
    format: 'MAIL',
    value: 'api@example.com',
    defaultValue: 'help@example.com',
};

function renderDialog(metadata: ApiMetadata, open = true) {
    const onClose = jest.fn();
    const onConfirm = jest.fn();
    render(<ApiMetadataDeleteDialog open={open} metadata={metadata} onClose={onClose} onConfirm={onConfirm} isDeleting={false} />);
    return { onClose, onConfirm };
}

describe('ApiMetadataDeleteDialog', () => {
    it('asks to delete API-only metadata', () => {
        renderDialog(API_ONLY);
        expect(screen.getByRole('heading', { name: 'Delete API metadata' })).not.toBeNull();
        expect(screen.getByRole('button', { name: 'Delete' })).not.toBeNull();
    });

    it('asks to reset inherited metadata that has an API override', () => {
        renderDialog(GLOBAL_OVERRIDE);
        expect(screen.getByRole('heading', { name: 'Reset global metadata' })).not.toBeNull();
        expect(screen.getByText(/help@example.com/)).not.toBeNull();
        expect(screen.getByRole('button', { name: 'Reset' })).not.toBeNull();
    });

    it('invokes onConfirm from the destructive action', () => {
        const { onConfirm } = renderDialog(API_ONLY);
        fireEvent.click(screen.getByRole('button', { name: 'Delete' }));
        expect(onConfirm).toHaveBeenCalledTimes(1);
    });
});
