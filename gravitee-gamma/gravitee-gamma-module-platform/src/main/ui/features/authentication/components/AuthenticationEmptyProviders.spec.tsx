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

import { buttonHarness, renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen } from '@testing-library/react';

import { AuthenticationEmptyProviders } from './AuthenticationEmptyProviders';

beforeAll(() => {
    Object.defineProperty(window, 'matchMedia', {
        writable: true,
        value: jest.fn().mockImplementation((query: string) => ({
            matches: false,
            media: query,
            onchange: null,
            addListener: jest.fn(),
            removeListener: jest.fn(),
            addEventListener: jest.fn(),
            removeEventListener: jest.fn(),
            dispatchEvent: jest.fn(),
        })),
    });
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    } as typeof ResizeObserver;
});

describe('AuthenticationEmptyProviders', () => {
    it('uses the first-use empty state with educational content', () => {
        renderWithGraphene(<AuthenticationEmptyProviders />);
        expect(screen.getByText('No identity providers yet')).not.toBeNull();
        expect(screen.getByText('Without a provider')).not.toBeNull();
        expect(screen.getByText('With a provider')).not.toBeNull();
        expect(screen.getByText('Choose a provider')).not.toBeNull();
        expect(screen.getByText('Connect it')).not.toBeNull();
        expect(screen.getByText('Map access')).not.toBeNull();
        expect(screen.queryByRole('button', { name: /Add an identity provider/i })).toBeNull();
    });

    it('owns the create action when the user can create', async () => {
        const onAdd = jest.fn();
        renderWithGraphene(<AuthenticationEmptyProviders canCreate onAdd={onAdd} />);
        await buttonHarness({ name: /Add an identity provider/i }).click();
        expect(onAdd).toHaveBeenCalledTimes(1);
    });

    it('renders through DataTableEmptyState instead of a nested bordered panel', () => {
        renderWithGraphene(<AuthenticationEmptyProviders />);
        expect(screen.getByText('No identity providers yet').closest('div.space-y-6')).toBeNull();
        expect(screen.getByText('Without a provider').closest('[data-slot="card"]')).not.toBeNull();
        expect(screen.getByText('With a provider').closest('[data-slot="card"]')).not.toBeNull();
    });
});
