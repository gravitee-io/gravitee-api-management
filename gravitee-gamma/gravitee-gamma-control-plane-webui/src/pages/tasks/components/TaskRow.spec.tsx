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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { act } from 'react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';

import { TaskRow } from './TaskRow';
import { useModulesStore } from '../../../features/modules';
import { permissionService } from '../../../shared/permissions/permission-service';
import { resetAllStores } from '../../../testing/helpers';
import type { TaskEntity, TaskView } from '../tasks.types';

function makePromotionTask(overrides: Partial<TaskView> = {}, dataOverrides: Partial<TaskEntity['data']> = {}): TaskView {
    const data = {
        promotionId: 'promo-1',
        apiName: 'Loyalty API',
        sourceEnvironmentName: 'Staging',
        targetEnvironmentName: 'Production',
        targetApiId: 'api-9',
        isApiUpdate: false,
        authorDisplayName: 'Ada Lovelace',
        ...dataOverrides,
    };
    return {
        id: 'task-promo-1',
        type: 'PROMOTION_APPROVAL',
        category: 'API_PROMOTION',
        categoryLabel: 'API Promotion',
        actionLabel: 'Review promotion',
        iconKey: 'promotion',
        area: { key: 'apim', label: 'API Management' },
        title: 'Loyalty API',
        subtitle: 'Staging → Production',
        createdAt: 0,
        to: '/environments/env-1/apim/apis/api-9',
        toModuleId: 'apim',
        entity: { type: 'PROMOTION_APPROVAL', created_at: 0, data },
        ...overrides,
    };
}

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

function renderRow(task: TaskView, onProcessPromotion?: (promotionId: string, accepted: boolean) => Promise<void>) {
    return render(
        <MemoryRouter initialEntries={['/start']}>
            <TaskRow task={task} onProcessPromotion={onProcessPromotion} />
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

describe('TaskRow promotion review', () => {
    beforeEach(() => {
        resetAllStores();
        permissionService.load('environment', ['api-definition-u']);
    });

    it('opens the review dialog instead of navigating when a promotion task is clicked', () => {
        renderRow(makePromotionTask());

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));

        expect(screen.getByRole('dialog')).toBeTruthy();
        expect(screen.getByTestId('location').textContent).toBe('/start');
    });

    it('accepts a promotion and closes the dialog', async () => {
        const onProcessPromotion = jest.fn().mockResolvedValue(undefined);
        renderRow(makePromotionTask(), onProcessPromotion);

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));
        fireEvent.click(screen.getByRole('button', { name: /^Accept$/ }));

        await waitFor(() => expect(onProcessPromotion).toHaveBeenCalledWith('promo-1', true));
        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    });

    it('rejects a promotion and closes the dialog', async () => {
        const onProcessPromotion = jest.fn().mockResolvedValue(undefined);
        renderRow(makePromotionTask(), onProcessPromotion);

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));
        fireEvent.click(screen.getByRole('button', { name: /^Reject$/ }));

        await waitFor(() => expect(onProcessPromotion).toHaveBeenCalledWith('promo-1', false));
        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    });

    it('shows an error and keeps the task open when processing fails', async () => {
        const onProcessPromotion = jest.fn().mockRejectedValue(new Error('Target already has a newer promotion'));
        renderRow(makePromotionTask(), onProcessPromotion);

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));
        fireEvent.click(screen.getByRole('button', { name: /^Accept$/ }));

        expect(await screen.findByText('Target already has a newer promotion')).toBeTruthy();
        expect(screen.getByRole('dialog')).toBeTruthy();
    });

    it('hides Accept/Reject and shows a review-only notice when the viewer lacks accept/reject permission', () => {
        act(() => permissionService.reset());
        renderRow(makePromotionTask());

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));

        expect(screen.queryByRole('button', { name: /^Accept$/ })).toBeNull();
        expect(screen.queryByRole('button', { name: /^Reject$/ })).toBeNull();
        expect(screen.getByText(/do not have permission/i)).toBeTruthy();
    });
});
