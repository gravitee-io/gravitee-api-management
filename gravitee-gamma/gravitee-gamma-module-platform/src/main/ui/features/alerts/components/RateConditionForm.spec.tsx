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

import { applyRateComparison, RateConditionForm, rateComparisonFrom } from './RateConditionForm';
import { NODE_METRICS } from '../constants/alertConstants';

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

const numericRate = {
    type: 'RATE' as const,
    comparisonType: 'THRESHOLD' as const,
    property: 'os.cpu.percent',
    operator: 'GT' as const,
    threshold: 50,
    rateOperator: 'GT' as const,
    rateThreshold: 10,
    duration: 1,
    timeUnit: 'MINUTES' as const,
};

describe('rate comparison helpers', () => {
    it('keeps RATE wrapper fields when the When comparison type changes', () => {
        expect(
            applyRateComparison(numericRate, {
                type: 'THRESHOLD_RANGE',
                property: 'os.cpu.percent',
                thresholdLow: 10,
                thresholdHigh: 80,
            }),
        ).toEqual(
            expect.objectContaining({
                type: 'RATE',
                comparisonType: 'THRESHOLD_RANGE',
                rateOperator: 'GT',
                rateThreshold: 10,
                thresholdLow: 10,
                thresholdHigh: 80,
            }),
        );
    });

    it('reads the nested When comparison for the Classic widget', () => {
        expect(rateComparisonFrom(numericRate)).toEqual(
            expect.objectContaining({
                type: 'THRESHOLD',
                property: 'os.cpu.percent',
                operator: 'GT',
                threshold: 50,
            }),
        );
    });
});

describe('RateConditionForm', () => {
    it('shows Classic When type options for a node CPU comparison', () => {
        render(<RateConditionForm condition={numericRate} metrics={NODE_METRICS} onChange={jest.fn()} />);

        expect(screen.getByText('When')).not.toBeNull();
        expect(screen.getByLabelText(/^type$/i)).not.toBeNull();
        expect(screen.getByText('If rate is')).not.toBeNull();
    });
});
