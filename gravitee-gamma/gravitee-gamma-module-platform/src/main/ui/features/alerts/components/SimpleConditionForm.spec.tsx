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

import { SimpleConditionForm } from './SimpleConditionForm';
import { API_METRICS, NODE_METRICS } from '../constants/alertConstants';

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

beforeAll(() => {
    Element.prototype.hasPointerCapture = jest.fn();
    Element.prototype.setPointerCapture = jest.fn();
    Element.prototype.releasePointerCapture = jest.fn();
    Element.prototype.scrollIntoView = jest.fn();
});

describe('SimpleConditionForm', () => {
    it('shows the Classic When connector', () => {
        render(
            <SimpleConditionForm
                condition={{ type: 'THRESHOLD', property: 'response.response_time', operator: 'GT', threshold: 10 }}
                metrics={API_METRICS}
                onChange={jest.fn()}
            />,
        );

        expect(screen.getByText('When')).not.toBeNull();
    });

    it('does not accept a negative threshold (classic min=1)', () => {
        const onChange = jest.fn();
        render(
            <SimpleConditionForm
                condition={{ type: 'THRESHOLD', property: 'response.response_time', operator: 'GT', threshold: 10 }}
                metrics={API_METRICS}
                onChange={onChange}
            />,
        );

        const input = screen.getByPlaceholderText('e.g. 500') as HTMLInputElement;
        expect(input.min).toBe('1');

        fireEvent.change(input, { target: { value: '-8' } });

        expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ threshold: 10 }));
    });

    it('keeps STRING fields when the metric is not in the catalog', () => {
        render(
            <SimpleConditionForm
                condition={{ type: 'STRING', property: 'custom.unknown', operator: 'EQUALS', pattern: 'foo' }}
                metrics={API_METRICS}
                onChange={jest.fn()}
            />,
        );

        expect(screen.getByPlaceholderText('e.g. API_KEY_MISSING')).not.toBeNull();
        expect(screen.queryByPlaceholderText('e.g. 500')).toBeNull();
    });

    it('always shows Type even when the metric only supports STRING', () => {
        render(
            <SimpleConditionForm
                condition={{ type: 'STRING', property: 'error.key', operator: 'EQUALS' }}
                metrics={API_METRICS}
                onChange={jest.fn()}
            />,
        );

        expect(screen.getByLabelText(/^type$/i)).not.toBeNull();
    });

    it('shows Value for Error Key with EQUALS', () => {
        render(
            <SimpleConditionForm
                condition={{ type: 'STRING', property: 'error.key', operator: 'EQUALS' }}
                metrics={API_METRICS}
                onChange={jest.fn()}
            />,
        );

        expect(screen.getByText('Value')).not.toBeNull();
        expect(screen.queryByPlaceholderText('e.g. API_KEY_MISSING')).toBeNull();
    });

    it('shows Pattern for Error Key with MATCHES', () => {
        render(
            <SimpleConditionForm
                condition={{ type: 'STRING', property: 'error.key', operator: 'MATCHES', pattern: 'API.*' }}
                metrics={API_METRICS}
                onChange={jest.fn()}
            />,
        );

        expect(screen.getByText('Pattern')).not.toBeNull();
        expect(screen.getByPlaceholderText('e.g. API_KEY_MISSING')).not.toBeNull();
    });

    it('shows Pattern for Hostname even with EQUALS because Classic has no loader', () => {
        render(
            <SimpleConditionForm
                condition={{ type: 'STRING', property: 'node.hostname', operator: 'EQUALS' }}
                metrics={NODE_METRICS}
                onChange={jest.fn()}
            />,
        );

        expect(screen.getByText('Pattern')).not.toBeNull();
        expect(screen.queryByText('Value')).toBeNull();
    });

    it('lets the user type a high threshold below low (Classic marks invalid, does not block input)', () => {
        const onChange = jest.fn();
        render(
            <SimpleConditionForm
                condition={{
                    type: 'THRESHOLD_RANGE',
                    property: 'os.cpu.percent',
                    thresholdLow: 23,
                }}
                metrics={NODE_METRICS}
                onChange={onChange}
            />,
        );

        fireEvent.change(screen.getByPlaceholderText('e.g. 500'), { target: { value: '10' } });

        expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ thresholdHigh: 10 }));
    });

    it('shows a range error when high threshold is below low', () => {
        render(
            <SimpleConditionForm
                condition={{
                    type: 'THRESHOLD_RANGE',
                    property: 'os.cpu.percent',
                    thresholdLow: 23,
                    thresholdHigh: 10,
                }}
                metrics={NODE_METRICS}
                onChange={jest.fn()}
            />,
        );

        expect(screen.getByText('High threshold must be greater than or equal to low threshold.')).not.toBeNull();
    });

    it('clears a stale MATCHES pattern when switching to EQUALS so it is not reused as an invalid Value', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();
        render(
            <SimpleConditionForm
                condition={{ type: 'STRING', property: 'error.key', operator: 'MATCHES', pattern: 'API.*' }}
                metrics={API_METRICS}
                onChange={onChange}
            />,
        );

        const operatorSelect = screen.getAllByRole('combobox').find(el => el.textContent?.toLowerCase().includes('matches'));
        expect(operatorSelect).toBeDefined();
        await user.click(operatorSelect!);
        await user.click(screen.getByRole('option', { name: 'equals to' }));

        expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ operator: 'EQUALS', pattern: undefined }));
    });
});
