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
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';

import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';

import { PortalsListPage } from './PortalsListPage';
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

function LocationProbe() {
    return <output aria-label="Current path">{useLocation().pathname}</output>;
}

function renderList(federated = false) {
    const { wrapper, queryClient } = createQueryClientWrapper();
    const root = federated ? '/environments/dev/portals' : '';
    const app = () => (
        <MemoryRouter basename={federated ? '/console' : '/'} initialEntries={[`${federated ? '/console' : ''}${root}/list`]}>
            <Routes>
                <Route path={`${root}/list`} element={<PortalsListPage />} />
                <Route path={`${root}/:portalId/overview`} element={<h1>Overview destination</h1>} />
            </Routes>
            <LocationProbe />
        </MemoryRouter>
    );
    const result = render(app(), { wrapper });
    return { ...result, queryClient, rerenderList: () => result.rerender(app()) };
}

function mockPagedResponse() {
    const requests: URL[] = [];
    const statsRequests = jest.fn();
    server.use(
        http.get(`${TEST_V2_BASE}/portals`, ({ request }) => {
            const url = new URL(request.url);
            requests.push(url);
            const page = Number(url.searchParams.get('page'));
            const query = url.searchParams.get('q');
            return HttpResponse.json(
                createPortalsResponse({
                    data: [createPortal({ name: `${query || 'All'} page ${page}` })],
                    pagination: { page, perPage: 10, pageCount: query ? 2 : 3, totalCount: query ? 11 : 25 },
                }),
            );
        }),
        http.get(`${TEST_V2_BASE}/portals/_stats`, () => {
            statsRequests();
            return HttpResponse.json(createPortalsStats({ quota: { used: 25, limit: 30 } }));
        }),
    );
    return { requests, statsRequests };
}

describe('PortalsListPage', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: TEST_CONFIG.environmentId, organizationId: TEST_CONFIG.organizationId });
        mockUseHasPermission.mockReturnValue(true);
    });

    it('renders the whole server page in API order with quota and no dashboard preview limit', async () => {
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, () =>
                HttpResponse.json(
                    createPortalsResponse({
                        data: Array.from({ length: 10 }, (_, i) =>
                            createPortal({
                                id: `p-${i}`,
                                name: i === 0 ? 'Zulu' : `Alpha ${i}`,
                                default: i === 1,
                                url: i === 0 ? 'https://z.example.com' : null,
                                publishedApisCount: i + 20,
                            }),
                        ),
                        pagination: { page: 1, perPage: 10, pageCount: 2, totalCount: 12 },
                    }),
                ),
            ),
        );
        renderList();
        expect(await screen.findByText('Zulu')).toBeVisible();
        const rows = within(screen.getByRole('table')).getAllByRole('row');
        expect(rows).toHaveLength(11);
        expect(rows[1]).toHaveTextContent('Zulu');
        expect(rows[2]).toHaveTextContent('Alpha 1');
        expect(rows[2]).toHaveTextContent('Default');
        expect(rows[10]).toHaveTextContent('29');
        expect(screen.getByRole('columnheader', { name: 'URL' })).toBeVisible();
        expect(screen.getByText('https://z.example.com')).toBeVisible();
        expect(screen.getAllByText('No public URL yet')).toHaveLength(9);
        expect(screen.getByText('Z')).toBeVisible();
        expect(screen.getByRole('button', { name: 'New Portal' })).toHaveAttribute('aria-disabled', 'true');
        expect(screen.queryByRole('link', { name: /configure/i })).not.toBeInTheDocument();
        expect(screen.getByText('Page 1 of 2')).toBeVisible();
    });

    it('debounces query changes atomically with page 1, retains q on paging, and never refetches quota for filters', async () => {
        const user = userEvent.setup();
        const { requests, statsRequests } = mockPagedResponse();
        renderList();
        await screen.findByText('All page 1');
        await user.click(screen.getByRole('button', { name: 'Next page' }));
        await screen.findByText('All page 2');
        await user.click(screen.getByRole('button', { name: 'Next page' }));
        await screen.findByText('All page 3');
        jest.useFakeTimers();
        try {
            fireEvent.change(screen.getByRole('textbox', { name: 'Search portals' }), { target: { value: '  partner  ' } });
            await act(() => jest.advanceTimersByTimeAsync(299));
            expect(requests).toHaveLength(3);
            await act(() => jest.advanceTimersByTimeAsync(1));
            await screen.findByText('partner page 1');
            expect(requests).toHaveLength(4);
            expect(requests[3].searchParams.get('q')).toBe('partner');
            expect(requests[3].searchParams.get('page')).toBe('1');
            expect(requests[3].searchParams.get('perPage')).toBe('10');
            expect(screen.getByText('Page 1 of 2')).toBeVisible();
        } finally {
            jest.useRealTimers();
        }
        await user.click(screen.getByRole('button', { name: 'Next page' }));
        await screen.findByText('partner page 2');
        expect(requests[4].searchParams.get('q')).toBe('partner');
        expect(screen.getByRole('button', { name: 'Next page' })).toBeDisabled();
        await user.clear(screen.getByRole('textbox', { name: 'Search portals' }));
        await screen.findByText('All page 1');
        expect(requests.at(-1)?.searchParams.has('q')).toBe(false);
        expect(statsRequests).toHaveBeenCalledTimes(1);
        expect(screen.getByRole('region', { name: 'Portal quota' })).toHaveTextContent('25 / 30');
    });

    it.each(['', 'partner'])('keeps the table, pager and focus while fetching the next page with query "%s"', async query => {
        const user = userEvent.setup();
        const { statsRequests } = mockPagedResponse();
        renderList();
        await screen.findByText('All page 1');
        if (query) {
            await user.type(screen.getByRole('textbox', { name: 'Search portals' }), query);
            await screen.findByText(`${query} page 1`);
        }
        const table = screen.getByRole('table');
        const pager = screen.getByRole('navigation', { name: 'Portal pagination' });
        const next = screen.getByRole('button', { name: 'Next page' });
        let release: () => void = () => undefined;
        const pending = new Promise<void>(resolve => {
            release = resolve;
        });
        const requests: URL[] = [];
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, async ({ request }) => {
                requests.push(new URL(request.url));
                await pending;
                return HttpResponse.json(
                    createPortalsResponse({
                        data: [createPortal({ name: 'Next page portal' })],
                        pagination: { page: 2, perPage: 10, pageCount: 3, totalCount: 25 },
                    }),
                );
            }),
        );
        try {
            await user.click(next);
            await waitFor(() => expect(requests).toHaveLength(1));
            expect(screen.getByRole('table')).toBe(table);
            expect(screen.getByText(`${query || 'All'} page 1`)).toBeVisible();
            expect(screen.getByRole('navigation', { name: 'Portal pagination' })).toBe(pager);
            expect(within(pager).getByRole('status')).toHaveTextContent('Loading portals…');
            expect(next).toHaveFocus();
            expect(next).toBeDisabled();
            expect(screen.getByRole('button', { name: 'Previous page' })).toBeDisabled();
            expect(table.closest('[aria-busy]')).toHaveAttribute('aria-busy', 'true');
            expect(screen.queryByRole('status', { name: 'Loading portals' })).not.toBeInTheDocument();
            await user.click(next);
            expect(requests).toHaveLength(1);
            expect(requests[0].searchParams.get('page')).toBe('2');
            expect(requests[0].searchParams.get('q')).toBe(query || null);
        } finally {
            release();
        }
        expect(await screen.findByText('Next page portal')).toBeVisible();
        expect(screen.queryByText(`${query || 'All'} page 1`)).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Previous page' })).toBeEnabled();
        expect(screen.getByRole('button', { name: 'Next page' })).toBeEnabled();
        expect(next).toHaveFocus();
        expect(screen.getByText('Page 2 of 3')).toBeVisible();
        expect(statsRequests).toHaveBeenCalledTimes(1);
    });

    it('retains the previous table while a search resets the page, then shows the matching empty state', async () => {
        const user = userEvent.setup();
        const { statsRequests } = mockPagedResponse();
        renderList();
        await screen.findByText('All page 1');
        await user.click(screen.getByRole('button', { name: 'Next page' }));
        await screen.findByText('All page 2');
        let release: () => void = () => undefined;
        const pending = new Promise<void>(resolve => {
            release = resolve;
        });
        const requests: URL[] = [];
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, async ({ request }) => {
                requests.push(new URL(request.url));
                await pending;
                return HttpResponse.json(createPortalsResponse({ data: [], pagination: { page: 1, totalCount: 0 } }));
            }),
        );
        try {
            await user.type(screen.getByRole('textbox', { name: 'Search portals' }), 'missing');
            await waitFor(() => expect(requests).toHaveLength(1));
            expect(screen.getByText('All page 2')).toBeVisible();
            expect(screen.getByRole('table')).toBeVisible();
            expect(screen.getByRole('button', { name: 'Previous page' })).toBeDisabled();
            expect(screen.getByRole('button', { name: 'Next page' })).toBeDisabled();
            expect(screen.getByRole('textbox', { name: 'Search portals' })).toHaveFocus();
            expect(screen.queryByText('No portals found')).not.toBeInTheDocument();
            expect(requests[0].searchParams.get('page')).toBe('1');
            expect(requests[0].searchParams.get('q')).toBe('missing');
        } finally {
            release();
        }
        expect(await screen.findByText('No portals found')).toBeVisible();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
        expect(screen.queryByText('No portals on this page')).not.toBeInTheDocument();
        expect(statsRequests).toHaveBeenCalledTimes(1);
    });

    it('uses links for paging with an unknown total and never follows their URL directly', async () => {
        const requests: URL[] = [];
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, ({ request }) => {
                const url = new URL(request.url);
                requests.push(url);
                const page = Number(url.searchParams.get('page'));
                return HttpResponse.json(
                    createPortalsResponse({
                        data: [createPortal({ name: `Unknown page ${page}` })],
                        pagination: { totalCount: -1 },
                        links: page === 1 ? { next: 'https://untrusted.example/other' } : {},
                    }),
                );
            }),
        );
        const user = userEvent.setup();
        renderList();
        await screen.findByText('Unknown page 1');
        expect(screen.getByText('Page 1')).toBeVisible();
        await user.click(screen.getByRole('button', { name: 'Next page' }));
        await screen.findByText('Unknown page 2');
        expect(screen.getByRole('button', { name: 'Next page' })).toBeDisabled();
        expect(screen.getByRole('button', { name: 'Previous page' })).toBeEnabled();
        expect(requests[1].pathname).toBe('/management/v2/environments/DEFAULT/portals');
    });

    it.each([false, true])('opens a focused row on Enter and preserves the module basename (federated=%s)', async federated => {
        const user = userEvent.setup();
        renderList(federated);
        const row = (await screen.findByText('Developer Portal')).closest('tr');
        expect(row).not.toBeNull();
        act(() => row?.focus());
        await user.keyboard('{Enter}');
        expect(screen.getByRole('heading', { name: 'Overview destination' })).toBeVisible();
        expect(screen.getByLabelText('Current path')).toHaveTextContent(
            `${federated ? '/environments/dev/portals' : ''}/00f8c9e7-78fc-4907-b8c9-e778fc790750/overview`,
        );
    });

    it('keeps rows passive without documentation update permission while preserving View Portal', async () => {
        mockUseHasPermission.mockImplementation(({ anyOf }) => !anyOf?.includes('environment-documentation-u'));
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, () =>
                HttpResponse.json(createPortalsResponse({ data: [createPortal({ url: 'https://portal.example.com' })] })),
            ),
        );
        const user = userEvent.setup();
        renderList();
        const name = await screen.findByText('Developer Portal');
        const row = name.closest('tr');
        expect(row).not.toHaveAttribute('tabindex');
        expect(row).not.toHaveAttribute('aria-label', 'Open portal Developer Portal');
        await user.click(name);
        fireEvent.keyDown(name, { key: 'Enter' });
        expect(screen.getByLabelText('Current path')).toHaveTextContent('/list');
        const link = screen.getByRole('link', { name: 'View Portal: Developer Portal' });
        expect(link).toHaveAttribute('href', 'https://portal.example.com');
        await user.click(link);
        act(() => link.focus());
        await user.keyboard('{Enter}');
        expect(screen.getByLabelText('Current path')).toHaveTextContent('/list');
        expect(screen.queryByRole('link', { name: /configure/i })).not.toBeInTheDocument();
    });

    it('keeps external and unavailable View Portal actions separate from row navigation', async () => {
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, () =>
                HttpResponse.json(
                    createPortalsResponse({
                        data: [
                            createPortal({ id: 'external', name: 'External', url: 'https://portal.example.com' }),
                            createPortal({ id: 'missing', name: 'Missing', url: null }),
                        ],
                    }),
                ),
            ),
        );
        const user = userEvent.setup();
        renderList();
        const link = await screen.findByRole('link', { name: 'View Portal: External' });
        expect(link).toHaveAttribute('target', '_blank');
        expect(link).toHaveAttribute('rel', 'noopener noreferrer');
        await user.click(link);
        expect(screen.getByLabelText('Current path')).toHaveTextContent('/list');
        act(() => link.focus());
        await user.keyboard('{Enter}');
        expect(screen.getByLabelText('Current path')).toHaveTextContent('/list');
        const unavailable = screen.getByRole('button', { name: 'View Portal: Missing' });
        expect(unavailable).toHaveAccessibleDescription('No portal URL is configured.');
        await user.click(unavailable);
        await user.keyboard('{Enter} ');
        expect(screen.getByLabelText('Current path')).toHaveTextContent('/list');
        await user.click(screen.getByText('External'));
        expect(screen.getByLabelText('Current path')).toHaveTextContent('/external/overview');
    });

    it('keeps the latest search when an earlier response arrives late', async () => {
        let release: () => void = () => undefined;
        const pending = new Promise<void>(resolve => {
            release = resolve;
        });
        const requestedQueries: string[] = [];
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, async ({ request }) => {
                const q = new URL(request.url).searchParams.get('q') || '';
                requestedQueries.push(q);
                if (q === 'old') await pending;
                return HttpResponse.json(createPortalsResponse({ data: [createPortal({ name: q || 'Initial' })] }));
            }),
        );
        renderList();
        await screen.findByText('Initial');
        const search = screen.getByRole('textbox', { name: 'Search portals' });
        try {
            fireEvent.change(search, { target: { value: 'old' } });
            await waitFor(() => expect(requestedQueries).toContain('old'));
            fireEvent.change(search, { target: { value: 'latest' } });
            await screen.findByText('latest');
        } finally {
            await act(async () => release());
        }
        expect(screen.getByText('latest')).toBeVisible();
        expect(screen.queryByText('old')).not.toBeInTheDocument();
    });

    it('keeps row and external action reachable as separate tab stops and leaves New Portal inert', async () => {
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, () =>
                HttpResponse.json(createPortalsResponse({ data: [createPortal({ url: 'https://portal.example.com' })] })),
            ),
        );
        const user = userEvent.setup();
        renderList();
        const row = (await screen.findByText('Developer Portal')).closest('tr');
        act(() => row?.focus());
        await user.tab();
        expect(screen.getByRole('link', { name: 'View Portal: Developer Portal' })).toHaveFocus();
        const create = screen.getByRole('button', { name: 'New Portal' });
        act(() => create.focus());
        await user.keyboard('{Enter} ');
        expect(create).toHaveFocus();
        expect(create).toHaveAccessibleDescription('Creating portals is coming soon.');
        expect(screen.getByLabelText('Current path')).toHaveTextContent('/list');
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('distinguishes initial loading, an empty collection and an empty search result', async () => {
        let release: () => void = () => undefined;
        const pending = new Promise<void>(resolve => {
            release = resolve;
        });
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, async () => {
                await pending;
                return HttpResponse.json(createPortalsResponse({ data: [], pagination: { totalCount: 0 } }));
            }),
        );
        const user = userEvent.setup();
        renderList();
        try {
            expect(screen.getByRole('status', { name: 'Loading portals' })).toBeVisible();
            expect(screen.queryByText('No portals yet')).not.toBeInTheDocument();
        } finally {
            release();
        }
        expect(await screen.findByText('No portals yet')).toBeVisible();
        await user.type(screen.getByRole('textbox', { name: 'Search portals' }), 'missing');
        expect(await screen.findByText('No portals found')).toBeVisible();
    });

    it.each(['', 'partner'])('distinguishes an empty later page after results shrink and allows returning with query "%s"', async query => {
        const user = userEvent.setup();
        mockPagedResponse();
        const { queryClient } = renderList();
        await screen.findByText('All page 1');
        if (query) {
            await user.type(screen.getByRole('textbox', { name: 'Search portals' }), query);
            await screen.findByText(`${query} page 1`);
        }
        await user.click(screen.getByRole('button', { name: 'Next page' }));
        await screen.findByText(`${query || 'All'} page 2`);

        const requests: URL[] = [];
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, ({ request }) => {
                const url = new URL(request.url);
                requests.push(url);
                const page = Number(url.searchParams.get('page'));
                return HttpResponse.json(
                    createPortalsResponse({
                        data: page === 1 ? [createPortal({ name: 'Remaining portal' })] : [],
                        pagination: { page, perPage: 10, pageCount: 1, totalCount: 1 },
                    }),
                );
            }),
        );
        await act(async () => {
            await queryClient.invalidateQueries();
        });

        expect(await screen.findByText('No portals on this page')).toBeVisible();
        expect(screen.getByText('Go back to a previous page.')).toBeVisible();
        expect(screen.queryByText('No portals yet')).not.toBeInTheDocument();
        expect(screen.queryByText('No portals found')).not.toBeInTheDocument();
        expect(screen.queryByText('There are no portals in this environment.')).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Previous page' })).toBeEnabled();

        await user.click(screen.getByRole('button', { name: 'Previous page' }));
        expect(await screen.findByText('Remaining portal')).toBeVisible();
        expect(requests.at(-1)?.searchParams.get('page')).toBe('1');
        expect(requests.at(-1)?.searchParams.get('q')).toBe(query || null);
        expect(screen.getByRole('textbox', { name: 'Search portals' })).toHaveValue(query);
    });

    it('does not reuse a previous empty-state message while a different search is pending', async () => {
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, () =>
                HttpResponse.json(createPortalsResponse({ data: [], pagination: { page: 1, totalCount: 0 } })),
            ),
        );
        renderList();
        await screen.findByText('No portals yet');
        let release: () => void = () => undefined;
        const pending = new Promise<void>(resolve => {
            release = resolve;
        });
        const requested = jest.fn();
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, async () => {
                requested();
                await pending;
                return HttpResponse.json(createPortalsResponse({ data: [createPortal({ name: 'Matching portal' })] }));
            }),
        );
        try {
            fireEvent.change(screen.getByRole('textbox', { name: 'Search portals' }), { target: { value: 'matching' } });
            await waitFor(() => expect(requested).toHaveBeenCalledTimes(1));
            expect(screen.getByRole('status', { name: 'Loading portals' })).toBeVisible();
            expect(screen.queryByText('No portals found')).not.toBeInTheDocument();
            expect(screen.queryByText('No portals yet')).not.toBeInTheDocument();
        } finally {
            release();
        }
        expect(await screen.findByText('Matching portal')).toBeVisible();
    });

    it.each([403, 500])('discards retained rows when the next page fails with %s', async status => {
        const user = userEvent.setup();
        mockPagedResponse();
        renderList();
        await screen.findByText('All page 1');
        let release: () => void = () => undefined;
        const pending = new Promise<void>(resolve => {
            release = resolve;
        });
        const requested = jest.fn();
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, async () => {
                requested();
                await pending;
                return HttpResponse.json({}, { status });
            }),
        );
        try {
            await user.click(screen.getByRole('button', { name: 'Next page' }));
            await waitFor(() => expect(requested).toHaveBeenCalledTimes(1));
            expect(screen.getByText('All page 1')).toBeVisible();
        } finally {
            release();
        }
        expect(await screen.findByText(status === 403 ? 'Access denied' : 'Unable to load portals')).toBeVisible();
        expect(screen.queryByText('All page 1')).not.toBeInTheDocument();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
        expect(screen.queryByRole('navigation', { name: 'Portal pagination' })).not.toBeInTheDocument();
    });

    it.each(['portals', 'portals/_stats'])('hides partial data when %s returns 403', async endpoint => {
        server.use(http.get(`${TEST_V2_BASE}/${endpoint}`, () => HttpResponse.json({}, { status: 403 })));
        renderList();
        expect(await screen.findByText('Access denied')).toBeVisible();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
        expect(screen.queryByText('No portals yet')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Retry' })).not.toBeInTheDocument();
    });

    it('retries the failed page without discarding its search or page number', async () => {
        const user = userEvent.setup();
        mockPagedResponse();
        renderList();
        await screen.findByText('All page 1');
        await user.type(screen.getByRole('textbox', { name: 'Search portals' }), 'partner');
        await screen.findByText('partner page 1');
        server.use(http.get(`${TEST_V2_BASE}/portals`, () => HttpResponse.json({}, { status: 500 })));
        await user.click(screen.getByRole('button', { name: 'Next page' }));
        expect(await screen.findByText('Unable to load portals')).toBeVisible();
        const requests: URL[] = [];
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, ({ request }) => {
                requests.push(new URL(request.url));
                return HttpResponse.json(
                    createPortalsResponse({ data: [createPortal({ name: 'Recovered' })], pagination: { page: 2, pageCount: 2 } }),
                );
            }),
        );
        await user.click(screen.getByRole('button', { name: 'Retry' }));
        await screen.findByText('Recovered');
        expect(requests[0].searchParams.get('q')).toBe('partner');
        expect(requests[0].searchParams.get('page')).toBe('2');
    });

    it('keeps the list when stats fail and restores quota with a separate Retry', async () => {
        const user = userEvent.setup();
        server.use(http.get(`${TEST_V2_BASE}/portals/_stats`, () => HttpResponse.json({}, { status: 500 })));
        renderList();
        await screen.findByText('Developer Portal');
        expect(await screen.findByText('Unable to load portal statistics')).toBeVisible();
        expect(screen.queryByText('0 / 0')).not.toBeInTheDocument();
        server.use(
            http.get(`${TEST_V2_BASE}/portals/_stats`, () => HttpResponse.json(createPortalsStats({ quota: { used: 12, limit: 20 } }))),
        );
        await user.click(screen.getByRole('button', { name: 'Retry' }));
        await waitFor(() => expect(screen.getByRole('region', { name: 'Portal quota' })).toHaveTextContent('12 / 20'));
    });

    it('resets page and search and hides old results when the environment changes', async () => {
        const user = userEvent.setup();
        mockPagedResponse();
        const { rerenderList } = renderList();
        await screen.findByText('All page 1');
        await user.click(screen.getByRole('button', { name: 'Next page' }));
        await screen.findByText('All page 2');
        const requests: URL[] = [];
        server.use(
            http.get(`${TEST_CONFIG.managementBaseURL}/v2/environments/second/portals`, ({ request }) => {
                requests.push(new URL(request.url));
                return HttpResponse.json(createPortalsResponse({ data: [createPortal({ name: 'Second environment' })] }));
            }),
            http.get(`${TEST_CONFIG.managementBaseURL}/v2/environments/second/portals/_stats`, () =>
                HttpResponse.json(createPortalsStats()),
            ),
        );
        mockUseEnvironment.mockReturnValue({ id: 'second', organizationId: TEST_CONFIG.organizationId });
        rerenderList();
        expect(screen.queryByText('All page 2')).not.toBeInTheDocument();
        await screen.findByText('Second environment');
        expect(requests[0].searchParams.get('page')).toBe('1');
        expect(requests[0].searchParams.has('q')).toBe(false);
    });
});
