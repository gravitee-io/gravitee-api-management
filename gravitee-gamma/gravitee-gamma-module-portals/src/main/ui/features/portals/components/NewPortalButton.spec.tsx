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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';

import { NewPortalButton } from './NewPortalButton';
import { createPortal, TEST_CONFIG, TEST_V2_BASE } from '../../../testing/factories';
import { createQueryClientWrapper } from '../../../testing/helpers';
import { server } from '../../../testing/server';
import { PortalOverviewPlaceholderPage } from '../pages/PortalOverviewPlaceholderPage';
import { portalKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
    useHasPermission: jest.fn(),
}));
const environment = jest.mocked(useEnvironment);
const permission = jest.mocked(useHasPermission);

function Destination() {
    return (
        <>
            <h1>Created overview</h1>
            <PortalOverviewPlaceholderPage />
        </>
    );
}

function renderButton(quota = { used: 1, limit: 10 }, quotaError = false) {
    const { wrapper, queryClient } = createQueryClientWrapper();
    const view = render(
        <MemoryRouter>
            <Routes>
                <Route path="/" element={<NewPortalButton quota={quota} quotaLoading={false} quotaError={quotaError} />} />
                <Route path="/:portalId/overview" element={<Destination />} />
            </Routes>
        </MemoryRouter>,
        { wrapper },
    );
    return { ...view, queryClient };
}

beforeEach(() => {
    environment.mockReturnValue({ id: TEST_CONFIG.environmentId, organizationId: TEST_CONFIG.organizationId });
    permission.mockReturnValue(true);
});

it('hides creation without portal create permission', () => {
    permission.mockImplementation(({ anyOf }) => !anyOf?.includes('environment-portal-c'));
    renderButton();
    expect(screen.queryByRole('button', { name: 'New Portal' })).not.toBeInTheDocument();
});

it('explains an unavailable quota and prevents opening', async () => {
    const user = userEvent.setup();
    renderButton({ used: 1, limit: 10 }, true);
    const button = screen.getByRole('button', { name: 'New Portal' });
    expect(button).toHaveAttribute('aria-disabled', 'true');
    expect(button).toHaveAccessibleDescription('Portal quota is unavailable. Please try again.');
    await user.click(button);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});

it('validates, sends once, invalidates environment lists and quota and navigates to the returned id', async () => {
    const user = userEvent.setup();
    const post = jest.fn();
    server.use(
        http.post(`${TEST_V2_BASE}/portals`, async ({ request }) => {
            post(await request.json());
            return HttpResponse.json(createPortal({ id: 'new-id', name: 'Partners' }), { status: 201 });
        }),
    );
    const { queryClient } = renderButton();
    const list = portalKeys.list(TEST_CONFIG.environmentId, 'partners', 2, 10);
    const stats = portalKeys.stats(TEST_CONFIG.environmentId);
    const other = portalKeys.stats('OTHER');
    queryClient.setQueryData(list, {});
    queryClient.setQueryData(stats, {});
    queryClient.setQueryData(other, {});
    await user.click(screen.getByRole('button', { name: 'New Portal' }));
    expect(screen.getByRole('textbox', { name: 'Name' })).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Create portal' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Enter a portal name');
    await user.type(screen.getByRole('textbox', { name: 'Name' }), 'Partners{Enter}');
    expect(await screen.findByRole('heading', { name: 'Created overview' })).toBeVisible();
    expect(post).toHaveBeenCalledTimes(1);
    expect(post).toHaveBeenCalledWith({ name: 'Partners' });
    expect(queryClient.getQueryState(list)?.isInvalidated).toBe(true);
    expect(queryClient.getQueryState(stats)?.isInvalidated).toBe(true);
    expect(queryClient.getQueryState(other)?.isInvalidated).toBe(false);
});

it.each([
    [409, 'portal.name.exists', 'A portal with this name already exists.'],
    [403, 'portal.limitReached', "You've reached your portal limit. Contact support to add more portals."],
    [403, 'other', 'You do not have permission to create a portal.'],
    [
        400,
        'other',
        'Unable to create the portal. The environment may need a default portal with a valid URL, or its portals may be managed through the Automation API.',
    ],
    [500, 'other', 'Unable to create the portal. Check the portal list before trying again.'],
])('keeps the entered name on HTTP %s with code %s', async (status, technicalCode, message) => {
    const user = userEvent.setup();
    server.use(http.post(`${TEST_V2_BASE}/portals`, () => HttpResponse.json({ technicalCode }, { status })));
    renderButton();
    await user.click(screen.getByRole('button', { name: 'New Portal' }));
    await user.type(screen.getByRole('textbox', { name: 'Name' }), 'Partners{Enter}');
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent(message));
    expect(screen.getByRole('textbox', { name: 'Name' })).toHaveValue('Partners');
    expect(screen.getByRole('dialog')).toBeVisible();
});

it('prevents duplicate submissions and cancellation while the request is pending', async () => {
    const user = userEvent.setup();
    let finish!: () => void;
    const gate = new Promise<void>(resolve => {
        finish = resolve;
    });
    const post = jest.fn();
    server.use(
        http.post(`${TEST_V2_BASE}/portals`, async () => {
            post();
            await gate;
            return HttpResponse.json(createPortal({ id: 'new-id' }), { status: 201 });
        }),
    );
    renderButton();
    await user.click(screen.getByRole('button', { name: 'New Portal' }));
    await user.type(screen.getByRole('textbox', { name: 'Name' }), 'Partners{Enter}');
    await waitFor(() => expect(post).toHaveBeenCalledTimes(1));
    expect(screen.getByRole('button', { name: 'Creating…' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    await user.keyboard('{Escape}{Enter}');
    expect(screen.getByRole('dialog')).toBeVisible();
    expect(post).toHaveBeenCalledTimes(1);
    finish();
    expect(await screen.findByRole('heading', { name: 'Created overview' })).toBeVisible();
});

it('does not navigate after the environment changes while creation is pending', async () => {
    const user = userEvent.setup();
    let finish!: () => void;
    const gate = new Promise<void>(resolve => {
        finish = resolve;
    });
    const post = jest.fn();
    server.use(
        http.post(`${TEST_V2_BASE}/portals`, async () => {
            post();
            await gate;
            return HttpResponse.json(createPortal({ id: 'new-id' }), { status: 201 });
        }),
    );
    const { rerender } = renderButton();
    await user.click(screen.getByRole('button', { name: 'New Portal' }));
    await user.type(screen.getByRole('textbox', { name: 'Name' }), 'Partners{Enter}');
    await waitFor(() => expect(post).toHaveBeenCalledTimes(1));
    environment.mockReturnValue({ id: 'OTHER', organizationId: TEST_CONFIG.organizationId });
    rerender(
        <MemoryRouter>
            <Routes>
                <Route path="/" element={<NewPortalButton quota={{ used: 1, limit: 10 }} quotaLoading={false} quotaError={false} />} />
                <Route path="/:portalId/overview" element={<Destination />} />
            </Routes>
        </MemoryRouter>,
    );
    finish();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(screen.queryByRole('heading', { name: 'Created overview' })).not.toBeInTheDocument();
});

it('navigates after a successful POST even if refreshing fails without retrying creation', async () => {
    const user = userEvent.setup();
    const post = jest.fn();
    server.use(
        http.post(`${TEST_V2_BASE}/portals`, () => {
            post();
            return HttpResponse.json(createPortal({ id: 'new-id' }), { status: 201 });
        }),
    );
    const { queryClient } = renderButton();
    jest.spyOn(queryClient, 'invalidateQueries').mockRejectedValue(new Error('refresh failed'));
    await user.click(screen.getByRole('button', { name: 'New Portal' }));
    await user.type(screen.getByRole('textbox', { name: 'Name' }), 'Partners{Enter}');
    expect(await screen.findByText('Portal created, but the portal list or quota could not be refreshed.')).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Created overview' })).toBeVisible();
    expect(post).toHaveBeenCalledTimes(1);
});

it('navigates before the refresh finishes and reports a later refresh failure', async () => {
    const user = userEvent.setup();
    server.use(http.post(`${TEST_V2_BASE}/portals`, () => HttpResponse.json(createPortal({ id: 'new-id' }), { status: 201 })));
    const { queryClient } = renderButton();
    let rejectRefresh!: (error: Error) => void;
    const refresh = new Promise<void>((_resolve, reject) => {
        rejectRefresh = reject;
    });
    jest.spyOn(queryClient, 'invalidateQueries').mockReturnValue(refresh);
    await user.click(screen.getByRole('button', { name: 'New Portal' }));
    await user.type(screen.getByRole('textbox', { name: 'Name' }), 'Partners{Enter}');
    expect(await screen.findByRole('heading', { name: 'Created overview' })).toBeVisible();
    rejectRefresh(new Error('refresh failed'));
    expect(await screen.findByText('Portal created, but the portal list or quota could not be refreshed.')).toBeVisible();
});

it('keeps the submit action disabled after a server limit until quota is refreshed', async () => {
    const user = userEvent.setup();
    const post = jest.fn();
    server.use(
        http.post(`${TEST_V2_BASE}/portals`, () => {
            post();
            return HttpResponse.json({ technicalCode: 'portal.limitReached' }, { status: 403 });
        }),
    );
    const { rerender } = renderButton();
    await user.click(screen.getByRole('button', { name: 'New Portal' }));
    await user.type(screen.getByRole('textbox', { name: 'Name' }), 'Partners{Enter}');
    await waitFor(() => expect(screen.getByRole('button', { name: 'Create portal' })).toBeDisabled());
    await user.keyboard('{Enter}');
    expect(post).toHaveBeenCalledTimes(1);
    const tree = (loading: boolean, error: boolean) => (
        <MemoryRouter>
            <Routes>
                <Route path="/" element={<NewPortalButton quota={{ used: 1, limit: 10 }} quotaLoading={loading} quotaError={error} />} />
            </Routes>
        </MemoryRouter>
    );
    rerender(tree(true, false));
    rerender(tree(false, true));
    expect(screen.getByRole('button', { name: 'Create portal' })).toBeDisabled();
    rerender(tree(false, false));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Create portal' })).toBeEnabled());
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
});

it('explains quota checking and failure inside the open dialog', async () => {
    const user = userEvent.setup();
    const { rerender } = renderButton();
    await user.click(screen.getByRole('button', { name: 'New Portal' }));
    const tree = (loading: boolean, error: boolean) => (
        <MemoryRouter>
            <Routes>
                <Route path="/" element={<NewPortalButton quota={{ used: 1, limit: 10 }} quotaLoading={loading} quotaError={error} />} />
            </Routes>
        </MemoryRouter>
    );
    rerender(tree(true, false));
    expect(screen.getByRole('dialog')).toHaveTextContent('Checking portal quota');
    expect(screen.getByRole('button', { name: 'Create portal' })).toBeDisabled();
    rerender(tree(false, true));
    expect(screen.getByRole('dialog')).toHaveTextContent('Portal quota is unavailable');
    rerender(tree(false, false));
    expect(screen.getByRole('button', { name: 'Create portal' })).toBeEnabled();
});
