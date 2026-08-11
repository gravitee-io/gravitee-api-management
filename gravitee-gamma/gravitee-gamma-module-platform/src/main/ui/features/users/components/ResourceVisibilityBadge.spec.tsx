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
jest.mock('@gravitee/graphene-core/icons', () => ({
    GlobeIcon: () => <svg data-testid="globe-icon" aria-hidden />,
    LockIcon: () => <svg data-testid="lock-icon" aria-hidden />,
}));

import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen } from '@testing-library/react';

import { ResourceVisibilityBadge } from './ResourceVisibilityBadge';

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
});

describe('ResourceVisibilityBadge', () => {
    it('renders a globe icon for public visibility', () => {
        renderWithGraphene(<ResourceVisibilityBadge visibility="PUBLIC" />);

        expect(screen.getByText('Public')).toBeTruthy();
        expect(screen.getByTestId('globe-icon')).toBeTruthy();
        expect(screen.queryByTestId('lock-icon')).toBeNull();
    });

    it('renders a lock icon for private visibility', () => {
        renderWithGraphene(<ResourceVisibilityBadge visibility="PRIVATE" />);

        expect(screen.getByText('Private')).toBeTruthy();
        expect(screen.getByTestId('lock-icon')).toBeTruthy();
        expect(screen.queryByTestId('globe-icon')).toBeNull();
    });

    it('renders an em dash when visibility is missing', () => {
        renderWithGraphene(<ResourceVisibilityBadge />);

        expect(screen.getByText('—')).toBeTruthy();
        expect(screen.queryByTestId('globe-icon')).toBeNull();
        expect(screen.queryByTestId('lock-icon')).toBeNull();
    });
});
