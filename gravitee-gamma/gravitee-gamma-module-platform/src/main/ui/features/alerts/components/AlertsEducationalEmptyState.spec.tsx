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

import { AlertsEducationalEmptyState } from './AlertsEducationalEmptyState';

describe('AlertsEducationalEmptyState', () => {
    it('renders the why/how-it-works/capabilities content', () => {
        render(<AlertsEducationalEmptyState />);

        expect(screen.getByText('Why configure alerts?')).not.toBeNull();
        expect(screen.getByText('How it works')).not.toBeNull();
        expect(screen.getByText('Alert rule')).not.toBeNull();
        expect(screen.getByText('Email · Slack · Webhook')).not.toBeNull();
        expect(screen.getByText('What you can alert on')).not.toBeNull();
        expect(screen.getByText('Key capabilities')).not.toBeNull();
        expect(screen.getByText(/Node lifecycle/)).not.toBeNull();
        expect(screen.getByText('Send notifications via email, Slack, or webhook')).not.toBeNull();
    });
});
