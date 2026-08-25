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
import { useQuery } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';

import { SharedPolicyGroupHistoryDetailsDialog } from './SharedPolicyGroupHistoryDetailsDialog';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';

jest.mock('@tanstack/react-query', () => ({ useQuery: jest.fn() }));
jest.mock('@gravitee/graphene-policy-studio', () => ({ getProtocolType: () => 'HTTP_PROXY' }));
jest.mock('./SharedPolicyGroupPolicyStudio', () => ({
    SharedPolicyGroupPolicyStudio: ({ readOnly }: { readOnly: boolean }) => <div>{readOnly ? 'Read-only Policy Studio' : 'Editable'}</div>,
}));

const mockUseQuery = jest.mocked(useQuery);
const HISTORY: SharedPolicyGroup = {
    id: 'spg-1',
    name: 'Auth Bundle',
    description: 'Authentication policies',
    prerequisiteMessage: 'Configure an identity provider',
    apiType: 'PROXY',
    phase: 'REQUEST',
    version: 2,
};

describe('SharedPolicyGroupHistoryDetailsDialog', () => {
    beforeEach(() => {
        mockUseQuery.mockReturnValue({
            data: [{ id: 'jwt', name: 'JWT' }],
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useQuery>);
    });

    it('shows metadata and the historical policy configuration as read-only', () => {
        render(
            <SharedPolicyGroupHistoryDetailsDialog
                sharedPolicyGroup={HISTORY}
                canRestore={false}
                onOpenChange={jest.fn()}
                onRestore={jest.fn()}
            />,
        );

        expect(screen.getByDisplayValue('Auth Bundle')).not.toBeNull();
        expect(screen.getByDisplayValue('Authentication policies')).not.toBeNull();
        expect(screen.getByText('Read-only Policy Studio')).not.toBeNull();
        expect(screen.queryByRole('button', { name: 'Restore version' })).toBeNull();
    });

    it('keeps the policy canvas wide instead of falling back to the narrow default dialog width', () => {
        render(
            <SharedPolicyGroupHistoryDetailsDialog
                sharedPolicyGroup={HISTORY}
                canRestore={false}
                onOpenChange={jest.fn()}
                onRestore={jest.fn()}
            />,
        );

        const dialog = screen.getByRole('dialog');
        expect(dialog.className).toContain('w-[min(72rem,calc(100vw-2rem))]');
        expect(dialog.className).toContain('sm:max-w-none');
    });

    it('offers restore only when the caller authorizes it', () => {
        const onRestore = jest.fn();
        render(
            <SharedPolicyGroupHistoryDetailsDialog sharedPolicyGroup={HISTORY} canRestore onOpenChange={jest.fn()} onRestore={onRestore} />,
        );

        fireEvent.click(screen.getByRole('button', { name: 'Restore version' }));
        expect(onRestore).toHaveBeenCalledWith(HISTORY);
    });
});
