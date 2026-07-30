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
import userEvent from '@testing-library/user-event';

import { ShardingTagsTable } from './ShardingTagsTable';
import type { ShardingTagRow } from '../types/entrypoint';

const ROWS: ShardingTagRow[] = [
    {
        id: 'tag-1',
        key: 'prod',
        name: 'Production',
        description: 'Production gateway group',
        restrictedGroupIds: ['group-1'],
        restrictedGroupNames: ['Ops Team'],
    },
    {
        id: 'tag-2',
        key: 'dev',
        name: 'Development',
        description: '',
        restrictedGroupIds: [],
        restrictedGroupNames: [],
    },
];

describe('ShardingTagsTable', () => {
    it('renders sharding tag rows and opens detail on key click', () => {
        const onOpenDetail = jest.fn();
        render(<ShardingTagsTable rows={ROWS} canCreate={false} hasLicense onOpenDetail={onOpenDetail} onUpgrade={jest.fn()} />);
        expect(screen.getByText('prod')).not.toBeNull();
        expect(screen.getByText('dev')).not.toBeNull();
        fireEvent.click(screen.getByRole('button', { name: 'prod' }));
        expect(onOpenDetail).toHaveBeenCalledWith(ROWS[0]);
    });

    it('filters rows by search query', () => {
        render(<ShardingTagsTable rows={ROWS} canCreate={false} hasLicense onOpenDetail={jest.fn()} onUpgrade={jest.fn()} />);
        fireEvent.change(screen.getByLabelText('Search sharding tags'), { target: { value: 'dev' } });
        expect(screen.queryByText('prod')).toBeNull();
        expect(screen.getByText('dev')).not.toBeNull();
    });

    it('calls onEdit when Edit is selected from the actions menu', async () => {
        const user = userEvent.setup();
        const onEdit = jest.fn();
        render(
            <ShardingTagsTable
                rows={ROWS}
                canCreate={false}
                hasLicense
                canEdit
                onOpenDetail={jest.fn()}
                onEdit={onEdit}
                onUpgrade={jest.fn()}
            />,
        );
        await user.click(screen.getByRole('button', { name: /Actions for prod/i }));
        await user.click(await screen.findByRole('menuitem', { name: /^Edit$/ }));
        expect(onEdit).toHaveBeenCalledWith(ROWS[0]);
    });

    it('shows create CTA in empty state when canCreate is true', () => {
        const onCreate = jest.fn();
        render(<ShardingTagsTable rows={[]} canCreate hasLicense onOpenDetail={jest.fn()} onCreate={onCreate} onUpgrade={jest.fn()} />);
        fireEvent.click(screen.getByRole('button', { name: /Add a tag/i }));
        expect(onCreate).toHaveBeenCalled();
    });
});
