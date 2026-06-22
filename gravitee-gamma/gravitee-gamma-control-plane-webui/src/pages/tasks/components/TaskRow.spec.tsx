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
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';

import { TaskRow } from './TaskRow';
import { useModulesStore } from '../../../features/modules';
import { resetAllStores } from '../../../testing/helpers';
import type { TaskView } from '../tasks.types';

function makeTask(overrides: Partial<TaskView> = {}): TaskView {
    return {
        id: 'task-1',
        type: 'SUBSCRIPTION_APPROVAL',
        category: 'SUBSCRIPTION',
        categoryLabel: 'Subscription Approval',
        actionLabel: 'Validate subscription',
        iconKey: 'subscription',
        area: { key: 'apim', label: 'API Management' },
        title: 'Passenger App → Flight Status API',
        subtitle: 'Plan: Gold',
        createdAt: 0,
        to: '/environments/env-1/apim/apis/api-http/consumers/sub-1',
        toModuleId: 'apim',
        entity: { type: 'SUBSCRIPTION_APPROVAL', created_at: 0, data: {} },
        ...overrides,
    };
}

function seedModule(id: string) {
    useModulesStore.setState({
        modules: [{ id, name: 'API Management', version: '1.0', remoteName: 'remote', exposedModule: 'App' }],
    });
}

function LocationProbe() {
    return <span data-testid="location">{useLocation().pathname}</span>;
}

function renderRow(task: TaskView) {
    return render(
        <MemoryRouter initialEntries={['/start']}>
            <TaskRow task={task} />
            <Routes>
                <Route path="*" element={<LocationProbe />} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('TaskRow', () => {
    beforeEach(() => {
        resetAllStores();
    });

    it('navigates to the task target when its module is available', () => {
        seedModule('apim');
        renderRow(makeTask());

        fireEvent.click(screen.getByRole('button', { name: /Validate subscription/ }));

        expect(screen.getByTestId('location').textContent).toBe('/environments/env-1/apim/apis/api-http/consumers/sub-1');
    });

    it('renders no action and never navigates when the task has no destination', () => {
        renderRow(makeTask({ actionLabel: 'Validate user', to: null, toModuleId: null }));

        expect(screen.getByText('Passenger App → Flight Status API')).toBeTruthy();
        expect(screen.queryByRole('button')).toBeNull();
        expect(screen.getByTestId('location').textContent).toBe('/start');
    });
});
