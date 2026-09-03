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

import { TooltipProvider } from '@gravitee/graphene-core';
import { fireEvent, render, screen } from '@testing-library/react';
import { useState } from 'react';

import { ApiLoggingSection } from './ApiLoggingSection';
import type { ApiLoggingFormState } from '../utils/loggingValidators';

const INITIAL: ApiLoggingFormState = {
    maxDurationMillis: '15000',
    auditEnabled: true,
    auditTrailEnabled: false,
    userDisplayed: true,
    probabilisticDefault: '0.01',
    probabilisticLimit: '0.5',
    countDefault: '100',
    countLimit: '10',
    temporalDefault: 'PT1S',
    temporalLimit: 'PT1S',
    windowedCountDefault: '1/PT10S',
    windowedCountLimit: '1/PT1S',
};

function Harness({
    initial = INITIAL,
    disabled = false,
    readonly,
}: {
    initial?: ApiLoggingFormState;
    disabled?: boolean;
    readonly?: Parameters<typeof ApiLoggingSection>[0]['readonly'];
}) {
    const [value, setValue] = useState(initial);
    return (
        <TooltipProvider>
            <ApiLoggingSection value={value} disabled={disabled} readonly={readonly} onChange={setValue} />
        </TooltipProvider>
    );
}

describe('ApiLoggingSection', () => {
    it('renders duration, audit, user, and sampling groups in Graphene cards', () => {
        render(<Harness />);
        expect(screen.getByText('Duration').closest('[data-slot="card-title"]')).not.toBeNull();
        expect(screen.getByText('Audit').closest('[data-slot="card-title"]')).not.toBeNull();
        expect(screen.getByText('User').closest('[data-slot="card-title"]')).not.toBeNull();
        expect(screen.getByText('Message Sampling').closest('[data-slot="card-title"]')).not.toBeNull();
        expect(screen.getByRole('heading', { name: 'Probabilistic' })).not.toBeNull();
        expect(screen.getByRole('heading', { name: 'Count' })).not.toBeNull();
        expect(screen.getByRole('heading', { name: 'Temporal' })).not.toBeNull();
        expect(screen.getByRole('heading', { name: 'Windowed count' })).not.toBeNull();
        expect((screen.getByLabelText('Max Duration (in ms)') as HTMLInputElement).value).toBe('15000');
        expect((screen.getByTestId('api-logging-probabilistic-default') as HTMLInputElement).value).toBe('0.01');
        expect((screen.getByTestId('api-logging-windowed-count-limit') as HTMLInputElement).value).toBe('1/PT1S');
    });

    it('toggles audit and user switches', () => {
        render(<Harness />);
        fireEvent.click(screen.getByLabelText('Enable audit on API Logging consultation'));
        expect(screen.getByLabelText('Enable audit on API Logging consultation')).toHaveAttribute('aria-checked', 'false');
        fireEvent.click(screen.getByLabelText('Display end user on API Logging (in case of OAuth2/JWT plan)'));
        expect(screen.getByLabelText('Display end user on API Logging (in case of OAuth2/JWT plan)')).toHaveAttribute(
            'aria-checked',
            'false',
        );
    });

    it('shows sampling validation errors', () => {
        render(<Harness />);
        fireEvent.change(screen.getByTestId('api-logging-probabilistic-default'), { target: { value: '0.9' } });
        expect(screen.getAllByText('Default should be lower than Limit').length).toBeGreaterThan(0);
    });

    it('shows windowed-count format errors', () => {
        render(<Harness />);
        fireEvent.change(screen.getByTestId('api-logging-windowed-count-default'), { target: { value: 'bad' } });
        expect(screen.getByRole('alert').textContent).toContain('COUNT/DURATION');
    });

    it('locks only the system-provided max duration field', () => {
        render(<Harness readonly={{ maxDurationMillis: true }} />);
        expect((screen.getByLabelText('Max Duration (in ms)') as HTMLInputElement).disabled).toBe(true);
        expect((screen.getByTestId('api-logging-count-default') as HTMLInputElement).disabled).toBe(false);
        expect((screen.getByLabelText('Enable audit on API Logging consultation') as HTMLButtonElement).disabled).toBe(false);
    });

    it('disables every control when the section is read-only', () => {
        render(<Harness disabled />);
        expect((screen.getByLabelText('Max Duration (in ms)') as HTMLInputElement).disabled).toBe(true);
        expect((screen.getByTestId('api-logging-temporal-limit') as HTMLInputElement).disabled).toBe(true);
        expect((screen.getByLabelText('Enable audit on API Logging consultation') as HTMLButtonElement).disabled).toBe(true);
    });
});
