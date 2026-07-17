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
import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';

import { GraviteeDocsRenderer } from './GraviteeDocsRenderer';

const nbaSpec = readFileSync(
    join(__dirname, '../../../dummy-spec/api-sports-nba-v2.openapi.json'),
    'utf-8',
);

describe('GraviteeDocsRenderer', () => {
    beforeAll(() => {
        class MockIntersectionObserver implements IntersectionObserver {
            readonly root: Element | Document | null = null;
            readonly rootMargin = '';
            readonly thresholds: readonly number[] = [];

            observe(): void {}
            unobserve(): void {}
            disconnect(): void {}
            takeRecords(): IntersectionObserverEntry[] {
                return [];
            }
        }

        Object.defineProperty(window, 'IntersectionObserver', {
            writable: true,
            configurable: true,
            value: MockIntersectionObserver,
        });

        class MockResizeObserver {
            observe(): void {}
            unobserve(): void {}
            disconnect(): void {}
        }

        Object.defineProperty(window, 'ResizeObserver', {
            writable: true,
            configurable: true,
            value: MockResizeObserver,
        });
    });

    it('should render API title and operations sidebar', () => {
        renderWithGraphene(<GraviteeDocsRenderer specContent={nbaSpec} />);

        expect(screen.getByTestId('gravitee-docs-renderer')).toBeInTheDocument();
        expect(screen.getByText('API-NBA')).toBeInTheDocument();
        expect(screen.getByText('2.2.5')).toBeInTheDocument();
        expect(screen.getAllByText('List seasons').length).toBeGreaterThan(0);
    });

    it('should render all operations on one scrollable page', () => {
        renderWithGraphene(<GraviteeDocsRenderer specContent={nbaSpec} />);

        expect(
            screen.getByRole('heading', { level: 2, name: 'API status and account information' }),
        ).toBeInTheDocument();
        expect(screen.getByRole('heading', { level: 2, name: 'List seasons' })).toBeInTheDocument();
        expect(screen.getByRole('heading', { level: 2, name: 'List games' })).toBeInTheDocument();
    });

    it('should highlight sidebar item when navigating to an operation', async () => {
        const user = userEvent.setup();
        renderWithGraphene(<GraviteeDocsRenderer specContent={nbaSpec} />);

        const seasonsButton = screen.getByRole('button', { name: /List seasons/i });
        await user.click(seasonsButton);

        expect(seasonsButton).toHaveAttribute('aria-current', 'true');
        expect(
            within(screen.getByTestId('gravitee-docs-renderer')).getByText('/seasons'),
        ).toBeInTheDocument();
    });

    it('should show error message for invalid spec', () => {
        renderWithGraphene(<GraviteeDocsRenderer specContent="{" />);

        expect(screen.getByText('Unable to render spec with Gravitee Docs.')).toBeInTheDocument();
    });
});
