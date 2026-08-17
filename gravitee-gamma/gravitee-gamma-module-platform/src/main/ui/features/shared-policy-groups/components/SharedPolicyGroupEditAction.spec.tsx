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

import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import { SharedPolicyGroupEditAction } from './SharedPolicyGroupEditAction';
import { notify } from '../../../shared/notify';
import { installFormActionTestEnvironment } from '../../../shared/testing/formAction';
import { useUpdateSharedPolicyGroup } from '../hooks/useSharedPolicyGroupMutations';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';

let restoreTestEnvironment: () => void;

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
}));
jest.mock('../hooks/useSharedPolicyGroupMutations');
jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn() },
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseUpdateSharedPolicyGroup = jest.mocked(useUpdateSharedPolicyGroup);

const SHARED_POLICY_GROUP: SharedPolicyGroup = {
    id: 'spg-1',
    name: 'Auth Bundle',
    description: 'Reusable auth policies',
    prerequisiteMessage: 'Requires the "auth-cache" resource',
    lifecycleState: 'DEPLOYED',
    apiType: 'PROXY',
    phase: 'REQUEST',
    steps: [{ name: 'jwt' }],
};

function makeMutation(mutateAsync = jest.fn()) {
    return { mutateAsync } as unknown as ReturnType<typeof useUpdateSharedPolicyGroup>;
}

describe('SharedPolicyGroupEditAction', () => {
    beforeAll(() => {
        restoreTestEnvironment = installFormActionTestEnvironment();
    });

    afterAll(() => {
        restoreTestEnvironment();
    });

    beforeEach(() => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseUpdateSharedPolicyGroup.mockReturnValue(makeMutation());
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('updates Shared Policy Group metadata', async () => {
        const updateMutateAsync = jest.fn().mockResolvedValue(SHARED_POLICY_GROUP);
        mockUseUpdateSharedPolicyGroup.mockReturnValue(makeMutation(updateMutateAsync));
        render(<SharedPolicyGroupEditAction sharedPolicyGroup={SHARED_POLICY_GROUP} />);

        fireEvent.click(screen.getByRole('button', { name: 'Edit' }));
        fireEvent.change(await screen.findByLabelText(/Name/i), { target: { value: 'Renamed Bundle' } });
        fireEvent.click(screen.getByRole('button', { name: 'Save' }));

        await waitFor(() =>
            expect(updateMutateAsync).toHaveBeenCalledWith({
                id: 'spg-1',
                payload: {
                    name: 'Renamed Bundle',
                    description: 'Reusable auth policies',
                    prerequisiteMessage: 'Requires the "auth-cache" resource',
                },
            }),
        );
        expect(notify.success).toHaveBeenCalledWith('Shared Policy Group updated');
    });

    it('keeps the edit sheet open when the update fails', async () => {
        const error = new Error('update failed');
        mockUseUpdateSharedPolicyGroup.mockReturnValue(makeMutation(jest.fn().mockRejectedValue(error)));
        render(<SharedPolicyGroupEditAction sharedPolicyGroup={SHARED_POLICY_GROUP} />);

        fireEvent.click(screen.getByRole('button', { name: 'Edit' }));
        fireEvent.click(await screen.findByRole('button', { name: 'Save' }));

        await waitFor(() => expect(notify.error).toHaveBeenCalledWith(error, 'Error during Shared Policy Group update!'));
        expect(screen.queryByRole('heading', { name: 'Edit Shared Policy Group' })).not.toBeNull();
    });

    it('hides Edit without update permission', () => {
        mockUseHasPermission.mockReturnValue(false);
        render(<SharedPolicyGroupEditAction sharedPolicyGroup={SHARED_POLICY_GROUP} />);

        expect(screen.queryByRole('button', { name: 'Edit' })).toBeNull();
    });

    it('hides Edit for a Kubernetes-origin Shared Policy Group', () => {
        render(<SharedPolicyGroupEditAction sharedPolicyGroup={{ ...SHARED_POLICY_GROUP, originContext: { origin: 'KUBERNETES' } }} />);

        expect(screen.queryByRole('button', { name: 'Edit' })).toBeNull();
    });
});
