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
import { fireEvent, screen } from '@testing-library/react';

import { IntegrationProviderSelector } from './IntegrationProviderSelector';

const PROVIDER_LABELS_IN_DISPLAY_ORDER = [
    'A2A Protocol',
    'AWS API Gateway',
    'Solace',
    'Apigee',
    'Azure API Management',
    'IBM API Connect',
    'Confluent Platform',
    'MuleSoft',
    'Edge Stack',
    'SAP Business Technology Platform',
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

describe('IntegrationProviderSelector', () => {
    it('offers A2A first, then one radio per gateway-style provider, in display order', () => {
        renderWithGraphene(<IntegrationProviderSelector value={undefined} onChange={jest.fn()} />);

        const radios = screen.getAllByRole('radio');

        expect(radios).toHaveLength(PROVIDER_LABELS_IN_DISPLAY_ORDER.length);
        radios.forEach((radio, index) => expect(radio).toHaveAccessibleName(PROVIDER_LABELS_IN_DISPLAY_ORDER[index]));
    });

    it.each([
        { position: 'first', index: 0, provider: 'A2A' },
        { position: 'eighth', index: 7, provider: 'mulesoft' },
    ])('reports the stored provider value when the $position option is clicked', ({ index, provider }) => {
        const onChange = jest.fn();
        renderWithGraphene(<IntegrationProviderSelector value={undefined} onChange={onChange} />);

        fireEvent.click(screen.getAllByRole('radio')[index]);

        expect(onChange).toHaveBeenCalledWith(provider);
    });

    it('marks only the selected provider as checked', () => {
        renderWithGraphene(<IntegrationProviderSelector value="mulesoft" onChange={jest.fn()} />);

        const checkedStates = screen.getAllByRole('radio').map(radio => radio.getAttribute('aria-checked'));

        expect(checkedStates).toEqual(['false', 'false', 'false', 'false', 'false', 'false', 'false', 'true', 'false', 'false']);
    });
});
