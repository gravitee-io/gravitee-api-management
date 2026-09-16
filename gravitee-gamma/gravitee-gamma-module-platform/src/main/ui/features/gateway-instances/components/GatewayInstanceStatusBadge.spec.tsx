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

import { GatewayInstanceStatusBadge } from './GatewayInstanceStatusBadge';

describe('GatewayInstanceStatusBadge', () => {
    it.each([
        ['STARTED', 'Running', 'success'],
        ['STOPPED', 'Stopped', 'secondary'],
        ['UNKNOWN', 'Unknown', 'outline'],
        [undefined, 'Unknown', 'outline'],
    ] as const)('renders %s as %s (%s badge)', (state, label, variant) => {
        render(<GatewayInstanceStatusBadge state={state} />);
        expect(screen.queryByText(label)).not.toBeNull();
        expect(screen.getByText(label).closest('[data-slot="badge"]')?.getAttribute('data-variant')).toBe(variant);
    });
});
