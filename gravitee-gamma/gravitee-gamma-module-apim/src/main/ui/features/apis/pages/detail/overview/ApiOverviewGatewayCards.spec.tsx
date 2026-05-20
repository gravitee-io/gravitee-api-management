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

import { ApiOverviewGatewayCards } from './ApiOverviewGatewayCards';

describe('ApiOverviewGatewayCards', () => {
    it('shows the gateway URL when provided', () => {
        render(
            <ApiOverviewGatewayCards gatewayUrl="https://gateway.example.com/api" upstreamUrl={undefined} isLoadingEntrypoints={false} />,
        );
        expect(screen.getByText('https://gateway.example.com/api')).toBeInTheDocument();
    });

    it('shows the upstream URL when provided', () => {
        render(<ApiOverviewGatewayCards gatewayUrl={undefined} upstreamUrl="https://backend.internal:8080" isLoadingEntrypoints={false} />);
        expect(screen.getByText('https://backend.internal:8080')).toBeInTheDocument();
    });

    it('shows "Not configured" for both cards when URLs are absent', () => {
        render(<ApiOverviewGatewayCards gatewayUrl={undefined} upstreamUrl={undefined} isLoadingEntrypoints={false} />);
        expect(screen.getAllByText('Not configured')).toHaveLength(2);
    });

    it('does not show the gateway URL while the entrypoints are loading', () => {
        render(
            <ApiOverviewGatewayCards gatewayUrl="https://gateway.example.com/api" upstreamUrl={undefined} isLoadingEntrypoints={true} />,
        );
        expect(screen.queryByText('https://gateway.example.com/api')).toBeNull();
        // Upstream is synchronous (from context) — still shows "Not configured" while gateway loads
        expect(screen.getByText('Not configured')).toBeInTheDocument();
    });
});
