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

import { SharedPolicyGroupDeleteSheet } from './SharedPolicyGroupDeleteSheet';

describe('SharedPolicyGroupDeleteSheet', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders the confirmation title and warning copy', () => {
        render(<SharedPolicyGroupDeleteSheet open onClose={jest.fn()} onConfirm={jest.fn()} isDeleting={false} />);
        expect(screen.queryByRole('heading', { name: 'Remove Shared Policy Group' })).not.toBeNull();
        expect(screen.queryByText(/Are you sure you want to remove this Shared Policy Group/)).not.toBeNull();
        expect(screen.queryByText(/inform API publishers/)).not.toBeNull();
    });

    it('calls onConfirm when Remove is clicked', () => {
        const onConfirm = jest.fn();
        render(<SharedPolicyGroupDeleteSheet open onClose={jest.fn()} onConfirm={onConfirm} isDeleting={false} />);
        fireEvent.click(screen.getByRole('button', { name: 'Remove' }));
        expect(onConfirm).toHaveBeenCalled();
    });

    it('calls onClose when Cancel is clicked', () => {
        const onClose = jest.fn();
        render(<SharedPolicyGroupDeleteSheet open onClose={onClose} onConfirm={jest.fn()} isDeleting={false} />);
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalled();
    });

    it('disables actions while deleting', () => {
        render(<SharedPolicyGroupDeleteSheet open onClose={jest.fn()} onConfirm={jest.fn()} isDeleting />);
        expect(screen.getByRole('button', { name: 'Removing…' })).toHaveProperty('disabled', true);
        expect(screen.getByRole('button', { name: 'Cancel' })).toHaveProperty('disabled', true);
    });
});
