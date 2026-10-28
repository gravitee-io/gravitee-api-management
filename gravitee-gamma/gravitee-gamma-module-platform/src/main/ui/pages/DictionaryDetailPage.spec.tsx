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
    useUpdateDictionaryProperties,
} from '../features/dictionaries/hooks/useDictionaryMutations';
import { useDictionaryPermissions } from '../features/dictionaries/hooks/useDictionaryPermissions';
import { useEnvironmentDictionary } from '../features/dictionaries/hooks/useEnvironmentDictionary';
import type { Dictionary } from '../features/dictionaries/types/dictionary';
import { notify } from '../shared/notify';

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

const DYNAMIC: Dictionary = {
    id: 'd2',
    name: 'Remote',
    type: 'DYNAMIC',
    state: 'STARTED',
    properties: { a: '1', b: MASK },
    propertyOptions: { b: { encrypted: true } },
    trigger: { rate: 5, unit: 'MINUTES' },
    provider: { type: 'HTTP', configuration: { url: 'https://example.com/codes', method: 'GET' } },
    encryption: { encryptOnFetch: true },
};

const mutateAsync = jest.fn();
// What the mutation re-reads from the server; defaults to the dictionary the page shows.
let freshCopy: Dictionary | undefined;

function givenDictionary(dictionary: Dictionary, { canUpdate = true }: { canUpdate?: boolean } = {}) {
    freshCopy = dictionary;
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

async function encryptAfterConfirming(key: string) {
    const user = await chooseAction(key, 'Encrypt value');
    await user.click(await screen.findByRole('button', { name: 'Encrypt' }));
    return user;
}

function lastUpdate() {
    const call = mutateAsync.mock.calls.at(-1)?.[0];
    return call && { dictionaryId: call.dictionaryId, data: call.change(freshCopy) };
}

describe('DictionaryDetailPage property encryption', () => {
    beforeEach(() => {
        mutateAsync.mockReset().mockResolvedValue(undefined);
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const idle: any = { mutateAsync: jest.fn(), isPending: false };
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        jest.mocked(useUpdateDictionaryProperties).mockReturnValue({ mutateAsync, isPending: false } as any);
        jest.mocked(useDeployDictionary).mockReturnValue(idle);
        jest.mocked(useStartDictionary).mockReturnValue(idle);
        jest.mocked(useStopDictionary).mockReturnValue(idle);
    });

    describe('manual dictionary', () => {
        it('applies the change to the re-read dictionary, not the cached one', async () => {
            givenDictionary(MANUAL);
            freshCopy = { ...MANUAL, properties: { PLAIN: 'visible', TOKEN: MASK, ADDED: 'by-provider' } };

            await encryptAfterConfirming('PLAIN');

            await waitFor(() => expect(lastUpdate().data.properties).toEqual({ PLAIN: 'visible', TOKEN: MASK, ADDED: 'by-provider' }));
        });

        it('sends nothing when the encryption is cancelled', async () => {
            givenDictionary(MANUAL);

            const user = await chooseAction('PLAIN', 'Encrypt value');
            await user.click(await screen.findByRole('button', { name: 'Cancel' }));

            expect(mutateAsync).not.toHaveBeenCalled();
        });

        it('fails instead of saving when the property to encrypt no longer exists', async () => {
            givenDictionary(MANUAL);
            freshCopy = { ...MANUAL, properties: { TOKEN: MASK } };

            await encryptAfterConfirming('PLAIN');

            await waitFor(() => expect(mutateAsync).toHaveBeenCalled());
            expect(() => lastUpdate()).toThrow('no longer exists');
        });

        it('edits a plaintext value without sending encryption options', async () => {
            givenDictionary(MANUAL);

            const user = await chooseAction('PLAIN', 'Edit value');
            await user.clear(screen.getByLabelText(/^Value/));
            await user.type(screen.getByLabelText(/^Value/), 'changed');
            await user.click(screen.getByRole('button', { name: 'Update' }));

            await waitFor(() => expect(lastUpdate().data.properties).toEqual({ PLAIN: 'changed', TOKEN: MASK }));
            expect(lastUpdate().data.propertyOptions).toBeUndefined();
        });

        it('reports a failed encryption', async () => {
            mutateAsync.mockRejectedValueOnce(new Error('boom'));
            givenDictionary(MANUAL);

            await encryptAfterConfirming('PLAIN');

            await waitFor(() => expect(notify.error).toHaveBeenCalledWith(expect.any(Error), 'Failed to encrypt property'));
        });

        it('reports a failed renewal and keeps the sheet open', async () => {
            mutateAsync.mockRejectedValueOnce(new Error('boom'));
            givenDictionary(MANUAL);

            const user = await chooseAction('TOKEN', 'Renew encryption');
            await user.type(screen.getByLabelText(/^Value/), 'n3w');
            await user.click(screen.getByRole('button', { name: 'Renew' }));

            await waitFor(() => expect(notify.error).toHaveBeenCalledWith(expect.any(Error), 'Failed to renew property encryption'));
            expect(screen.getByText('Renew Encryption')).not.toBeNull();
        });

        it('masks encrypted values and never shows their plaintext', () => {
            givenDictionary(MANUAL);

            expect(screen.getByText('•'.repeat(8))).not.toBeNull();
            expect(screen.queryByText(MASK)).toBeNull();
            expect(within(screen.getByRole('row', { name: /TOKEN/ })).getByText('Encrypted')).not.toBeNull();
            expect(within(screen.getByRole('row', { name: /PLAIN/ })).getByText('Unencrypted')).not.toBeNull();
        });

        it('encrypts an existing plaintext value without changing it', async () => {
            givenDictionary(MANUAL);

            await encryptAfterConfirming('PLAIN');

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

    describe('dynamic dictionary', () => {
        it('encrypts a synced value while preserving the provider configuration', async () => {
            givenDictionary(DYNAMIC);
            expect(screen.queryByRole('button', { name: 'Add Property' })).toBeNull();

            await encryptAfterConfirming('a');

            await waitFor(() =>
                expect(lastUpdate()).toEqual({
                    dictionaryId: 'd2',
                    data: {
                        name: 'Remote',
                        description: undefined,
                        type: 'DYNAMIC',
                        properties: { a: '1', b: MASK },
                        propertyOptions: { a: { encryptable: true } },
                        provider: DYNAMIC.provider,
                        trigger: DYNAMIC.trigger,
                    },
                }),
            );
        });

        it('has no action on an already encrypted synced value', () => {
            givenDictionary(DYNAMIC);

            expect(screen.queryByRole('button', { name: 'Actions for b' })).toBeNull();
            expect(within(screen.getByRole('row', { name: /^b / })).getByText('Encrypted')).not.toBeNull();
        });

        it('shows whether fetched values are encrypted', () => {
            givenDictionary(DYNAMIC);

            expect(screen.getByText('Encrypt fetched values')).not.toBeNull();
            expect(screen.getByText('Yes', { selector: '[data-testid="encrypt-on-fetch"]' })).not.toBeNull();
        });
    });
});
