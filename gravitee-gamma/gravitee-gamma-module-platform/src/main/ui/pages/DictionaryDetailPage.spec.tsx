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

import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { DictionaryDetailPage } from './DictionaryDetailPage';
import {
    useDeployDictionary,
    useStartDictionary,
    useStopDictionary,
    useUpdateDictionary,
} from '../features/dictionaries/hooks/useDictionaryMutations';
import { useDictionaryPermissions } from '../features/dictionaries/hooks/useDictionaryPermissions';
import { useEnvironmentDictionary } from '../features/dictionaries/hooks/useEnvironmentDictionary';
import type { Dictionary } from '../features/dictionaries/types/dictionary';

jest.mock('../features/dictionaries/hooks/useDictionaryPermissions');
jest.mock('../features/dictionaries/hooks/useEnvironmentDictionary');
jest.mock('../features/dictionaries/hooks/useDictionaryMutations');
jest.mock('../shared/notify', () => ({
    notify: { error: jest.fn(), success: jest.fn() },
}));

// jsdom ships no ResizeObserver, which the switch mounts.
globalThis.ResizeObserver ??= class {
    observe() {}
    unobserve() {}
    disconnect() {}
};

const MASK = '••••••••••••';

const MANUAL: Dictionary = {
    id: 'd1',
    name: 'Secrets',
    description: 'Manual dictionary',
    type: 'MANUAL',
    properties: { PLAIN: 'visible', TOKEN: MASK },
    propertyOptions: { TOKEN: { encrypted: true } },
};

const mutateAsync = jest.fn();

function givenDictionary(dictionary: Dictionary, { canUpdate = true }: { canUpdate?: boolean } = {}) {
    jest.mocked(useDictionaryPermissions).mockReturnValue({ canRead: true, canCreate: true, canUpdate, canDelete: true });
    jest.mocked(useEnvironmentDictionary).mockReturnValue({ data: dictionary, isLoading: false, isError: false } as ReturnType<
        typeof useEnvironmentDictionary
    >);
    render(
        <MemoryRouter initialEntries={[`/dictionaries/${dictionary.id}`]}>
            <Routes>
                <Route path="dictionaries/:dictionaryId" element={<DictionaryDetailPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

async function chooseAction(key: string, action: string) {
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: `Actions for ${key}` }));
    await user.click(screen.getByRole('menuitem', { name: action }));
    return user;
}

function lastUpdate() {
    return mutateAsync.mock.calls.at(-1)?.[0];
}

describe('DictionaryDetailPage property encryption', () => {
    beforeEach(() => {
        mutateAsync.mockReset().mockResolvedValue(undefined);
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const idle: any = { mutateAsync: jest.fn(), isPending: false };
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        jest.mocked(useUpdateDictionary).mockReturnValue({ mutateAsync, isPending: false } as any);
        jest.mocked(useDeployDictionary).mockReturnValue(idle);
        jest.mocked(useStartDictionary).mockReturnValue(idle);
        jest.mocked(useStopDictionary).mockReturnValue(idle);
    });

    describe('manual dictionary', () => {
        it('masks encrypted values and never shows their plaintext', () => {
            givenDictionary(MANUAL);

            expect(screen.getByText('•'.repeat(8))).not.toBeNull();
            expect(screen.queryByText(MASK)).toBeNull();
            expect(within(screen.getByRole('row', { name: /TOKEN/ })).getByText('Encrypted')).not.toBeNull();
            expect(within(screen.getByRole('row', { name: /PLAIN/ })).getByText('Unencrypted')).not.toBeNull();
        });

        it('encrypts an existing plaintext value without changing it', async () => {
            givenDictionary(MANUAL);

            await chooseAction('PLAIN', 'Encrypt value');

            await waitFor(() =>
                expect(lastUpdate()).toEqual({
                    dictionaryId: 'd1',
                    data: {
                        name: 'Secrets',
                        description: 'Manual dictionary',
                        type: 'MANUAL',
                        properties: { PLAIN: 'visible', TOKEN: MASK },
                        propertyOptions: { PLAIN: { encryptable: true } },
                    },
                }),
            );
        });

        it('renews an encrypted value with a new plaintext', async () => {
            givenDictionary(MANUAL);

            const user = await chooseAction('TOKEN', 'Renew encryption');
            await user.type(screen.getByLabelText(/^Value/), 'n3w-s3cret');
            await user.click(screen.getByRole('button', { name: 'Renew' }));

            await waitFor(() =>
                expect(lastUpdate().data).toEqual(
                    expect.objectContaining({
                        properties: { PLAIN: 'visible', TOKEN: 'n3w-s3cret' },
                        propertyOptions: { TOKEN: { encryptable: true } },
                    }),
                ),
            );
        });

        it('adds a property that is encrypted on save', async () => {
            givenDictionary(MANUAL);
            const user = userEvent.setup();

            await user.click(screen.getByRole('button', { name: 'Add Property' }));
            await user.type(screen.getByLabelText(/^Key/), 'NEW');
            await user.click(screen.getByRole('switch', { name: 'Encrypt on save' }));
            await user.type(screen.getByLabelText(/^Value/), 's3cret');
            await user.click(screen.getByRole('button', { name: 'Add' }));

            await waitFor(() =>
                expect(lastUpdate().data).toEqual(
                    expect.objectContaining({
                        properties: { PLAIN: 'visible', TOKEN: MASK, NEW: 's3cret' },
                        propertyOptions: { NEW: { encryptable: true } },
                    }),
                ),
            );
        });

        it('keeps encrypted values untouched when another property is deleted', async () => {
            givenDictionary(MANUAL);

            const user = await chooseAction('PLAIN', 'Delete');
            await user.click(screen.getByRole('button', { name: 'Delete' }));

            await waitFor(() => expect(lastUpdate().data.properties).toEqual({ TOKEN: MASK }));
            expect(lastUpdate().data.propertyOptions).toBeUndefined();
        });

        it('offers no property actions to a user who cannot update', () => {
            givenDictionary(MANUAL, { canUpdate: false });

            expect(screen.queryByRole('button', { name: 'Add Property' })).toBeNull();
            expect(screen.queryByRole('button', { name: /Actions for/ })).toBeNull();
            expect(screen.getByText('•'.repeat(8))).not.toBeNull();
        });
    });
});
