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

import { SharedPolicyGroupStatusBadge } from './SharedPolicyGroupStatusBadge';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';

describe('SharedPolicyGroupStatusBadge', () => {
    it.each([
        ['DEPLOYED', 'Deployed'],
        ['UNDEPLOYED', 'Undeployed'],
        ['PENDING', 'Pending'],
    ] as const)('renders %s as %s', (lifecycleState, label) => {
        render(<SharedPolicyGroupStatusBadge lifecycleState={lifecycleState} />);
        expect(screen.queryByText(label)).not.toBeNull();
    });

    it('renders nothing when lifecycleState is undefined', () => {
        const { container } = render(<SharedPolicyGroupStatusBadge lifecycleState={undefined as SharedPolicyGroup['lifecycleState']} />);
        expect(container.firstChild).toBeNull();
    });
});
