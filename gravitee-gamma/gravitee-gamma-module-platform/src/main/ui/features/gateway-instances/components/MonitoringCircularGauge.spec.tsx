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

import { MonitoringCircularGauge } from './MonitoringCircularGauge';

describe('MonitoringCircularGauge', () => {
    it('renders a circular SVG with the percentage and label', () => {
        const { container } = render(<MonitoringCircularGauge pct={54} label="Heap" />);

        expect(screen.getByLabelText('54%')).not.toBeNull();
        expect(screen.getByText('54%')).not.toBeNull();
        expect(screen.getByText('Heap')).not.toBeNull();
        expect(container.querySelector('svg circle[stroke-dasharray]')).not.toBeNull();
    });

    it('clamps non-finite values to 0%', () => {
        render(<MonitoringCircularGauge pct={Number.NaN} label="CPU" />);
        expect(screen.getByLabelText('0%')).not.toBeNull();
    });
});
