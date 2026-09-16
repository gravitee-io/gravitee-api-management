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
import { toast } from '@gravitee/graphene-core';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';

import { TaskRow } from './TaskRow';
import { useModulesStore } from '../../../features/modules';
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

// The sheet renders its own dismiss control with the same accessible name as the footer button, so a
// plain getByRole('button', { name: 'Close' }) is ambiguous. Either one closes the sheet.
function footerCloseButton(): HTMLButtonElement {
    const buttons = screen.getAllByRole('button', { name: /^Close$/ });
    return buttons[buttons.length - 1] as HTMLButtonElement;
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
        jest.spyOn(toast, 'success').mockImplementation(() => '');
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('opens the review sheet instead of navigating when a promotion task is clicked', () => {
        renderRow(makePromotionTask());

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));

        expect(screen.getByRole('dialog')).toBeTruthy();
        expect(screen.getByText('API promotion request')).toBeTruthy();
        expect(screen.getByTestId('location').textContent).toBe('/start');
    });

    it('shows Accept/Reject without any client-side permission gate — the backend enforces it', () => {
        renderRow(makePromotionTask());

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));

        expect(screen.getByRole('button', { name: /^Accept$/ })).toBeTruthy();
        expect(screen.getByRole('button', { name: /^Reject$/ })).toBeTruthy();
    });

    it('accepts a promotion, toasts success, and closes the sheet', async () => {
        const onProcessPromotion = jest.fn().mockResolvedValue(undefined);
        renderRow(makePromotionTask(), onProcessPromotion);

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));
        fireEvent.click(screen.getByRole('button', { name: /^Accept$/ }));

        await waitFor(() => expect(onProcessPromotion).toHaveBeenCalledWith('promo-1', true));
        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
        expect(toast.success).toHaveBeenCalledWith('API promotion accepted.');
    });

    it('requires confirmation before rejecting a promotion, then toasts success and closes the sheet', async () => {
        const onProcessPromotion = jest.fn().mockResolvedValue(undefined);
        renderRow(makePromotionTask(), onProcessPromotion);

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));
        fireEvent.click(screen.getByRole('button', { name: /^Reject$/ }));

        expect(onProcessPromotion).not.toHaveBeenCalled();
        expect(screen.getByText(/reject this promotion/i)).toBeTruthy();

        fireEvent.click(screen.getByRole('button', { name: /^Confirm reject$/ }));

        await waitFor(() => expect(onProcessPromotion).toHaveBeenCalledWith('promo-1', false));
        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
        expect(toast.success).toHaveBeenCalledWith('API promotion rejected.');
    });

    it('cancelling the reject confirmation does not process the promotion', () => {
        const onProcessPromotion = jest.fn().mockResolvedValue(undefined);
        renderRow(makePromotionTask(), onProcessPromotion);

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));
        fireEvent.click(screen.getByRole('button', { name: /^Reject$/ }));
        fireEvent.click(screen.getByRole('button', { name: /^Cancel$/ }));

        expect(onProcessPromotion).not.toHaveBeenCalled();
        expect(screen.getByRole('button', { name: /^Reject$/ })).toBeTruthy();
        expect(screen.queryByText(/Reject this promotion\?/)).toBeNull();
    });

    // Each of the four fields the sheet needs, and the guard that they are strings: drop any one check and a
    // review opens on a blank value — or Accept sends a promotion id that is not one.
    it.each([
        ['promotionId', { promotionId: '' }],
        ['apiName', { apiName: '' }],
        ['sourceEnvironmentName', { sourceEnvironmentName: '' }],
        ['targetEnvironmentName', { targetEnvironmentName: '' }],
        ['a promotionId that is not a string', { promotionId: 42 as unknown as string }],
    ])('refuses to review a promotion task missing %s', (_case, dataOverrides) => {
        const errorSpy = jest.spyOn(toast, 'error').mockImplementation(() => '');
        renderRow(makePromotionTask({}, dataOverrides));

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));

        expect(errorSpy).toHaveBeenCalledWith('This promotion task is missing required details.');
        expect(screen.queryByRole('dialog')).toBeNull();
        expect(screen.getByTestId('location').textContent).toBe('/start');
    });

    it('promises no navigation on a promotion row, whose button opens the review instead', () => {
        renderRow(makePromotionTask());

        expect(screen.queryByText(/^Opens /)).toBeNull();
    });

    it('names the requester and shows their email when the payload carries one', () => {
        renderRow(makePromotionTask({}, { authorEmail: 'ada@gv.io' }));

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));

        expect(screen.getByText('Ada Lovelace')).toBeTruthy();
        expect(screen.getByText(/\(ada@gv\.io\)/)).toBeTruthy();
    });

    it.each([
        ['absent', undefined],
        ['empty', ''],
    ])('leaves no empty parentheses when the requester email is %s', (_case, authorEmail) => {
        renderRow(makePromotionTask({}, { authorEmail }));

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));

        expect(screen.getByText(/requested the promotion of/).textContent).not.toMatch(/\(\s*\)/);
    });

    it('falls back to Unknown requester when the payload carries no author name', () => {
        renderRow(makePromotionTask({}, { authorDisplayName: undefined }));

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));

        expect(screen.getByText('Unknown requester')).toBeTruthy();
    });

    it('warns about sharding tags and explains a creation', () => {
        renderRow(makePromotionTask());

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));

        expect(screen.getByText('Sharding tags')).toBeTruthy();
        expect(screen.getByText(/will create Loyalty API as a new, stopped and private API in Production/)).toBeTruthy();
    });

    it('explains an update when the API has already been promoted to the target', () => {
        renderRow(makePromotionTask({}, { isApiUpdate: true }));

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));

        expect(screen.getByText(/has already been promoted to Production, accepting this promotion will update it/)).toBeTruthy();
    });

    it('labels the action, disables every button, and refuses to close while the request is in flight', async () => {
        let release: () => void = () => {};
        const onProcessPromotion = jest.fn().mockImplementation(() => new Promise<void>(resolve => (release = resolve)));
        renderRow(makePromotionTask(), onProcessPromotion);

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));
        fireEvent.click(screen.getByRole('button', { name: /^Accept$/ }));

        expect(screen.getByRole('button', { name: /^Accepting…$/ })).toBeTruthy();
        expect((screen.getByRole('button', { name: /^Reject$/ }) as HTMLButtonElement).disabled).toBe(true);

        // A request owns the sheet until it settles, because the sheet is where its outcome is reported.
        // Asserted as behaviour rather than on the disabled attribute, because the sheet renders its own
        // dismiss control next to the footer button and neither may close it here.
        screen.getAllByRole('button', { name: /^Close$/ }).forEach(button => fireEvent.click(button));
        expect(screen.getByRole('dialog')).toBeTruthy();
        fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' });
        expect(screen.getByRole('dialog')).toBeTruthy();

        release();
        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    });

    it('labels the reject in flight and holds the confirmation still while it runs', async () => {
        // The accept path is covered above; the reject path has its own label and its own two buttons, and a
        // second click on either while the first is in flight would process the promotion twice.
        let release: () => void = () => {};
        const onProcessPromotion = jest.fn().mockImplementation(() => new Promise<void>(resolve => (release = resolve)));
        renderRow(makePromotionTask(), onProcessPromotion);

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));
        fireEvent.click(screen.getByRole('button', { name: /^Reject$/ }));
        fireEvent.click(screen.getByRole('button', { name: /^Confirm reject$/ }));

        expect(screen.getByRole('button', { name: /^Rejecting…$/ })).toBeTruthy();
        expect((screen.getByRole('button', { name: /^Rejecting…$/ }) as HTMLButtonElement).disabled).toBe(true);
        expect((screen.getByRole('button', { name: /^Cancel$/ }) as HTMLButtonElement).disabled).toBe(true);

        release();
        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
        expect(onProcessPromotion).toHaveBeenCalledTimes(1);
    });

    it('returns to Reject when confirming the rejection fails', async () => {
        jest.spyOn(console, 'error').mockImplementation(() => {});
        jest.spyOn(toast, 'error').mockImplementation(() => '');
        const onProcessPromotion = jest.fn().mockRejectedValue(new Error('Promotion already processed'));
        renderRow(makePromotionTask(), onProcessPromotion);

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));
        fireEvent.click(screen.getByRole('button', { name: /^Reject$/ }));
        fireEvent.click(screen.getByRole('button', { name: /^Confirm reject$/ }));

        expect(await screen.findByText('Promotion already processed')).toBeTruthy();
        expect(toast.error).toHaveBeenCalledWith('Promotion already processed');
        // Back to the first step: the confirmation is spent, so a retry has to be asked for again.
        expect(screen.getByRole('button', { name: /^Reject$/ })).toBeTruthy();
        expect(screen.queryByRole('button', { name: /^Confirm reject$/ })).toBeNull();
    });

    it('reports a failure without a message through a generic one', async () => {
        jest.spyOn(console, 'error').mockImplementation(() => {});
        const errorToast = jest.spyOn(toast, 'error').mockImplementation(() => '');
        const onProcessPromotion = jest.fn().mockRejectedValue('not an Error');
        renderRow(makePromotionTask(), onProcessPromotion);

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));
        fireEvent.click(screen.getByRole('button', { name: /^Accept$/ }));

        expect(await screen.findByText('Failed to process the promotion.')).toBeTruthy();
        expect(errorToast).toHaveBeenCalledWith('Failed to process the promotion.');
    });

    it('carries no error or reject confirmation from a previous review into the next one', async () => {
        jest.spyOn(console, 'error').mockImplementation(() => {});
        jest.spyOn(toast, 'error').mockImplementation(() => '');
        const onProcessPromotion = jest.fn().mockRejectedValue(new Error('Target already has a newer promotion'));
        renderRow(makePromotionTask(), onProcessPromotion);

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));
        fireEvent.click(screen.getByRole('button', { name: /^Reject$/ }));
        fireEvent.click(screen.getByRole('button', { name: /^Confirm reject$/ }));
        expect(await screen.findByText('Target already has a newer promotion')).toBeTruthy();

        fireEvent.click(footerCloseButton());
        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));

        expect(screen.queryByText('Target already has a newer promotion')).toBeNull();
        expect(screen.queryByText(/reject this promotion/i)).toBeNull();
    });

    it('closes the sheet on Close without processing anything', async () => {
        const onProcessPromotion = jest.fn().mockResolvedValue(undefined);
        renderRow(makePromotionTask(), onProcessPromotion);

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));
        fireEvent.click(footerCloseButton());

        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
        expect(onProcessPromotion).not.toHaveBeenCalled();
    });

    it('offers Open API only when the owning module is registered, and closes the sheet to navigate', async () => {
        renderRow(makePromotionTask());
        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));
        expect(screen.queryByRole('button', { name: /^Open API$/ })).toBeNull();
        fireEvent.click(footerCloseButton());
        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());

        seedModule('apim');
        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));
        fireEvent.click(screen.getByRole('button', { name: /^Open API$/ }));

        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
        expect(screen.getByTestId('location').textContent).toBe('/environments/env-1/apim/apis/api-9');
    });

    it('shows an error and keeps the sheet open when processing fails', async () => {
        const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
        const failure = new Error('Target already has a newer promotion');
        const onProcessPromotion = jest.fn().mockRejectedValue(failure);
        renderRow(makePromotionTask(), onProcessPromotion);

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));
        fireEvent.click(screen.getByRole('button', { name: /^Accept$/ }));

        expect(await screen.findByText('Target already has a newer promotion')).toBeTruthy();
        expect(screen.getByRole('dialog')).toBeTruthy();
        expect(toast.success).not.toHaveBeenCalled();
        // The sheet closes and the message goes with it; the cause has to outlive it somewhere.
        expect(consoleSpy).toHaveBeenCalledWith('Failed to process the promotion', failure);
    });
});
