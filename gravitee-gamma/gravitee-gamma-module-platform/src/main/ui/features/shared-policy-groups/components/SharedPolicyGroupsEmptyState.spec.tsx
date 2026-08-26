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

import { SharedPolicyGroupsEmptyState } from './SharedPolicyGroupsEmptyState';

describe('SharedPolicyGroupsEmptyState', () => {
    it('explains how Shared Policy Groups work and their benefits', () => {
        render(<SharedPolicyGroupsEmptyState />);

        expect(screen.queryByText('How it works')).not.toBeNull();
        expect(screen.queryByText('Policy steps')).not.toBeNull();
        expect(screen.queryByText('Shared Policy Group')).not.toBeNull();
        expect(screen.queryByText('Attached to API flows')).not.toBeNull();
        expect(screen.queryByText(/view a group's history and roll back to a previous revision at any time/)).not.toBeNull();
    });
});
