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

import { AggregationConditionForm } from './AggregationConditionForm';
import { NODE_METRICS } from '../constants/alertConstants';

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

describe('AggregationConditionForm', () => {
    it('hides Metric when Function is count, like Classic', () => {
        render(
            <AggregationConditionForm
                condition={{
                    type: 'AGGREGATION',
                    property: 'node.hostname',
                    aggregationFunction: 'COUNT',
                    operator: 'GT',
                    threshold: 5,
                    duration: 1,
                    timeUnit: 'MINUTES',
                }}
                metrics={NODE_METRICS}
                onChange={jest.fn()}
            />,
        );

        expect(screen.getByText('Calculate')).not.toBeNull();
        expect(screen.queryByText('on')).toBeNull();
        expect(screen.queryByText('Metric')).toBeNull();
    });

    it('shows Metric for average, including Classic Hostname', () => {
        render(
            <AggregationConditionForm
                condition={{
                    type: 'AGGREGATION',
                    property: 'node.hostname',
                    aggregationFunction: 'AVG',
                    operator: 'GT',
                    timeUnit: 'MINUTES',
                }}
                metrics={NODE_METRICS}
                onChange={jest.fn()}
            />,
        );

        expect(screen.getByText('Calculate')).not.toBeNull();
        expect(screen.getByText('on')).not.toBeNull();
        expect(screen.getByText('Metric')).not.toBeNull();
        expect(screen.getByText('Hostname')).not.toBeNull();
    });
});
