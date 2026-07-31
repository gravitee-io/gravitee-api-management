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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { ShardingTagDeleteDialog } from './ShardingTagDeleteDialog';
import type { ShardingTagRow } from '../types/entrypoint';

const TAG: ShardingTagRow = {
    id: 'tag-1',
    key: 'prod',
    name: 'Production',
    description: 'Prod tag',
    restrictedGroupIds: [],
    restrictedGroupNames: [],
};

describe('ShardingTagDeleteDialog', () => {
    it('does not show title when closed', () => {
        render(<ShardingTagDeleteDialog open={false} tag={TAG} onClose={jest.fn()} onConfirm={jest.fn()} isDeleting={false} />);
        expect(screen.queryByText('Delete Sharding Tag')).toBeNull();
    });

    it('shows title and tag name in message when open', () => {
        render(<ShardingTagDeleteDialog open tag={TAG} onClose={jest.fn()} onConfirm={jest.fn()} isDeleting={false} />);
        expect(screen.getByText('Delete Sharding Tag')).not.toBeNull();
        expect(screen.getByText('Production')).not.toBeNull();
        expect(screen.getByText(/Are you sure you want to delete the tag/)).not.toBeNull();
    });

    it('shows single entrypoint update and delete impact messages', () => {
        render(
            <ShardingTagDeleteDialog
                open
                tag={TAG}
                entrypointsToUpdate={['https://gateway.example.com']}
                entrypointsToDelete={['https://solo.example.com']}
                onClose={jest.fn()}
                onConfirm={jest.fn()}
                isDeleting={false}
            />,
        );
        expect(screen.getByText(/The tag will be removed for the entrypoint/)).not.toBeNull();
        expect(screen.getByText('https://gateway.example.com')).not.toBeNull();
        expect(screen.getByText(/entrypoint will be deleted as it is only using this tag/)).not.toBeNull();
        expect(screen.getByText('https://solo.example.com')).not.toBeNull();
    });

    it('shows lists when multiple entrypoints are impacted', () => {
        render(
            <ShardingTagDeleteDialog
                open
                tag={TAG}
                entrypointsToUpdate={['https://a.example.com', 'https://b.example.com']}
                entrypointsToDelete={['https://c.example.com', 'https://d.example.com']}
                onClose={jest.fn()}
                onConfirm={jest.fn()}
                isDeleting={false}
            />,
        );
        expect(screen.getByText(/The tag will be removed from all these entrypoints/)).not.toBeNull();
        expect(screen.getByText(/The following entrypoints will be deleted as they are only using this tag/)).not.toBeNull();
        expect(screen.getByText('https://a.example.com')).not.toBeNull();
        expect(screen.getByText('https://d.example.com')).not.toBeNull();
    });

    it('falls back to tag key when name is empty', () => {
        render(<ShardingTagDeleteDialog open tag={{ ...TAG, name: '' }} onClose={jest.fn()} onConfirm={jest.fn()} isDeleting={false} />);
        expect(screen.getByText('prod')).not.toBeNull();
    });

    it('calls onClose when Cancel is clicked', async () => {
        const user = userEvent.setup();
        const onClose = jest.fn();
        render(<ShardingTagDeleteDialog open tag={TAG} onClose={onClose} onConfirm={jest.fn()} isDeleting={false} />);
        await user.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalled();
    });

    it('calls onConfirm when Delete is clicked', async () => {
        const user = userEvent.setup();
        const onConfirm = jest.fn();
        render(<ShardingTagDeleteDialog open tag={TAG} onClose={jest.fn()} onConfirm={onConfirm} isDeleting={false} />);
        await user.click(screen.getByRole('button', { name: 'Delete' }));
        expect(onConfirm).toHaveBeenCalled();
    });

    it('disables buttons and shows Deleting… while deleting', () => {
        render(<ShardingTagDeleteDialog open tag={TAG} onClose={jest.fn()} onConfirm={jest.fn()} isDeleting />);
        expect(screen.getByRole('button', { name: 'Deleting…' })).toHaveProperty('disabled', true);
        expect(screen.getByRole('button', { name: 'Cancel' })).toHaveProperty('disabled', true);
    });
});
