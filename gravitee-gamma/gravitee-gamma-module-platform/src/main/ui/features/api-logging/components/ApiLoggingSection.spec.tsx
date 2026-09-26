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

import { ApiLoggingSection } from './ApiLoggingSection';
import type { ApiLoggingFieldReadonly } from '../utils/apiLoggingFormState';
import { validateApiLoggingForm, type ApiLoggingFormState } from '../utils/apiLoggingValidators';

const VALUE: ApiLoggingFormState = {
    maxDurationMillis: '15000',
    auditEnabled: true,
    auditTrailEnabled: true,
    userDisplayed: true,
    probabilisticDefault: '0.25',
    probabilisticLimit: '0.5',
    countDefault: '200',
    countLimit: '20',
    temporalDefault: 'PT10S',
    temporalLimit: 'PT20S',
    windowedCountDefault: '1/PT10S',
    windowedCountLimit: '1/PT1S',
};

const READONLY: ApiLoggingFieldReadonly = {
    maxDurationMillis: false,
    auditEnabled: false,
    auditTrailEnabled: false,
    userDisplayed: false,
    probabilisticDefault: false,
    probabilisticLimit: false,
    countDefault: false,
    countLimit: false,
    temporalDefault: false,
    temporalLimit: false,
    windowedCountDefault: false,
    windowedCountLimit: false,
};

describe('ApiLoggingSection', () => {
    it('shows temporal compare errors on both fields and at group level', () => {
        const errors = validateApiLoggingForm(VALUE);
        render(<ApiLoggingSection value={VALUE} errors={errors} disabled={false} readonly={READONLY} onChange={jest.fn()} />);

        expect(screen.getAllByText('Default should be greater than Limit')).toHaveLength(3);
        expect(document.getElementById('api-logging-temporal-group-error')).not.toBeNull();
    });

    it('shows count compare errors on both fields when validation fails', () => {
        const countCompareValue: ApiLoggingFormState = {
            ...VALUE,
            countDefault: '5',
            countLimit: '20',
            temporalDefault: 'PT5S',
            temporalLimit: 'PT1S',
        };
        const errors = validateApiLoggingForm(countCompareValue);
        render(<ApiLoggingSection value={countCompareValue} errors={errors} disabled={false} readonly={READONLY} onChange={jest.fn()} />);

        expect(screen.getAllByText('Default should be greater than Limit')).toHaveLength(2);
    });
});
