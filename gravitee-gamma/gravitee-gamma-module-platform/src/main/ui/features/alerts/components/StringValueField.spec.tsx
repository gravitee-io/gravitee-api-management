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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { StringValueField } from './StringValueField';
import type { AlertMetricValueOption } from '../constants/alertConstants';

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

beforeAll(() => {
    Element.prototype.hasPointerCapture = jest.fn();
    Element.prototype.setPointerCapture = jest.fn();
    Element.prototype.releasePointerCapture = jest.fn();
    Element.prototype.scrollIntoView = jest.fn();
});

function manyTenants(): AlertMetricValueOption[] {
    return Array.from({ length: 12 }, (_, index) => ({
        value: `tenant-${index}`,
        label: `Tenant ${index}`,
    }));
}

describe('StringValueField', () => {
    it('keeps a searchable Value combobox anchored when there are many catalog values', async () => {
        const user = userEvent.setup();
        const onPatternChange = jest.fn();

        render(
            <StringValueField
                id="filter-value"
                operator="EQUALS"
                pattern={undefined}
                options={manyTenants()}
                onPatternChange={onPatternChange}
            />,
        );

        expect(screen.queryByPlaceholderText('Search values')).toBeNull();

        const valueInput = screen.getByPlaceholderText('Select a value');
        await user.click(valueInput);
        await user.type(valueInput, 'Tenant 11');

        expect(screen.getByRole('option', { name: 'Tenant 11' })).toBeTruthy();
        expect(screen.queryByRole('option', { name: 'Tenant 0' })).toBeNull();

        await user.click(screen.getByRole('option', { name: 'Tenant 11' }));

        expect(onPatternChange).toHaveBeenCalledWith('tenant-11');
    });

    it('keeps a free-text Pattern field when the operator is MATCHES', () => {
        const onPatternChange = jest.fn();

        render(
            <StringValueField
                id="filter-pattern"
                operator="MATCHES"
                pattern="API.*"
                options={manyTenants()}
                onPatternChange={onPatternChange}
            />,
        );

        expect(screen.getByText('Pattern')).toBeTruthy();
        expect(screen.getByDisplayValue('API.*')).toBeTruthy();
        expect(screen.queryByPlaceholderText('Select a value')).toBeNull();
    });
});
