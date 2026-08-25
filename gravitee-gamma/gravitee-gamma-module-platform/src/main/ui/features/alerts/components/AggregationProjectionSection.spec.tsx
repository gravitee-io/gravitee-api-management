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

import { AggregationProjectionSection } from './AggregationProjectionSection';
import { getProjectionMetricsForRuleId } from '../constants/alertConstants';
import { propertyProjection } from '../utils/alertProjection';

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

const METRICS = getProjectionMetricsForRuleId('REQUEST@METRICS_AGGREGATION');
const CONDITION = {
    type: 'AGGREGATION' as const,
    property: 'response.response_time',
    aggregationFunction: 'AVG' as const,
    operator: 'GT' as const,
    threshold: 500,
};

describe('AggregationProjectionSection', () => {
    it('lets the user set a Classic group-by projection', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();
        render(<AggregationProjectionSection condition={CONDITION} metrics={METRICS} onChange={onChange} />);

        expect(screen.getByText('Aggregation')).not.toBeNull();
        await user.click(screen.getByRole('button', { name: /set a projection/i }));

        expect(onChange).toHaveBeenCalledWith(
            expect.objectContaining({
                projections: [propertyProjection('response.status')],
            }),
        );
    });

    it('clears the projection', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();
        render(
            <AggregationProjectionSection
                condition={{ ...CONDITION, projections: [propertyProjection('api')] }}
                metrics={METRICS}
                onChange={onChange}
            />,
        );

        await user.click(screen.getByRole('button', { name: /remove/i }));
        expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ projections: [] }));
    });
});
