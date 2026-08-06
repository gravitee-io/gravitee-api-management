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

import { GroupAssociationSection } from './GroupAssociationSection';
import type { GroupMembershipItem } from '../types/group';

// GroupMembershipTable's own spec covers its columns/search internals — here we only care that
// GroupAssociationSection picks the right branch (error vs. table) and forwards props correctly.
jest.mock('./GroupMembershipTable', () => ({
    GroupMembershipTable: (props: {
        items: GroupMembershipItem[];
        ariaLabel: string;
        searchPlaceholder: string;
        emptyTitle: string;
        showVersionColumn?: boolean;
    }) => (
        <div data-testid="group-membership-table" data-props={JSON.stringify(props)}>
            {props.items.map(i => i.name).join(', ')}
        </div>
    ),
}));

const BILLING_API: GroupMembershipItem = { id: 'api-1', name: 'Billing API', version: '1.0' };

describe('GroupAssociationSection', () => {
    it('renders the title and the membership table when there is no error', () => {
        render(
            <GroupAssociationSection
                title="APIs"
                error={false}
                errorMessage="Failed to load associated APIs. Please refresh and try again."
                items={[BILLING_API]}
                loading={false}
                ariaLabel="APIs"
                searchPlaceholder="Search APIs…"
                emptyTitle="No dependent APIs to display"
            />,
        );

        expect(screen.getByRole('heading', { name: 'APIs' })).not.toBeNull();
        expect(screen.getByTestId('group-membership-table').textContent).toContain('Billing API');
    });

    it('forwards showVersionColumn through to the membership table', () => {
        render(
            <GroupAssociationSection
                title="Applications"
                error={false}
                errorMessage="Failed to load associated applications. Please refresh and try again."
                items={[]}
                loading={false}
                ariaLabel="Applications"
                searchPlaceholder="Search Applications…"
                emptyTitle="No dependent applications to display"
                showVersionColumn={false}
            />,
        );

        const props = JSON.parse(screen.getByTestId('group-membership-table').getAttribute('data-props')!);
        expect(props.showVersionColumn).toBe(false);
    });

    it('shows a section error instead of the table when error is true', () => {
        render(
            <GroupAssociationSection
                title="APIs"
                error
                errorMessage="Failed to load associated APIs. Please refresh and try again."
                items={[BILLING_API]}
                loading={false}
                ariaLabel="APIs"
                searchPlaceholder="Search APIs…"
                emptyTitle="No dependent APIs to display"
            />,
        );

        expect(screen.getByText('Failed to load associated APIs. Please refresh and try again.')).not.toBeNull();
        expect(screen.queryByTestId('group-membership-table')).toBeNull();
    });

    it('renders the optional section action', async () => {
        const user = userEvent.setup();
        const onAction = jest.fn();
        render(
            <GroupAssociationSection
                title="APIs"
                error={false}
                errorMessage="Failed to load associated APIs. Please refresh and try again."
                items={[BILLING_API]}
                loading={false}
                ariaLabel="APIs"
                searchPlaceholder="Search APIs…"
                emptyTitle="No dependent APIs to display"
                actionLabel="Add group to existing APIs"
                onAction={onAction}
            />,
        );

        await user.click(screen.getByRole('button', { name: 'Add group to existing APIs' }));

        expect(onAction).toHaveBeenCalledTimes(1);
    });
});
