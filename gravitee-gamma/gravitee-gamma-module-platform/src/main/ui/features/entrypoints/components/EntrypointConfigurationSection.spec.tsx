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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import { EntrypointConfigurationSection } from './EntrypointConfigurationSection';
import { useSaveEntrypointConfigurations } from '../hooks/useSaveEntrypointConfigurations';
import type { EnvironmentEntrypointConfig } from '../types/entrypoint';

jest.mock('../../../shared/copyToClipboard', () => ({
    copyTextToClipboardWithNotifyHandler: jest.fn(),
}));

jest.mock('../hooks/useSaveEntrypointConfigurations');

const mockUseSaveEntrypointConfigurations = jest.mocked(useSaveEntrypointConfigurations);
const mockMutateAsync = jest.fn();

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

function renderSection(
    overrides: {
        configs?: EnvironmentEntrypointConfig[];
        failedEnvironmentNames?: string[];
        isLoading?: boolean;
        isError?: boolean;
        canEdit?: boolean;
    } = {},
) {
    return render(
        <EntrypointConfigurationSection
            configs={CONFIGS}
            failedEnvironmentNames={[]}
            isLoading={false}
            isError={false}
            canEdit={false}
            {...overrides}
        />,
    );
}

describe('EntrypointConfigurationSection', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockMutateAsync.mockResolvedValue({ succeededEnvironmentIds: ['env-1'], failed: [] });
        mockUseSaveEntrypointConfigurations.mockReturnValue({
            mutateAsync: mockMutateAsync,
            isPending: false,
        } as unknown as ReturnType<typeof useSaveEntrypointConfigurations>);
    });

    it('renders one card block per environment', () => {
        renderSection();
        expect(screen.getByText('Production')).not.toBeNull();
        expect(screen.getByText('Development')).not.toBeNull();
        expect(screen.getByDisplayValue('https://api.company.com')).not.toBeNull();
        expect(screen.getByDisplayValue('https://dev.company.com')).not.toBeNull();
    });

    it('shows a partial-failure banner when some environments fail to load', () => {
        renderSection({
            configs: [CONFIGS[0]!],
            failedEnvironmentNames: ['Staging'],
        });
        expect(screen.getByText(/Could not load entrypoint defaults for: Staging/)).not.toBeNull();
    });

    it('shows an error when environment list fails', () => {
        renderSection({ configs: [], isError: true });
        expect(screen.getByText(/Failed to load environments/)).not.toBeNull();
    });

    it('invokes copy helper when copy button is clicked', () => {
        const { copyTextToClipboardWithNotifyHandler } = jest.requireMock('../../../shared/copyToClipboard') as {
            copyTextToClipboardWithNotifyHandler: jest.Mock;
        };
        renderSection();
        fireEvent.click(screen.getAllByRole('button', { name: 'Copy Default HTTP entrypoint' })[0]!);
        expect(copyTextToClipboardWithNotifyHandler).toHaveBeenCalledWith('https://api.company.com', 'Copied to clipboard');
    });

    it('does not show Discard/Save when canEdit is false', () => {
        renderSection({ canEdit: false });
        const httpInput = screen.getByDisplayValue('https://api.company.com');
        fireEvent.change(httpInput, { target: { value: 'https://changed.company.com' } });
        expect(screen.queryByRole('button', { name: 'Discard' })).toBeNull();
        expect(screen.queryByRole('button', { name: 'Save' })).toBeNull();
    });

    it('shows Discard/Save when canEdit and values are dirty', () => {
        renderSection({ canEdit: true });
        expect(screen.queryByRole('button', { name: 'Discard' })).toBeNull();
        expect(screen.queryByRole('button', { name: 'Save' })).toBeNull();

        fireEvent.change(screen.getByDisplayValue('https://api.company.com'), {
            target: { value: 'https://changed.company.com' },
        });

        expect(screen.getByRole('button', { name: 'Discard' })).not.toBeNull();
        expect(screen.getByRole('button', { name: 'Save' })).not.toBeNull();
    });

    it('reverts dirty values when Discard is clicked', () => {
        renderSection({ canEdit: true });
        const httpInput = screen.getByDisplayValue('https://api.company.com');
        fireEvent.change(httpInput, { target: { value: 'https://changed.company.com' } });
        expect(screen.getByDisplayValue('https://changed.company.com')).not.toBeNull();

        fireEvent.click(screen.getByRole('button', { name: 'Discard' }));

        expect(screen.getByDisplayValue('https://api.company.com')).not.toBeNull();
        expect(screen.queryByRole('button', { name: 'Discard' })).toBeNull();
        expect(screen.queryByRole('button', { name: 'Save' })).toBeNull();
    });

    it('saves only dirty environments with merged payload', async () => {
        renderSection({ canEdit: true });
        const tcpInputs = screen.getAllByLabelText('Default TCP port');
        fireEvent.change(tcpInputs[0]!, { target: { value: '8888' } });
        fireEvent.click(screen.getByRole('button', { name: 'Save' }));

        expect(mockMutateAsync).toHaveBeenCalledTimes(1);
        const [inputs] = mockMutateAsync.mock.calls[0]!;
        expect(inputs).toEqual([
            {
                environmentId: 'env-1',
                settings: {
                    portal: {
                        entrypoint: 'https://api.company.com',
                        tcpPort: 8888,
                        kafkaDomain: '{apiHost}.company.org',
                        kafkaPort: 9092,
                    },
                },
            },
        ]);

        await waitFor(() => {
            expect(screen.queryByRole('button', { name: 'Discard' })).toBeNull();
            expect(screen.queryByRole('button', { name: 'Save' })).toBeNull();
        });
    });

    it('keeps a failed environment dirty when saving multiple environments partially fails', async () => {
        mockMutateAsync.mockResolvedValue({
            succeededEnvironmentIds: ['env-1'],
            failed: [{ environmentId: 'env-2', error: new Error('boom') }],
        });
        renderSection({ canEdit: true });
        const tcpInputs = screen.getAllByLabelText('Default TCP port');
        fireEvent.change(tcpInputs[0]!, { target: { value: '8888' } });
        fireEvent.change(tcpInputs[1]!, { target: { value: '9999' } });
        fireEvent.click(screen.getByRole('button', { name: 'Save' }));

        await waitFor(() => {
            // env-1 succeeded and is no longer dirty, but env-2 failed and Save/Discard should still show.
            expect(screen.getByRole('button', { name: 'Discard' })).not.toBeNull();
            expect(screen.getByRole('button', { name: 'Save' })).not.toBeNull();
        });
    });
});
