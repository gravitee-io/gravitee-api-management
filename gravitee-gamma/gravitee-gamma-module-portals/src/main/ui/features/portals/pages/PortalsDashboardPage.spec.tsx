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
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';

import { PortalsDashboardPage } from './PortalsDashboardPage';
import { createPortal, createPortalsResponse, createPortalsStats, TEST_CONFIG, TEST_V2_BASE } from '../../../testing/factories';
import { createQueryClientWrapper } from '../../../testing/helpers';
import { server } from '../../../testing/server';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
    useHasPermission: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockUseHasPermission = jest.mocked(useHasPermission);

function renderDashboard(federated = false) {
    const { wrapper, queryClient } = createQueryClientWrapper();
    const dashboard = () => (
        <MemoryRouter basename={federated ? '/console' : '/'} initialEntries={[federated ? '/console/environments/dev/portals' : '/']}>
            <Routes>
                <Route
                    path={federated ? '/environments/:envHrid/portals/:portalId/overview' : '/:portalId/overview'}
                    element={<h1>Portal overview destination</h1>}
                />
                <Route path={federated ? '/environments/:envHrid/portals/*' : '/*'} element={<PortalsDashboardPage />} />
            </Routes>
        </MemoryRouter>
    );
    const result = render(dashboard(), { wrapper });
    return { ...result, queryClient, rerenderDashboard: () => result.rerender(dashboard()) };
}

function portalsResponse(count: number) {
    return createPortalsResponse({
        data: Array.from({ length: count }, (_, index) =>
            createPortal({ id: `portal-${index}`, name: `Portal ${index + 1}`, default: index === 0, publishedApisCount: 7 }),
        ),
        pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: count, totalCount: count },
    });
}

describe('PortalsDashboardPage', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: TEST_CONFIG.environmentId, organizationId: TEST_CONFIG.organizationId });
        mockUseHasPermission.mockReturnValue(true);
    });

    it('keeps search, view selection and portal actions reachable in toolbar order', async () => {
        const user = userEvent.setup();
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, () =>
                HttpResponse.json(
                    createPortalsResponse({
                        data: [createPortal({ url: 'https://portal.example.com' })],
                    }),
                ),
            ),
        );
        renderDashboard();
        await screen.findByText('Developer Portal');

        const controls = screen.getByRole('group', { name: 'Portal controls' });
        expect(within(controls).getByRole('textbox', { name: 'Search portals' })).toBeVisible();
        await user.tab();
        expect(screen.getByRole('textbox', { name: 'Search portals' })).toHaveFocus();
        await user.tab();
        expect(screen.getByRole('radio', { name: 'Tiles' })).toHaveFocus();
        await user.tab();
        expect(screen.getByRole('button', { name: 'New Portal' })).toHaveFocus();
        await user.keyboard('{Enter} ');
        expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
        await user.tab();
        expect(screen.getByRole('link', { name: 'View Portal: Developer Portal' })).toHaveFocus();
        await user.tab();
        expect(screen.getByRole('link', { name: 'Configure Developer Portal' })).toHaveFocus();
        await user.keyboard('{Enter}');
        expect(await screen.findByRole('heading', { name: 'Portal overview destination' })).toBeVisible();
    });

    it('applies the three/five preview limits to server search results while preserving environment totals', async () => {
        const user = userEvent.setup();
        const statsRequests = jest.fn();
        const listRequests: URL[] = [];
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, ({ request }) => {
                const url = new URL(request.url);
                listRequests.push(url);
                const response = portalsResponse(6);
                return HttpResponse.json({
                    ...response,
                    data: response.data.map(portal => ({ ...portal, name: `${url.searchParams.get('q') ?? 'All'} ${portal.name}` })),
                });
            }),
            http.get(`${TEST_V2_BASE}/portals/_stats`, () => {
                statsRequests();
                return HttpResponse.json(createPortalsStats({ quota: { used: 12, limit: 20 }, publishedApisCount: 23 }));
            }),
        );
        renderDashboard();
        await screen.findByText('All Portal 1');
        await user.type(screen.getByRole('textbox', { name: 'Search portals' }), 'Partner');
        await screen.findByText('Partner Portal 1');

        expect(screen.getAllByRole('article')).toHaveLength(3);
        expect(screen.queryByText('Partner Portal 4')).not.toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'View all Portals' })).toBeVisible();
        const requestsBeforeToggle = listRequests.length;
        await user.click(screen.getByRole('radio', { name: 'Table' }));
        expect(within(screen.getByRole('table')).getAllByRole('row')).toHaveLength(6);
        expect(screen.queryByText('Partner Portal 6')).not.toBeInTheDocument();
        expect(listRequests).toHaveLength(requestsBeforeToggle);
        expect(listRequests.at(-1)?.searchParams.get('q')).toBe('Partner');
        expect(statsRequests).toHaveBeenCalledTimes(1);
        expect(screen.getByRole('region', { name: 'Portal quota' })).toHaveTextContent('12 / 20');
        expect(screen.getByRole('region', { name: 'Published APIs' })).toHaveTextContent('23');
    });

    it('keeps the latest search visible when an earlier response arrives late', async () => {
        const user = userEvent.setup();
        let releaseEarlier: () => void = () => undefined;
        const earlierStarted = jest.fn();
        const earlierResponse = new Promise<void>(resolve => {
            releaseEarlier = resolve;
        });
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, async ({ request }) => {
                const query = new URL(request.url).searchParams.get('q');
                if (query === 'Earlier') {
                    earlierStarted();
                    await earlierResponse;
                }
                return HttpResponse.json(
                    createPortalsResponse({ data: [createPortal({ name: query ? `${query} portal` : 'Initial portal' })] }),
                );
            }),
        );
        const { queryClient } = renderDashboard();
        await screen.findByText('Initial portal');
        const search = screen.getByRole('textbox', { name: 'Search portals' });
        try {
            await user.type(search, 'Earlier');
            await waitFor(() => expect(earlierStarted).toHaveBeenCalledTimes(1));
            await user.clear(search);
            await user.type(search, 'Latest');
            await screen.findByText('Latest portal');
        } finally {
            await act(async () => {
                releaseEarlier();
                await earlierResponse;
            });
        }
        await waitFor(() => expect(queryClient.isFetching()).toBe(0));
        expect(screen.getByText('Latest portal')).toBeVisible();
        expect(screen.queryByText('Earlier portal')).not.toBeInTheDocument();
    });

    it('uses environment statistics independently from the displayed portals and shows only three tiles', async () => {
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, () => HttpResponse.json(portalsResponse(4))),
            http.get(`${TEST_V2_BASE}/portals/_stats`, () =>
                HttpResponse.json(createPortalsStats({ quota: { used: 4, limit: 10 }, publishedApisCount: 19 })),
            ),
        );

        renderDashboard();

        expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
        expect(await screen.findByText('Portal 1')).toBeInTheDocument();
        expect(screen.getByText('Portal 2')).toBeInTheDocument();
        expect(screen.getByText('Portal 3')).toBeInTheDocument();
        expect(screen.queryByText('Portal 4')).not.toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'View all Portals' })).toHaveAttribute('href', '/list');
        await waitFor(() => expect(within(screen.getByRole('region', { name: 'Published APIs' })).getByText('19')).toBeInTheDocument());
        expect(screen.getByRole('region', { name: 'Portal quota' })).toHaveTextContent('4 / 10');
    });

    it('switches between three tiles and five rows without fetching again or reordering the portals', async () => {
        const user = userEvent.setup();
        const requests: URL[] = [];
        const response = portalsResponse(6);
        const orderedPortals = [0, 4, 2, 1, 3, 5].map(index => response.data[index]);
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, ({ request }) => {
                requests.push(new URL(request.url));
                return HttpResponse.json({ ...response, data: orderedPortals });
            }),
        );
        renderDashboard();
        await screen.findByText('Portal 1');

        await user.click(screen.getByRole('radio', { name: 'Table' }));

        const table = screen.getByRole('table');
        expect(within(table).getAllByRole('row')).toHaveLength(6);
        expect(
            within(table)
                .getAllByText(/^Portal \d$/)
                .map(element => element.textContent),
        ).toEqual(['Portal 1', 'Portal 5', 'Portal 3', 'Portal 2', 'Portal 4']);
        expect(screen.queryByText('Portal 6')).not.toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'View all Portals' })).toBeInTheDocument();

        await user.click(screen.getByRole('radio', { name: 'Tiles' }));

        expect(screen.queryByRole('table')).not.toBeInTheDocument();
        expect(screen.queryByText('Portal 4')).not.toBeInTheDocument();
        expect(requests).toHaveLength(1);
        expect(requests[0].searchParams.get('page')).toBe('1');
        expect(requests[0].searchParams.get('perPage')).toBe('10');
    });

    it.each([
        [1, false, false],
        [3, false, false],
        [4, true, false],
        [5, true, false],
        [6, true, true],
    ])('shows View all Portals only when %s results exceed the current view limit', async (count, tilesLink, tableLink) => {
        const user = userEvent.setup();
        server.use(http.get(`${TEST_V2_BASE}/portals`, () => HttpResponse.json(portalsResponse(count))));
        renderDashboard();
        await screen.findByText('Portal 1');

        expect(Boolean(screen.queryByRole('link', { name: 'View all Portals' }))).toBe(tilesLink);
        await user.click(screen.getByRole('radio', { name: 'Table' }));
        expect(Boolean(screen.queryByRole('link', { name: 'View all Portals' }))).toBe(tableLink);
    });

    it.each([undefined, -1])('uses the loaded results when totalCount is unknown (%s)', async totalCount => {
        server.use(http.get(`${TEST_V2_BASE}/portals`, () => HttpResponse.json({ ...portalsResponse(4), pagination: { totalCount } })));
        renderDashboard();

        expect(await screen.findByText('Portal 1')).toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'View all Portals' })).toBeInTheDocument();
    });

    it('starts a new dashboard visit in tile view without persisting the previous selection', async () => {
        const user = userEvent.setup();
        const { unmount } = renderDashboard();
        await screen.findByText('Developer Portal');
        await user.click(screen.getByRole('radio', { name: 'Table' }));
        expect(screen.getByRole('table')).toBeInTheDocument();
        unmount();

        renderDashboard();

        await screen.findByText('Developer Portal');
        expect(screen.getByRole('radio', { name: 'Tiles' })).toBeChecked();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });

    it('debounces server-side search for 300 ms, trims its value, and does not refetch environment statistics', async () => {
        const requests: URL[] = [];
        const statsRequests = jest.fn();
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, ({ request }) => {
                const url = new URL(request.url);
                requests.push(url);
                return HttpResponse.json(
                    createPortalsResponse({
                        data: [createPortal({ name: url.searchParams.get('q') ? 'Matching portal' : 'All portals' })],
                    }),
                );
            }),
            http.get(`${TEST_V2_BASE}/portals/_stats`, () => {
                statsRequests();
                return HttpResponse.json(createPortalsStats({ publishedApisCount: 23 }));
            }),
        );
        renderDashboard();
        await screen.findByText('All portals');
        await waitFor(() => expect(within(screen.getByRole('region', { name: 'Published APIs' })).getByText('23')).toBeInTheDocument());

        jest.useFakeTimers();
        try {
            const search = screen.getByRole('textbox', { name: 'Search portals' });
            fireEvent.change(search, { target: { value: '  partner  ' } });
            await act(() => jest.advanceTimersByTimeAsync(299));
            expect(requests).toHaveLength(1);
            await act(() => jest.advanceTimersByTimeAsync(1));
            await screen.findByText('Matching portal');

            expect(requests).toHaveLength(2);
            expect(requests[1].searchParams.get('q')).toBe('partner');
            expect(requests[1].searchParams.get('page')).toBe('1');
            expect(requests[1].searchParams.get('perPage')).toBe('10');
            expect(statsRequests).toHaveBeenCalledTimes(1);
            expect(screen.getByRole('region', { name: 'Published APIs' })).toHaveTextContent('23');

            fireEvent.change(search, { target: { value: '   ' } });
            await act(() => jest.advanceTimersByTimeAsync(300));
            await screen.findByText('All portals');
            await waitFor(() => expect(requests).toHaveLength(3));
            expect(requests[2].searchParams.has('q')).toBe(false);
            expect(statsRequests).toHaveBeenCalledTimes(1);
        } finally {
            jest.useRealTimers();
        }
    });

    it('distinguishes an empty portal collection from an empty search result', async () => {
        const user = userEvent.setup();
        server.use(http.get(`${TEST_V2_BASE}/portals`, () => HttpResponse.json(portalsResponse(0))));
        renderDashboard();

        expect(await screen.findByText('No portals yet')).toBeInTheDocument();
        expect(screen.queryByRole('link', { name: 'View all Portals' })).not.toBeInTheDocument();

        await user.type(screen.getByRole('textbox', { name: 'Search portals' }), 'missing');

        expect(await screen.findByText('No portals found')).toBeInTheDocument();
        expect(screen.queryByText('No portals yet')).not.toBeInTheDocument();
    });

    it('does not display a zero or an empty-state message while initial requests are loading', async () => {
        let release: () => void = () => undefined;
        const pending = new Promise<void>(resolve => {
            release = resolve;
        });
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, async () => {
                await pending;
                return HttpResponse.json(createPortalsResponse());
            }),
            http.get(`${TEST_V2_BASE}/portals/_stats`, async () => {
                await pending;
                return HttpResponse.json(createPortalsStats());
            }),
        );
        renderDashboard();

        try {
            expect(screen.queryByText('No portals yet')).not.toBeInTheDocument();
            expect(within(screen.getByRole('region', { name: 'Published APIs' })).queryByText('0')).not.toBeInTheDocument();
            expect(screen.queryByText('Developer Portal')).not.toBeInTheDocument();
        } finally {
            release();
        }
        expect(await screen.findByText('Developer Portal')).toBeInTheDocument();
    });

    it('distinguishes denied access from an empty result and a retryable technical error', async () => {
        server.use(http.get(`${TEST_V2_BASE}/portals`, () => HttpResponse.json({ message: 'Forbidden' }, { status: 403 })));
        renderDashboard();

        expect(await screen.findByText('Access denied')).toBeInTheDocument();
        expect(screen.getByRole('alert')).toHaveTextContent('You do not have permission to view portals in this environment.');
        expect(screen.queryByText('No portals yet')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Retry' })).not.toBeInTheDocument();
    });

    it('does not expose a partial dashboard when statistics access is denied', async () => {
        server.use(http.get(`${TEST_V2_BASE}/portals/_stats`, () => HttpResponse.json({ message: 'Forbidden' }, { status: 403 })));
        renderDashboard();

        expect(await screen.findByText('Access denied')).toBeInTheDocument();
        expect(screen.queryByText('Developer Portal')).not.toBeInTheDocument();
        expect(screen.queryByRole('region', { name: 'Published APIs' })).not.toBeInTheDocument();
        expect(screen.queryByRole('link', { name: /configure/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Retry' })).not.toBeInTheDocument();
    });

    it('retries a failed portal request and renders the recovered response', async () => {
        const user = userEvent.setup();
        server.use(http.get(`${TEST_V2_BASE}/portals`, () => HttpResponse.json({ message: 'Server error' }, { status: 500 })));
        renderDashboard();
        expect(await screen.findByText(/unable to load portals/i)).toBeInTheDocument();
        expect(screen.queryByText('No portals yet')).not.toBeInTheDocument();

        server.use(http.get(`${TEST_V2_BASE}/portals`, () => HttpResponse.json(createPortalsResponse())));
        await user.click(screen.getByRole('button', { name: 'Retry' }));

        expect(await screen.findByText('Developer Portal')).toBeInTheDocument();
        expect(screen.queryByText(/unable to load portals/i)).not.toBeInTheDocument();
    });

    it('keeps portals usable when statistics fail without fabricating zero counts', async () => {
        const user = userEvent.setup();
        server.use(http.get(`${TEST_V2_BASE}/portals/_stats`, () => HttpResponse.json({ message: 'Server error' }, { status: 500 })));
        renderDashboard();

        expect(await screen.findByText('Developer Portal')).toBeInTheDocument();
        expect(await screen.findByText(/unable to load portal statistics/i)).toBeInTheDocument();
        expect(screen.queryByText('0 / 0')).not.toBeInTheDocument();
        expect(screen.queryByText('0')).not.toBeInTheDocument();
        server.use(http.get(`${TEST_V2_BASE}/portals/_stats`, () => HttpResponse.json(createPortalsStats({ publishedApisCount: 23 }))));
        await user.click(screen.getByRole('button', { name: 'Retry' }));

        await waitFor(() => expect(within(screen.getByRole('region', { name: 'Published APIs' })).getByText('23')).toBeInTheDocument());
    });

    it('hides Configure without documentation update permission in both views', async () => {
        const user = userEvent.setup();
        mockUseHasPermission.mockImplementation(({ anyOf }) => !anyOf?.includes('environment-documentation-u'));
        renderDashboard();
        await screen.findByText('Developer Portal');

        expect(screen.queryByRole('link', { name: /configure/i })).not.toBeInTheDocument();
        await user.click(screen.getByRole('radio', { name: 'Table' }));
        expect(screen.queryByRole('link', { name: /configure/i })).not.toBeInTheDocument();
    });

    it('explains the disabled New Portal action to keyboard users', async () => {
        renderDashboard();
        await screen.findByText('Developer Portal');

        act(() => screen.getByRole('button', { name: 'New Portal' }).focus());

        expect(screen.getByRole('button', { name: 'New Portal' })).toHaveFocus();
        expect(screen.getByRole('button', { name: 'New Portal' })).toHaveAccessibleDescription('Creating portals is coming soon.');
        expect(await screen.findByRole('tooltip')).toHaveTextContent('Creating portals is coming soon.');
        expect(screen.getByRole('button', { name: 'New Portal' })).toHaveAttribute('aria-disabled', 'true');
    });

    it.each([false, true])('keeps dashboard links inside the module and router basename (federated=%s)', async federated => {
        server.use(http.get(`${TEST_V2_BASE}/portals`, () => HttpResponse.json(portalsResponse(4))));
        renderDashboard(federated);
        await screen.findByText('Portal 1');
        const moduleRoot = federated ? '/console/environments/dev/portals' : '';

        expect(screen.getByRole('link', { name: 'View all Portals' })).toHaveAttribute('href', `${moduleRoot}/list`);
        expect(screen.getAllByRole('link', { name: /configure/i })[0]).toHaveAttribute('href', `${moduleRoot}/portal-0/overview`);
        expect(screen.getByRole('button', { name: 'New Portal' })).toHaveAttribute('aria-disabled', 'true');
    });

    it('clears previous environment data immediately when the environment changes', async () => {
        let release: () => void = () => undefined;
        const pending = new Promise<void>(resolve => {
            release = resolve;
        });
        server.use(
            http.get(`${TEST_CONFIG.managementBaseURL}/v2/environments/second-env/portals`, async () => {
                await pending;
                return HttpResponse.json(createPortalsResponse({ data: [createPortal({ name: 'Second environment portal' })] }));
            }),
            http.get(`${TEST_CONFIG.managementBaseURL}/v2/environments/second-env/portals/_stats`, async () => {
                await pending;
                return HttpResponse.json(createPortalsStats({ publishedApisCount: 23 }));
            }),
        );
        const { rerenderDashboard } = renderDashboard();
        await screen.findByText('Developer Portal');
        await waitFor(() => expect(screen.getByRole('region', { name: 'Published APIs' })).toHaveTextContent('12'));

        mockUseEnvironment.mockReturnValue({ id: 'second-env', organizationId: TEST_CONFIG.organizationId });
        rerenderDashboard();

        try {
            expect(screen.queryByText('Developer Portal')).not.toBeInTheDocument();
            expect(screen.getByRole('region', { name: 'Published APIs' })).not.toHaveTextContent('12');
        } finally {
            release();
        }
        expect(await screen.findByText('Second environment portal')).toBeInTheDocument();
        await waitFor(() => expect(screen.getByRole('region', { name: 'Published APIs' })).toHaveTextContent('23'));
    });

    it('switches between portal views with the keyboard', async () => {
        const user = userEvent.setup();
        renderDashboard();
        await screen.findByText('Developer Portal');

        act(() => screen.getByRole('radio', { name: 'Tiles' }).focus());
        await user.keyboard('{ArrowRight} ');

        expect(screen.getByRole('radio', { name: 'Table' })).toHaveFocus();
        expect(screen.getByRole('table')).toBeInTheDocument();
        await user.keyboard('{ArrowLeft} ');

        expect(screen.getByRole('radio', { name: 'Tiles' })).toHaveFocus();
        expect(screen.getByRole('radio', { name: 'Tiles' })).toBeChecked();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });
});
