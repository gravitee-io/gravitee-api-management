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
import { rateConditionWithMetric } from './RateConditionForm';

describe('rateConditionWithMetric', () => {
    const numericRate = {
        type: 'RATE' as const,
        property: 'response.response_time',
        operator: 'GT' as const,
        threshold: 500,
        rateOperator: 'GT' as const,
        rateThreshold: 50,
        duration: 1,
        timeUnit: 'MINUTES' as const,
    };

    it('resets to a string operator when switching to a string metric', () => {
        expect(rateConditionWithMetric(numericRate, 'error.key')).toEqual(
            expect.objectContaining({
                property: 'error.key',
                operator: 'EQUALS',
                threshold: undefined,
                pattern: undefined,
            }),
        );
    });

    it('resets to a numeric operator when switching to a numeric metric', () => {
        expect(
            rateConditionWithMetric(
                { ...numericRate, property: 'error.key', operator: 'CONTAINS', pattern: 'foo' },
                'response.response_time',
            ),
        ).toEqual(
            expect.objectContaining({
                property: 'response.response_time',
                operator: 'GTE',
                threshold: undefined,
                pattern: undefined,
            }),
        );
    });
});
