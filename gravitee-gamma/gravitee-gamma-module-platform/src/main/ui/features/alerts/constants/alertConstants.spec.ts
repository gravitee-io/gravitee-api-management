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
import { API_METRICS, getConditionTypesForMetric, sourceTypeToRuleId } from './alertConstants';

describe('sourceTypeToRuleId', () => {
    it('maps a known source and type to its rule id', () => {
        expect(sourceTypeToRuleId('REQUEST', 'METRICS_SIMPLE_CONDITION')).toBe('REQUEST@METRICS_SIMPLE_CONDITION');
    });

    it('does not fall back to a default rule for an unrecognized type', () => {
        expect(sourceTypeToRuleId('CUSTOM', 'UNKNOWN_TYPE')).toBeUndefined();
    });
});

describe('getConditionTypesForMetric', () => {
    it('returns no types for an unrecognized metric instead of inventing THRESHOLD', () => {
        expect(getConditionTypesForMetric('not.a.metric', API_METRICS)).toEqual([]);
    });
});
