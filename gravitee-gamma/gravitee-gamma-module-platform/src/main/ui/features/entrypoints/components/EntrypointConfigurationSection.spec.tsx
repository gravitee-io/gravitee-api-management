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
import { fireEvent, render, screen } from '@testing-library/react';

import { EntrypointConfigurationSection } from './EntrypointConfigurationSection';
import type { EnvironmentEntrypointConfig } from '../types/entrypoint';

jest.mock('../../../shared/copyToClipboard', () => ({
    copyTextToClipboardWithNotifyHandler: jest.fn(),
}));

const CONFIGS: EnvironmentEntrypointConfig[] = [
    {
        environment: { id: 'env-1', name: 'Production' },
        portalSettings: {
            portal: {
                entrypoint: 'https://api.company.com',
                tcpPort: 4082,
                kafkaDomain: '{apiHost}.company.org',
                kafkaPort: 9092,
            },
        },
    },
    {
        environment: { id: 'env-2', name: 'Development' },
        portalSettings: {
            portal: {
                entrypoint: 'https://dev.company.com',
                tcpPort: 4082,
                kafkaDomain: '{apiHost}',
                kafkaPort: 9092,
            },
        },
    },
];

describe('EntrypointConfigurationSection', () => {
    it('renders one card block per environment', () => {
        render(<EntrypointConfigurationSection configs={CONFIGS} failedEnvironmentNames={[]} isLoading={false} isError={false} />);
        expect(screen.getByText('Production')).not.toBeNull();
        expect(screen.getByText('Development')).not.toBeNull();
        expect(screen.getByDisplayValue('https://api.company.com')).not.toBeNull();
        expect(screen.getByDisplayValue('https://dev.company.com')).not.toBeNull();
    });

    it('shows a partial-failure banner when some environments fail to load', () => {
        render(
            <EntrypointConfigurationSection
                configs={[CONFIGS[0]!]}
                failedEnvironmentNames={['Staging']}
                isLoading={false}
                isError={false}
            />,
        );
        expect(screen.getByText(/Could not load entrypoint defaults for: Staging/)).not.toBeNull();
    });

    it('shows an error when environment list fails', () => {
        render(<EntrypointConfigurationSection configs={[]} failedEnvironmentNames={[]} isLoading={false} isError />);
        expect(screen.getByText(/Failed to load environments/)).not.toBeNull();
    });

    it('invokes copy helper when copy button is clicked', () => {
        const { copyTextToClipboardWithNotifyHandler } = jest.requireMock('../../../shared/copyToClipboard') as {
            copyTextToClipboardWithNotifyHandler: jest.Mock;
        };
        render(<EntrypointConfigurationSection configs={CONFIGS} failedEnvironmentNames={[]} isLoading={false} isError={false} />);
        fireEvent.click(screen.getAllByRole('button', { name: 'Copy Default HTTP entrypoint' })[0]!);
        expect(copyTextToClipboardWithNotifyHandler).toHaveBeenCalledWith('https://api.company.com', 'Copied to clipboard');
    });
});
