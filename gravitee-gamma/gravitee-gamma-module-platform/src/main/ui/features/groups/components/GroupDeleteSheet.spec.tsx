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

import { GroupDeleteSheet } from './GroupDeleteSheet';
import type { Group } from '../types/group';

const GROUP: Group = { id: 'group-1', name: 'API Team' };

describe('GroupDeleteSheet', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('deletable group', () => {
        it('asks for confirmation and calls onConfirm', () => {
            const onConfirm = jest.fn();
            render(<GroupDeleteSheet open group={GROUP} onClose={jest.fn()} onConfirm={onConfirm} isDeleting={false} />);

            expect(screen.queryByRole('heading', { name: 'Delete group' })).not.toBeNull();
            expect(screen.getByRole('button', { name: 'Delete' })).not.toBeNull();

            fireEvent.click(screen.getByRole('button', { name: 'Delete' }));
            expect(onConfirm).toHaveBeenCalled();
        });

        it('calls onClose when Cancel is clicked', () => {
            const onClose = jest.fn();
            render(<GroupDeleteSheet open group={GROUP} onClose={onClose} onConfirm={jest.fn()} isDeleting={false} />);

            fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
            expect(onClose).toHaveBeenCalled();
        });
    });

    describe('primary-owner group (blocked)', () => {
        it.each([
            ['primary_owner is true', { primary_owner: true }],
            ['apiPrimaryOwner is set', { apiPrimaryOwner: 'user-1' }],
            ['apiProductPrimaryOwner is set', { apiProductPrimaryOwner: 'user-1' }],
        ])('shows why the group cannot be deleted, with no Delete button, when %s', (_label, override) => {
            const onConfirm = jest.fn();
            render(
                <GroupDeleteSheet open group={{ ...GROUP, ...override }} onClose={jest.fn()} onConfirm={onConfirm} isDeleting={false} />,
            );

            expect(screen.queryByRole('heading', { name: 'Delete group' })).not.toBeNull();
            expect(screen.queryByText(/cannot be deleted while it still has a primary owner membership/i)).not.toBeNull();
            expect(screen.queryByRole('button', { name: 'Delete' })).toBeNull();
            expect(onConfirm).not.toHaveBeenCalled();
        });

        it('still lets the user cancel out of the blocked dialog', () => {
            const onClose = jest.fn();
            render(
                <GroupDeleteSheet
                    open
                    group={{ ...GROUP, primary_owner: true }}
                    onClose={onClose}
                    onConfirm={jest.fn()}
                    isDeleting={false}
                />,
            );

            fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
            expect(onClose).toHaveBeenCalled();
        });
    });
});
