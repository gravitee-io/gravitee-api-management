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
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';

import { useTasks } from './useTasks';
import { useEnvironmentStore } from '../../features/environment/environment.store';
import { TEST_ENVIRONMENTS, TEST_MANAGEMENT_BASE } from '../../testing/factories';
import { resetAllStores, respondWith, seedBootstrap, seedEnvironments } from '../../testing/helpers';

const USER_REGISTRATION_RESPONSE = {
    data: [{ type: 'USER_REGISTRATION_APPROVAL', created_at: 1_700_000_000_000, data: { id: 'user-1', displayName: 'Ada' } }],
    metadata: {},
    page: { total_elements: 1 },
};

function renderAt(path: string) {
    return renderHook(() => useTasks(), {
        wrapper: ({ children }: { readonly children: ReactNode }) => <MemoryRouter initialEntries={[path]}>{children}</MemoryRouter>,
    });
}

describe('useTasks', () => {
    beforeEach(() => {
        resetAllStores();
        seedBootstrap();
        seedEnvironments();
        respondWith('get', `${TEST_MANAGEMENT_BASE}/user/tasks`, USER_REGISTRATION_RESPONSE);
    });

    it('should link a task with no environment to the environment of the address', async () => {
        const { result } = renderAt('/environments/env-2/home');

        await waitFor(() => expect(result.current.tasks[0]?.to).toBe('/environments/env-2/platform/users/user-1'));
    });

    it('should link a task with no environment to the one of the session from Account Management', async () => {
        useEnvironmentStore.setState({ currentEnvironment: TEST_ENVIRONMENTS[1], environmentId: TEST_ENVIRONMENTS[1]!.id });

        const { result } = renderAt('/account/overview');

        await waitFor(() => expect(result.current.tasks[0]?.to).toBe('/environments/env-2/platform/users/user-1'));
    });
});
