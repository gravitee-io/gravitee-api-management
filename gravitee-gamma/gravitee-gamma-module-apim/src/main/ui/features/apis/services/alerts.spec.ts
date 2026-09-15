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
import { formConditionToApi } from './alerts';

describe('formConditionToApi', () => {
    it('defaults THRESHOLD operator to GT when omitted from form state', () => {
        expect(
            formConditionToApi({
                type: 'THRESHOLD',
                property: 'response.response_time',
                threshold: 10,
            }),
        ).toEqual({
            type: 'THRESHOLD',
            property: 'response.response_time',
            operator: 'GT',
            threshold: 10,
        });
    });

    it('defaults STRING operator to EQUALS when omitted from form state', () => {
        expect(
            formConditionToApi({
                type: 'STRING',
                property: 'error.key',
                pattern: 'API_KEY_MISSING',
            }),
        ).toEqual({
            type: 'STRING',
            property: 'error.key',
            operator: 'EQUALS',
            pattern: 'API_KEY_MISSING',
            ignoreCase: true,
        });
    });

    it('maps THRESHOLD_RANGE with inclusive bounds and no spurious operator', () => {
        expect(
            formConditionToApi({
                type: 'THRESHOLD_RANGE',
                property: 'response.response_time',
                thresholdLow: 100,
                thresholdHigh: 500,
            }),
        ).toEqual({
            type: 'THRESHOLD_RANGE',
            property: 'response.response_time',
            operatorLow: 'INCLUSIVE',
            thresholdLow: 100,
            operatorHigh: 'INCLUSIVE',
            thresholdHigh: 500,
        });
    });

    it('maps STRING_COMPARE conditions', () => {
        expect(
            formConditionToApi({
                type: 'STRING_COMPARE',
                property: 'status.old',
                property2: 'status.new',
                operator: 'NOT_EQUALS',
            }),
        ).toEqual({
            type: 'STRING_COMPARE',
            property: 'status.old',
            property2: 'status.new',
            operator: 'NOT_EQUALS',
        });
    });
});
