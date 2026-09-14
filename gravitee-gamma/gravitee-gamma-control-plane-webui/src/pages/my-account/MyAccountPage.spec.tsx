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
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { MyAccountPage } from './MyAccountPage';
import { useAuthStore } from '../../features/auth/auth.store';
import { TEST_MANAGEMENT_BASE, buildUser } from '../../testing/factories';
import { respondWith, seedBootstrap, seedEnvironments, seedUser, trackHandler } from '../../testing/helpers';
import { server } from '../../testing/server';

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/environments/env-1/my-account']}>
            <Routes>
                <Route path="/environments/:envHrid/my-account" element={<MyAccountPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

function textInput(label: string | RegExp): HTMLInputElement {
    return screen.getByLabelText(label) as HTMLInputElement;
}

async function loadedPage() {
    return screen.findByRole('heading', { name: 'User information' });
}

describe('MyAccountPage', () => {
    beforeAll(() => {
        // jsdom cannot load management avatar URLs; keep the preview so Use default stays available.
        Object.defineProperty(HTMLImageElement.prototype, 'src', {
            configurable: true,
            set(this: HTMLImageElement, value: string) {
                this.setAttribute('src', String(value));
            },
            get(this: HTMLImageElement) {
                return this.getAttribute('src') ?? '';
            },
        });
        // Radix's Select drives pointer capture and scrolling, neither of which jsdom implements.
        Element.prototype.hasPointerCapture = jest.fn();
        Element.prototype.setPointerCapture = jest.fn();
        Element.prototype.releasePointerCapture = jest.fn();
        Element.prototype.scrollIntoView = jest.fn();
    });

    beforeEach(() => {
        seedBootstrap();
        seedEnvironments();
        seedUser();
        Object.defineProperty(navigator, 'clipboard', {
            configurable: true,
            value: { writeText: jest.fn().mockResolvedValue(undefined) },
        });
    });

    it('should show profile fields, roles, groups, and avatar for an internal user', async () => {
        respondWith(
            'get',
            `${TEST_MANAGEMENT_BASE}/user`,
            buildUser({
                roles: [{ scope: 'ORGANIZATION', name: 'ADMIN' }],
                groupsByEnvironment: { 'env-1-id': ['api-devs'] },
            }),
        );
        respondWith('get', `${TEST_MANAGEMENT_BASE}/configuration/custom-user-fields`, [{ key: 'team', label: 'Team', required: false }]);

        renderPage();
        await loadedPage();

        expect(textInput('First name').value).toBe('Test');
        expect(textInput('Last name').value).toBe('User');
        expect(textInput('Email').value).toBe('test@gravitee.io');
        expect(screen.getByText('[ORGANIZATION] ADMIN')).toBeTruthy();
        expect(screen.getByText('api-devs')).toBeTruthy();
        expect(textInput('Team')).toBeTruthy();
        expect(screen.getByRole('img', { name: 'Avatar' }).getAttribute('src')).toContain(
            '/organizations/test-org/user/avatar?user-1&cacheBust=',
        );
    });

    it('should keep first name, last name, and email read-only for IdP users', async () => {
        respondWith('get', `${TEST_MANAGEMENT_BASE}/user`, buildUser({ source: 'google' }));
        renderPage();
        await loadedPage();

        expect(textInput('First name').disabled).toBe(true);
        expect(textInput('Last name').disabled).toBe(true);
        expect(textInput('Email').disabled).toBe(true);
    });

    it('should keep Update disabled while a required custom-field dropdown is empty', async () => {
        const user = userEvent.setup();
        respondWith('get', `${TEST_MANAGEMENT_BASE}/configuration/custom-user-fields`, [
            { key: 'team', label: 'Team', values: ['Platform', 'Payments'], required: true },
        ]);
        respondWith('get', `${TEST_MANAGEMENT_BASE}/user`, buildUser({ customFields: {} }));
        renderPage();
        await loadedPage();

        const firstName = textInput('First name');
        await user.clear(firstName);
        await user.type(firstName, 'Ada');

        expect((screen.getByRole('button', { name: 'Update' }) as HTMLButtonElement).disabled).toBe(true);
        expect(screen.getByText('This field is required.')).toBeTruthy();

        await user.click(screen.getByRole('combobox', { name: /Team/ }));
        await user.click(await screen.findByRole('option', { name: 'Platform' }));

        expect((screen.getByRole('button', { name: 'Update' }) as HTMLButtonElement).disabled).toBe(false);
    });

    it('should save with PUT /user, round-trip custom fields, restore cancel, and bump avatar cacheBust', async () => {
        const user = userEvent.setup();
        respondWith('get', `${TEST_MANAGEMENT_BASE}/configuration/custom-user-fields`, [{ key: 'team', label: 'Team', required: false }]);
        respondWith('get', `${TEST_MANAGEMENT_BASE}/user`, buildUser({ customFields: { team: 'platform' } }));
        const put = trackHandler('put', `${TEST_MANAGEMENT_BASE}/user`, buildUser({ firstname: 'Ada', customFields: { team: 'edge' } }));
        renderPage();
        await loadedPage();

        const firstName = textInput('First name');
        const team = textInput('Team');
        expect(team.value).toBe('platform');
        await user.clear(firstName);
        await user.type(firstName, 'Ada');
        await user.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(firstName.value).toBe('Test');
        expect(team.value).toBe('platform');

        await user.clear(firstName);
        await user.type(firstName, 'Ada');
        await user.clear(team);
        await user.type(team, 'edge');
        expect(useAuthStore.getState().avatarCacheBust).toBe(0);
        await user.click(screen.getByRole('button', { name: 'Update' }));

        await waitFor(() => expect(put.callCount).toBe(1));
        expect(put.lastCall?.body).toEqual(
            expect.objectContaining({
                firstname: 'Ada',
                lastname: 'User',
                email: 'test@gravitee.io',
                customFields: { team: 'edge' },
            }),
        );
        expect(put.lastCall?.body).not.toHaveProperty('picture');
        await waitFor(() => expect(useAuthStore.getState().avatarCacheBust).toBeGreaterThan(0));
        expect(screen.getByRole('img', { name: 'Avatar' }).getAttribute('src')).toContain(
            `cacheBust=${useAuthStore.getState().avatarCacheBust}`,
        );
    });

    it('should keep the saved profile when PUT succeeds but the follow-up GET /user fails', async () => {
        const user = userEvent.setup();
        let userGets = 0;
        server.use(
            http.get(`${TEST_MANAGEMENT_BASE}/user`, () => {
                userGets += 1;
                if (userGets === 1) {
                    return HttpResponse.json(buildUser());
                }
                return HttpResponse.json({ message: 'unavailable' }, { status: 500 });
            }),
        );
        const put = trackHandler('put', `${TEST_MANAGEMENT_BASE}/user`, buildUser({ firstname: 'Ada' }));
        renderPage();
        await loadedPage();

        const firstName = textInput('First name');
        await user.clear(firstName);
        await user.type(firstName, 'Ada');
        await user.click(screen.getByRole('button', { name: 'Update' }));

        await waitFor(() => expect(put.callCount).toBe(1));
        await waitFor(() => expect(firstName.value).toBe('Ada'));
        expect(screen.queryByText('Failed to update user')).toBeNull();
        expect((screen.getByRole('button', { name: 'Update' }) as HTMLButtonElement).disabled).toBe(true);
        expect(useAuthStore.getState().avatarCacheBust).toBeGreaterThan(0);
    });

    it('should keep primary-owner delete lock and groups when PUT returns UserEntity and GET fails', async () => {
        const user = userEvent.setup();
        let userGets = 0;
        server.use(
            http.get(`${TEST_MANAGEMENT_BASE}/user`, () => {
                userGets += 1;
                if (userGets === 1) {
                    return HttpResponse.json(
                        buildUser({
                            primaryOwner: true,
                            groupsByEnvironment: { 'env-1-id': ['api-devs'] },
                        }),
                    );
                }
                return HttpResponse.json({ message: 'unavailable' }, { status: 500 });
            }),
        );
        trackHandler('put', `${TEST_MANAGEMENT_BASE}/user`, {
            id: 'user-1',
            firstname: 'Ada',
            lastname: 'User',
            email: 'test@gravitee.io',
            primary_owner: true,
        });
        renderPage();
        await loadedPage();

        expect(screen.getByText('api-devs')).toBeTruthy();
        const firstName = textInput('First name');
        await user.clear(firstName);
        await user.type(firstName, 'Ada');
        await user.click(screen.getByRole('button', { name: 'Update' }));

        await waitFor(() => expect(firstName.value).toBe('Ada'));
        expect(screen.getByText('api-devs')).toBeTruthy();
        expect((screen.getByRole('button', { name: 'Delete my account' }) as HTMLButtonElement).disabled).toBe(true);
    });

    it('should send an empty picture when Use default is saved', async () => {
        const user = userEvent.setup();
        const put = trackHandler('put', `${TEST_MANAGEMENT_BASE}/user`, buildUser());
        renderPage();

        await screen.findByRole('img', { name: 'Avatar' });
        await user.click(screen.getByRole('button', { name: 'Use default' }));
        await user.click(screen.getByRole('button', { name: 'Update' }));

        await waitFor(() => expect(put.lastCall?.body).toEqual(expect.objectContaining({ picture: '' })));
    });

    it('should generate a token once with a curl example and fail duplicate names inline', async () => {
        const user = userEvent.setup();
        renderPage();
        await user.click(await screen.findByRole('button', { name: 'Generate token' }));
        await user.type(screen.getByLabelText(/Name/), 'ci');
        await user.click(screen.getByRole('button', { name: 'Generate' }));

        expect(await screen.findByText('generated-token')).toBeTruthy();
        expect(
            screen.getByText(
                'curl -H "Authorization: Bearer generated-token" "http://api.test/management/organizations/test-org/environments/env-1-id"',
            ),
        ).toBeTruthy();

        await user.click(screen.getByRole('button', { name: 'Close' }));

        server.use(
            http.post(`${TEST_MANAGEMENT_BASE}/user/tokens`, () =>
                HttpResponse.json(
                    { message: 'A token with the name ci already exists', technicalCode: 'token.alreadyExists' },
                    { status: 400 },
                ),
            ),
        );

        await user.click(screen.getByRole('button', { name: 'Generate token' }));
        await user.type(screen.getByLabelText(/Name/), 'ci');
        await user.click(screen.getByRole('button', { name: 'Generate' }));

        expect(await screen.findByText('A token with the name ci already exists')).toBeTruthy();
        expect(screen.queryByText('generated-token')).toBeNull();
    });

    it('should revoke a token after confirmation', async () => {
        const user = userEvent.setup();
        respondWith('get', `${TEST_MANAGEMENT_BASE}/user/tokens`, [
            { id: 'tok-1', name: 'ci', created_at: 1_700_000_000_000, last_use_at: null },
        ]);
        const revoked = trackHandler('delete', `${TEST_MANAGEMENT_BASE}/user/tokens/tok-1`, undefined, 204);

        renderPage();
        await screen.findByText('ci');
        await user.click(screen.getByRole('button', { name: 'Revoke token ci' }));
        await user.click(screen.getByRole('button', { name: 'Revoke' }));

        await waitFor(() => expect(revoked.callCount).toBe(1));
    });

    it('should hide the danger zone when external auth is on and account deletion is off', async () => {
        respondWith('get', `${TEST_MANAGEMENT_BASE}/console`, {
            authentication: { externalAuth: { enabled: true }, externalAuthAccountDeletion: { enabled: false } },
        });
        renderPage();
        await loadedPage();

        expect(screen.queryByRole('heading', { name: 'Danger Zone' })).toBeNull();
        expect(screen.queryByRole('button', { name: 'Delete my account' })).toBeNull();
    });

    it('should still load the profile when custom-user-fields or console fail', async () => {
        const user = userEvent.setup();
        respondWith('get', `${TEST_MANAGEMENT_BASE}/configuration/custom-user-fields`, { message: 'unavailable' }, 500);
        respondWith('get', `${TEST_MANAGEMENT_BASE}/console`, { message: 'unavailable' }, 500);
        renderPage();
        await loadedPage();

        expect(textInput('First name').value).toBe('Test');
        expect(screen.queryByLabelText('Team')).toBeNull();
        expect(screen.queryByRole('heading', { name: 'Danger Zone' })).toBeNull();

        await user.clear(textInput('First name'));
        await user.type(textInput('First name'), 'Ada');
        expect((screen.getByRole('button', { name: 'Update' }) as HTMLButtonElement).disabled).toBe(false);
    });

    it('should show Retry instead of an empty token list when GET /user/tokens fails', async () => {
        respondWith('get', `${TEST_MANAGEMENT_BASE}/user/tokens`, { message: 'tokens unavailable' }, 500);
        renderPage();
        await loadedPage();

        expect(await screen.findByText('tokens unavailable')).toBeTruthy();
        expect(screen.queryByText('No personal access tokens')).toBeNull();
        expect(screen.getByRole('button', { name: 'Retry' })).toBeTruthy();
    });

    it('should disable delete while the user is a primary owner', async () => {
        respondWith('get', `${TEST_MANAGEMENT_BASE}/user`, buildUser({ primaryOwner: true }));
        renderPage();

        expect(((await screen.findByRole('button', { name: 'Delete my account' })) as HTMLButtonElement).disabled).toBe(true);
    });

    it('should require the display name then DELETE /user and log out', async () => {
        const user = userEvent.setup();
        const deleted = trackHandler('delete', `${TEST_MANAGEMENT_BASE}/user`, undefined, 204);
        const logout = trackHandler('post', `${TEST_MANAGEMENT_BASE}/user/logout`, null, 200);
        renderPage();

        await user.click(await screen.findByRole('button', { name: 'Delete my account' }));
        const dialog = screen.getByRole('dialog');
        expect((within(dialog).getByRole('button', { name: 'Yes, delete my account' }) as HTMLButtonElement).disabled).toBe(true);

        await user.type(within(dialog).getByRole('textbox'), 'Test User');
        await user.click(within(dialog).getByRole('button', { name: 'Yes, delete my account' }));

        await waitFor(() => expect(deleted.callCount).toBe(1));
        await waitFor(() => expect(logout.callCount).toBe(1));
        expect(useAuthStore.getState().user).toBeNull();
    });
});
