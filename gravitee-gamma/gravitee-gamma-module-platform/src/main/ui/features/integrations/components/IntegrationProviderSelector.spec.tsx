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

import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { IntegrationProviderSelector } from './IntegrationProviderSelector';

const GROUPED_PROVIDER_LABELS = [
    {
        group: 'API gateways',
        labels: [
            'Apigee',
            'AWS API Gateway',
            'Azure API Management',
            'Edge Stack',
            'IBM API Connect',
            'MuleSoft',
            'SAP Business Technology Platform',
        ],
    },
    { group: 'Event brokers', labels: ['Confluent Platform', 'Solace'] },
    { group: 'AI agents', labels: ['A2A Protocol'] },
];

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

function providerCards() {
    return screen.queryAllByRole('region').flatMap(group => within(group).getAllByRole('button'));
}

describe('IntegrationProviderSelector', () => {
    it('lists the providers grouped by type, in alphabetical order within each group', () => {
        renderWithGraphene(<IntegrationProviderSelector onSelect={jest.fn()} />);

        const groupNames = screen.getAllByRole('region').map(region => region.getAttribute('aria-label'));
        expect(groupNames).toEqual(GROUPED_PROVIDER_LABELS.map(({ group }) => group));
        GROUPED_PROVIDER_LABELS.forEach(({ group, labels }) => {
            const cards = within(screen.getByRole('region', { name: group })).getAllByRole('button');
            expect(cards).toHaveLength(labels.length);
            cards.forEach((card, index) => expect(card).toHaveAccessibleName(new RegExp(`^${labels[index]}`)));
        });
    });

    it.each([
        { provider: 'A2A Protocol', token: 'A2A' },
        { provider: 'MuleSoft', token: 'mulesoft' },
        { provider: 'Solace', token: 'solace' },
    ])('reports the $token provider entry when $provider is clicked', async ({ provider, token }) => {
        const onSelect = jest.fn();
        renderWithGraphene(<IntegrationProviderSelector onSelect={onSelect} />);

        await userEvent.click(screen.getByRole('button', { name: new RegExp(`^${provider}`) }));

        expect(onSelect).toHaveBeenCalledTimes(1);
        expect(onSelect).toHaveBeenCalledWith(expect.objectContaining({ token, label: provider }));
    });

    it('narrows the list to the providers whose name matches the filter and hides groups left empty', async () => {
        renderWithGraphene(<IntegrationProviderSelector onSelect={jest.fn()} />);

        await userEvent.type(screen.getByRole('textbox', { name: 'Filter providers' }), 'mule');

        expect(providerCards()).toHaveLength(1);
        expect(providerCards()[0]).toHaveAccessibleName(/^MuleSoft/);
        expect(screen.getAllByRole('region').map(region => region.getAttribute('aria-label'))).toEqual(['API gateways']);
    });

    it.each([{ filter: 'MULE' }, { filter: '  mule  ' }])('shows only MuleSoft for the filter "$filter"', async ({ filter }) => {
        renderWithGraphene(<IntegrationProviderSelector onSelect={jest.fn()} />);

        await userEvent.type(screen.getByRole('textbox', { name: 'Filter providers' }), filter);

        expect(providerCards()).toHaveLength(1);
        expect(providerCards()[0]).toHaveAccessibleName(/^MuleSoft/);
    });

    it('also matches the filter against the provider description', async () => {
        renderWithGraphene(<IntegrationProviderSelector onSelect={jest.fn()} />);

        await userEvent.type(screen.getByRole('textbox', { name: 'Filter providers' }), 'topics');

        expect(providerCards()).toHaveLength(1);
        expect(providerCards()[0]).toHaveAccessibleName(/^Confluent Platform/);
    });

    it.each([{ filter: 'zzz' }, { filter: '  zzz  ' }])('says no provider matches "zzz" for the filter "$filter"', async ({ filter }) => {
        renderWithGraphene(<IntegrationProviderSelector onSelect={jest.fn()} />);

        await userEvent.type(screen.getByRole('textbox', { name: 'Filter providers' }), filter);

        expect(screen.queryAllByRole('button')).toHaveLength(0);
        expect(screen.getByText('No providers match "zzz".')).toBeInTheDocument();
    });

    it('keeps every provider listed when the filter is only whitespace', async () => {
        renderWithGraphene(<IntegrationProviderSelector onSelect={jest.fn()} />);

        await userEvent.type(screen.getByRole('textbox', { name: 'Filter providers' }), '   ');

        expect(providerCards()).toHaveLength(GROUPED_PROVIDER_LABELS.flatMap(({ labels }) => labels).length);
        expect(screen.queryByText(/No providers match/)).not.toBeInTheDocument();
    });
});
