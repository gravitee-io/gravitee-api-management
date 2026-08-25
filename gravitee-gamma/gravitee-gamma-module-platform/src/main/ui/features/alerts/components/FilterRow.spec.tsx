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
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { FilterRow } from './FilterRow';
import { API_METRICS, HEALTHCHECK_METRICS, NODE_LIFECYCLE_METRICS, NODE_METRICS } from '../constants/alertConstants';

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

beforeAll(() => {
    Element.prototype.hasPointerCapture = jest.fn();
    Element.prototype.setPointerCapture = jest.fn();
    Element.prototype.releasePointerCapture = jest.fn();
    Element.prototype.scrollIntoView = jest.fn();
});

describe('FilterRow', () => {
    it('shows Classic compare multiplier and property for JVM Heap (%)', () => {
        render(
            <FilterRow
                filter={{ type: 'COMPARE', property: 'jvm.mem.heap.percent', operator: 'GT' }}
                index={0}
                metrics={NODE_METRICS}
                onChange={jest.fn()}
                onRemove={jest.fn()}
            />,
        );

        expect(screen.getByText('Multiplier')).not.toBeNull();
        expect(screen.getByText('Property')).not.toBeNull();
        expect(screen.queryByText('Threshold')).toBeNull();
    });

    it('always shows Type for a STRING-only filter metric', () => {
        render(
            <FilterRow
                filter={{ type: 'STRING', property: 'node.event', operator: 'EQUALS' }}
                index={0}
                metrics={NODE_LIFECYCLE_METRICS}
                onChange={jest.fn()}
                onRemove={jest.fn()}
            />,
        );

        expect(screen.getByLabelText(/^type$/i)).not.toBeNull();
        expect(screen.getByText('Value')).not.toBeNull();
    });

    it('shows Value for health-check Old Status', () => {
        render(
            <FilterRow
                filter={{ type: 'STRING', property: 'status.old', operator: 'EQUALS' }}
                index={0}
                metrics={HEALTHCHECK_METRICS}
                onChange={jest.fn()}
                onRemove={jest.fn()}
            />,
        );

        expect(screen.getByText('Value')).not.toBeNull();
        expect(screen.queryByPlaceholderText('Value to match')).toBeNull();
    });

    it('lets the user type a high threshold below low (Classic marks invalid, does not block input)', () => {
        const onChange = jest.fn();
        render(
            <FilterRow
                filter={{ type: 'THRESHOLD_RANGE', property: 'os.cpu.percent', thresholdLow: 23 }}
                index={0}
                metrics={NODE_METRICS}
                onChange={onChange}
                onRemove={jest.fn()}
            />,
        );

        fireEvent.change(screen.getByPlaceholderText('e.g. 500'), { target: { value: '10' } });

        expect(onChange).toHaveBeenCalledWith(0, expect.objectContaining({ thresholdHigh: 10 }));
    });

    it('shows a range error when high threshold is below low', () => {
        render(
            <FilterRow
                filter={{
                    type: 'THRESHOLD_RANGE',
                    property: 'os.cpu.percent',
                    thresholdLow: 23,
                    thresholdHigh: 10,
                }}
                index={0}
                metrics={NODE_METRICS}
                onChange={jest.fn()}
                onRemove={jest.fn()}
            />,
        );

        expect(screen.getByText('High threshold must be greater than or equal to low threshold.')).not.toBeNull();
    });

    it('clears a stale MATCHES pattern when switching to EQUALS so it is not reused as an invalid Value', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();
        render(
            <FilterRow
                filter={{ type: 'STRING', property: 'error.key', operator: 'MATCHES', pattern: 'API.*' }}
                index={0}
                metrics={API_METRICS}
                onChange={onChange}
                onRemove={jest.fn()}
            />,
        );

        const operatorSelect = screen.getAllByRole('combobox').find(el => el.textContent?.toLowerCase().includes('matches'));
        expect(operatorSelect).toBeDefined();
        await user.click(operatorSelect!);
        await user.click(screen.getByRole('option', { name: 'equals to' }));

        expect(onChange).toHaveBeenCalledWith(0, expect.objectContaining({ operator: 'EQUALS', pattern: undefined }));
    });
});
