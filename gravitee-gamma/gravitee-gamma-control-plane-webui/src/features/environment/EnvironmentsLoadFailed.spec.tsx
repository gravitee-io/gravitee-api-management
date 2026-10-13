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
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { useEnvironmentStore } from './environment.store';
import { EnvironmentsLoadFailed } from './EnvironmentsLoadFailed';
import { TEST_ENVIRONMENTS, TEST_MANAGEMENT_BASE } from '../../testing/factories';
import { trackHandler } from '../../testing/helpers';

function seedFailedLoad() {
    useEnvironmentStore.setState({
        organizationId: 'test-org',
        error: new Error('GET /environments failed'),
        loadFailed: true,
        initialized: true,
    });
}

describe('EnvironmentsLoadFailed', () => {
    beforeEach(seedFailedLoad);

    afterEach(() => {
        jest.useRealTimers();
    });

    it('should say that the environments could not be loaded, without the technical error', () => {
        render(<EnvironmentsLoadFailed />);

        const alert = screen.getByRole('alert');
        expect(alert.textContent).toContain("Couldn't load your environments");
        expect(alert.textContent).toContain('The platform may be updating. Try again in a minute.');
        expect(alert.textContent).not.toContain('GET /environments failed');
    });

    it('should title the page, as no page rendered its own heading', () => {
        render(<EnvironmentsLoadFailed />);

        expect(screen.getByRole('heading', { level: 1, name: "Couldn't load your environments" })).toBeTruthy();
    });

    it('should load the environments again on "Retry"', async () => {
        const user = userEvent.setup();
        const tracker = trackHandler('get', `${TEST_MANAGEMENT_BASE}/environments`, TEST_ENVIRONMENTS);
        render(<EnvironmentsLoadFailed />);

        await user.click(screen.getByRole('button', { name: 'Retry' }));

        await waitFor(() => expect(useEnvironmentStore.getState().environments).toEqual(TEST_ENVIRONMENTS));
        expect(tracker.callCount).toBe(1);
        expect(useEnvironmentStore.getState().loadFailed).toBe(false);
    });

    it('should keep "Retrying…" long enough to be read when the retry fails right away', () => {
        jest.useFakeTimers();
        render(<EnvironmentsLoadFailed />);
        const button = screen.getByRole('button', { name: 'Retry' });

        act(() => useEnvironmentStore.setState({ loading: true }));
        act(() => jest.advanceTimersByTime(25));
        act(() => useEnvironmentStore.setState({ loading: false }));
        act(() => jest.advanceTimersByTime(574));
        expect(button.getAttribute('aria-disabled')).toBe('true');
        expect(button.textContent).toBe('Retrying…');

        act(() => jest.advanceTimersByTime(1));
        expect(button.getAttribute('aria-disabled')).toBe('false');
        expect(button.textContent).toBe('Retry');
    });

    it('should not start another retry while one is running', async () => {
        const user = userEvent.setup();
        const tracker = trackHandler('get', `${TEST_MANAGEMENT_BASE}/environments`, TEST_ENVIRONMENTS);
        render(<EnvironmentsLoadFailed />);
        act(() => useEnvironmentStore.setState({ loading: true }));

        await user.click(screen.getByRole('button', { name: 'Retrying…' }));

        expect(tracker.callCount).toBe(0);
    });
});
