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

import { HealthCheckStep } from './HealthCheckStep';
import { DEFAULT_HEALTH_CHECK_CONFIG } from '../../../../utils/healthCheckForm';

it('places the enable health-check switch next to its label', () => {
    render(
        <HealthCheckStep
            mode="group"
            healthCheck={{ enabled: false, configuration: DEFAULT_HEALTH_CHECK_CONFIG }}
            errors={{}}
            onChange={jest.fn()}
            onConfigChange={jest.fn()}
        />,
    );

    const label = screen.getByText('Enable health-check');
    expect(label.parentElement?.className).toContain('justify-start');
    expect(label.parentElement?.className).not.toContain('justify-between');
    expect(label.parentElement?.contains(screen.getByRole('switch', { name: 'Enable health-check' }))).toBe(true);
});
