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
import { dataTableHarness } from '@gravitee/graphene-core/testing';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { IntegrationDiscoveryPreview } from './IntegrationDiscoveryPreview';
import type { IngestionScope, IntegrationPreview, IntegrationPreviewApi } from '../types/integration';
import { SMALLEST_TABLE_PAGE_SIZE } from '../utils/paginationConstants';

const ORDERS_API: IntegrationPreviewApi = { id: 'api-new-1', name: 'Orders', state: 'NEW' };
const PAYMENTS_API: IntegrationPreviewApi = { id: 'api-upd-1', name: 'Payments', state: 'UPDATE' };

function aPreview(overrides: Partial<IntegrationPreview> = {}): IntegrationPreview {
    return { isPartiallyDiscovered: false, totalCount: 2, newCount: 1, updateCount: 1, apis: [ORDERS_API, PAYMENTS_API], ...overrides };
}

function renderPreview(preview: IntegrationPreview, onProceed: (scope: IngestionScope) => void = jest.fn(), isProceeding = false) {
    return render(<IntegrationDiscoveryPreview preview={preview} onProceed={onProceed} isProceeding={isProceeding} />);
}

function switchNames(): string[] {
    return screen.queryAllByRole('switch').map(control => control.getAttribute('aria-label') ?? '');
}

function switchState(name: string): { checked: boolean; disabled: boolean } {
    const control = screen.getByRole('switch', { name });
    return { checked: control.getAttribute('aria-checked') === 'true', disabled: control.hasAttribute('disabled') };
}

function shownCounts(): Record<string, string | null> {
    const values = screen.getAllByRole('definition').map(definition => definition.textContent);
    return Object.fromEntries(screen.getAllByRole('term').map((term, index) => [term.textContent, values[index]]));
}

describe('IntegrationDiscoveryPreview', () => {
    beforeAll(() => {
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;

        Element.prototype.hasPointerCapture = jest.fn();
        Element.prototype.setPointerCapture = jest.fn();
        Element.prototype.releasePointerCapture = jest.fn();
        Element.prototype.scrollIntoView = jest.fn();
    });

    it.each([
        ['shows a', true],
        ['renders no', false],
    ])('%s partial discovery warning when isPartiallyDiscovered is %s', (_shown, isPartiallyDiscovered) => {
        renderPreview(aPreview({ isPartiallyDiscovered }));

        expect(screen.getByText('Discovered APIs')).toBeInTheDocument();
        expect(screen.queryByText('Partial API Discovery Warning') !== null).toBe(isPartiallyDiscovered);
    });

    // Two listed APIs against a total of 12 keeps the shown total from being derivable from the list length.
    it('shows the discovered, new and to-update API counts exactly as the preview counts them', () => {
        renderPreview(aPreview({ totalCount: 12, newCount: 8, updateCount: 4 }));

        expect(shownCounts()).toEqual({ 'Discovered APIs': '12', 'New APIs': '8', 'APIs to update': '4' });
    });

    it.each([
        [ORDERS_API.name, 'Create'],
        [PAYMENTS_API.name, 'Update'],
    ])('shows the %s row with a %s badge', (apiName, badge) => {
        renderPreview(aPreview());

        expect(dataTableHarness().getRow(new RegExp(apiName)).getCellText('Create or update')).toBe(badge);
    });

    it('shows No assets found when discovery finds no API', () => {
        renderPreview(aPreview({ totalCount: 0, newCount: 0, updateCount: 0, apis: [] }));

        expect(screen.getByText('No assets found')).toBeInTheDocument();
    });

    it.each([
        ['Orders (2.1.0)', { ...ORDERS_API, version: '2.1.0' }],
        ['Orders', ORDERS_API],
    ])('names the discovered API %s in its row', (expectedName, api) => {
        renderPreview(aPreview({ totalCount: 1, apis: [api] }));

        expect(
            dataTableHarness()
                .getRow(/Orders/)
                .getCellText('Name'),
        ).toBe(expectedName);
    });

    it('shows no badge for a discovered API whose state is neither NEW nor UPDATE', () => {
        const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        const unknownStateApi = { ...ORDERS_API, state: 'DELETE' } as unknown as IntegrationPreviewApi;

        renderPreview(aPreview({ totalCount: 1, apis: [unknownStateApi] }));

        expect(
            dataTableHarness()
                .getRow(/Orders/)
                .getCellText('Create or update'),
        ).toBe('');
        warn.mockRestore();
    });

    it.each([
        ['hides', 'fits within', SMALLEST_TABLE_PAGE_SIZE, false],
        ['shows', 'exceeds', SMALLEST_TABLE_PAGE_SIZE + 1, true],
    ])('%s pagination when the discovered API count %s the smallest page size', (_shown, _relation, apiCount, paginated) => {
        const apis = Array.from({ length: apiCount }, (_, index) => ({ ...ORDERS_API, id: `api-${index}`, name: `API ${index}` }));

        renderPreview(aPreview({ totalCount: apiCount, apis }));

        expect(screen.queryByRole('button', { name: 'Next page' }) !== null).toBe(paginated);
    });

    it('shows the next slice of discovered APIs on the next page', () => {
        const apis = Array.from({ length: SMALLEST_TABLE_PAGE_SIZE + 1 }, (_, index) => ({
            ...ORDERS_API,
            id: `api-${index}`,
            name: `API ${index}`,
        }));
        renderPreview(aPreview({ totalCount: apis.length, apis }));

        fireEvent.click(screen.getByRole('button', { name: 'Next page' }));

        expect(
            dataTableHarness()
                .getRows()
                .map(row => row.getCellText('Name')),
        ).toEqual([`API ${SMALLEST_TABLE_PAGE_SIZE}`]);
    });

    it('returns to the first discovered APIs when the page size grows from the next page', async () => {
        const apis = Array.from({ length: SMALLEST_TABLE_PAGE_SIZE + 1 }, (_, index) => ({
            ...ORDERS_API,
            id: `api-${index}`,
            name: `API ${index}`,
        }));
        renderPreview(aPreview({ totalCount: apis.length, apis }));
        fireEvent.click(screen.getByRole('button', { name: 'Next page' }));

        await userEvent.click(screen.getByRole('combobox', { name: 'Items per page' }));
        await userEvent.click(screen.getByRole('option', { name: '25' }));

        expect(
            dataTableHarness()
                .getRows()
                .map(row => row.getCellText('Name')),
        ).toEqual(apis.map(api => api.name));
    });

    it.each([
        ['complete', false, ['New APIs', 'APIs to update']],
        ['partial', true, []],
    ])('renders only the selective ingestion switches a %s discovery allows', (_discovery, isPartiallyDiscovered, expectedSwitches) => {
        renderPreview(aPreview({ isPartiallyDiscovered }));

        expect(switchNames()).toEqual(expectedSwitches);
    });

    it.each([
        ['New APIs', 'off and disabled', { totalCount: 3, newCount: 0, updateCount: 3 }, { checked: false, disabled: true }],
        ['New APIs', 'on', { totalCount: 5, newCount: 2, updateCount: 3 }, { checked: true, disabled: false }],
        ['APIs to update', 'off and disabled', { totalCount: 3, newCount: 3, updateCount: 0 }, { checked: false, disabled: true }],
        ['APIs to update', 'on', { totalCount: 5, newCount: 2, updateCount: 3 }, { checked: true, disabled: false }],
    ])('first renders the %s switch %s for counts %j', (switchName, _state, counts, expectedSwitch) => {
        renderPreview(aPreview(counts));

        expect(switchState(switchName)).toEqual(expectedSwitch);
    });

    it.each([
        ['only the NEW APIs', false, ['APIs to update'], { kind: 'SELECTED', apiIds: [ORDERS_API.id] }],
        ['only the APIs to update', false, ['New APIs'], { kind: 'SELECTED', apiIds: [PAYMENTS_API.id] }],
        ['every listed API', false, [], { kind: 'SELECTED', apiIds: [ORDERS_API.id, PAYMENTS_API.id] }],
        ['every API at the provider on a partial discovery', true, [], { kind: 'ALL' }],
    ])('proceeds with %s', (_selection, isPartiallyDiscovered, switchesTurnedOff, expectedScope) => {
        const onProceed = jest.fn();
        renderPreview(aPreview({ isPartiallyDiscovered }), onProceed);

        switchesTurnedOff.forEach(name => fireEvent.click(screen.getByRole('switch', { name })));
        fireEvent.click(screen.getByRole('button', { name: 'Proceed' }));

        expect(onProceed.mock.calls).toEqual([[expectedScope]]);
    });

    it('proceeds with every listed API, including those past the first table page', () => {
        const onProceed = jest.fn();
        const apis = Array.from({ length: SMALLEST_TABLE_PAGE_SIZE + 2 }, (_, index) =>
            index % 2 === 0 ? { ...ORDERS_API, id: `api-n-${index}` } : { ...PAYMENTS_API, id: `api-u-${index}` },
        );
        renderPreview(aPreview({ totalCount: apis.length, newCount: apis.length / 2, updateCount: apis.length / 2, apis }), onProceed);

        fireEvent.click(screen.getByRole('button', { name: 'Proceed' }));

        expect(onProceed.mock.calls).toEqual([[{ kind: 'SELECTED', apiIds: apis.map(api => api.id) }]]);
    });

    it('disables Proceed once both the NEW and the UPDATE switches are off', () => {
        renderPreview(aPreview());

        fireEvent.click(screen.getByRole('switch', { name: 'New APIs' }));
        fireEvent.click(screen.getByRole('switch', { name: 'APIs to update' }));

        expect(screen.getByRole('button', { name: 'Proceed' })).toHaveProperty('disabled', true);
    });

    it.each([
        [
            'enables',
            'on a partial discovery that lists no API',
            { isPartiallyDiscovered: true, totalCount: 0, newCount: 0, updateCount: 0, apis: [] },
            false,
            false,
        ],
        [
            'disables',
            'on a complete discovery that lists no API',
            { isPartiallyDiscovered: false, totalCount: 0, newCount: 0, updateCount: 0, apis: [] },
            false,
            true,
        ],
        ['disables', 'while ingestion is starting', {}, true, true],
    ])('%s Proceed %s', (_enabled, _situation, overrides, isProceeding, expectedDisabled) => {
        renderPreview(aPreview(overrides), jest.fn(), isProceeding);

        expect(screen.getByRole('button', { name: 'Proceed' })).toHaveProperty('disabled', expectedDisabled);
    });
});
