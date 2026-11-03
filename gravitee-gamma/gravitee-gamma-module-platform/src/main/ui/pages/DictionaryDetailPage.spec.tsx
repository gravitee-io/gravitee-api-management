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

const TWO_PLAIN: Dictionary = { ...MANUAL, properties: { PLAIN: 'visible', OTHER: 'also visible', TOKEN: MASK } };

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
                <Route path="/" element={<p>Dictionary list</p>} />
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

async function markForEncryption(key: string) {
    return chooseAction(key, 'Encrypt');
}

async function stageNewProperty(key: string, value: string) {
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Add Property' }));
    await user.type(screen.getByLabelText(/^Key/), key);
    await user.click(screen.getByRole('switch', { name: 'Encrypt on save' }));
    await user.type(screen.getByLabelText(/^Value/), value);
    await user.click(screen.getByRole('button', { name: 'Add' }));
    return user;
}

async function saveMarked(user: ReturnType<typeof userEvent.setup>) {
    await user.click(screen.getByRole('button', { name: 'Save' }));
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
        it('marks a plaintext value to be encrypted on save without calling the API', async () => {
            givenDictionary(MANUAL);

            await markForEncryption('PLAIN');

            expect(within(screen.getByRole('row', { name: /PLAIN/ })).getByText('Encrypted on save')).not.toBeNull();
            expect(screen.getByText(/^1 value will be encrypted on save\./)).not.toBeNull();
            expect(mutateAsync).not.toHaveBeenCalled();
        });

        it('encrypts nothing once the mark is undone', async () => {
            givenDictionary(MANUAL);

            const user = await markForEncryption('PLAIN');
            await user.click(screen.getByRole('button', { name: 'Actions for PLAIN' }));
            await user.click(screen.getByRole('menuitem', { name: 'Do not encrypt' }));

            expect(within(screen.getByRole('row', { name: /PLAIN/ })).getByText('Unencrypted')).not.toBeNull();
            expect(screen.queryByRole('button', { name: 'Save' })).toBeNull();
            expect(mutateAsync).not.toHaveBeenCalled();
        });

        it('drops every mark on Discard', async () => {
            givenDictionary(TWO_PLAIN);

            const user = await markForEncryption('PLAIN');
            await user.click(screen.getByRole('button', { name: 'Actions for OTHER' }));
            await user.click(screen.getByRole('menuitem', { name: 'Encrypt' }));
            expect(screen.getByText('2 values will be encrypted on save.')).not.toBeNull();

            await user.click(screen.getByRole('button', { name: 'Discard' }));

            expect(screen.queryByRole('button', { name: 'Save' })).toBeNull();
            expect(mutateAsync).not.toHaveBeenCalled();
        });

        it('saves only the marked keys, onto the re-read dictionary rather than the cached one', async () => {
            givenDictionary(MANUAL);
            freshCopy = { ...MANUAL, properties: { PLAIN: 'visible', TOKEN: MASK, ADDED: 'by-provider' } };

            const user = await markForEncryption('PLAIN');
            await saveMarked(user);

            await waitFor(() =>
                expect(lastUpdate()).toEqual({
                    dictionaryId: 'd1',
                    data: {
                        name: 'Secrets',
                        description: 'Manual dictionary',
                        type: 'MANUAL',
                        properties: { PLAIN: 'visible', TOKEN: MASK, ADDED: 'by-provider' },
                        propertyOptions: { PLAIN: { encryptable: true } },
                    },
                }),
            );
            await waitFor(() => expect(screen.queryByRole('button', { name: 'Save' })).toBeNull());
        });

        it('fails instead of saving when a marked property no longer exists', async () => {
            givenDictionary(MANUAL);
            freshCopy = { ...MANUAL, properties: { TOKEN: MASK } };

            const user = await markForEncryption('PLAIN');
            await saveMarked(user);

            await waitFor(() => expect(mutateAsync).toHaveBeenCalled());
            expect(() => lastUpdate()).toThrow('no longer exists');
        });

        it('reports a failed save and keeps the marks', async () => {
            mutateAsync.mockRejectedValueOnce(new Error('boom'));
            givenDictionary(MANUAL);

            const user = await markForEncryption('PLAIN');
            await saveMarked(user);

            await waitFor(() => expect(notify.error).toHaveBeenCalledWith(expect.any(Error), 'Failed to encrypt properties'));
            expect(screen.getByText(/^1 value will be encrypted on save\./)).not.toBeNull();
        });

        it('replaces a value with the new plaintext and sends only that key, leaving other marks staged', async () => {
            givenDictionary(TWO_PLAIN);

            const user = await markForEncryption('PLAIN');
            await user.click(screen.getByRole('button', { name: 'Actions for OTHER' }));
            await user.click(screen.getByRole('menuitem', { name: 'Encrypt' }));
            await user.click(screen.getByRole('button', { name: 'Actions for PLAIN' }));
            await user.click(screen.getByRole('menuitem', { name: 'Replace value' }));
            await user.type(screen.getByLabelText(/^Value/), 'n3w-s3cret');
            await user.click(screen.getByRole('button', { name: 'Replace' }));

            await waitFor(() =>
                expect(lastUpdate().data).toEqual(
                    expect.objectContaining({
                        properties: { PLAIN: 'n3w-s3cret', OTHER: 'also visible', TOKEN: MASK },
                        propertyOptions: { PLAIN: { encryptable: true } },
                    }),
                ),
            );
            expect(screen.getByText(/^1 value will be encrypted on save\./)).not.toBeNull();
        });

        it('replaces an encrypted value with a new plaintext', async () => {
            givenDictionary(MANUAL);

            const user = await chooseAction('TOKEN', 'Replace value');
            await user.type(screen.getByLabelText(/^Value/), 'n3w-s3cret');
            await user.click(screen.getByRole('button', { name: 'Replace' }));

            await waitFor(() =>
                expect(lastUpdate().data).toEqual(
                    expect.objectContaining({
                        properties: { PLAIN: 'visible', TOKEN: 'n3w-s3cret' },
                        propertyOptions: { TOKEN: { encryptable: true } },
                    }),
                ),
            );
        });

        it('reports a failed replacement and keeps the sheet open', async () => {
            mutateAsync.mockRejectedValueOnce(new Error('boom'));
            givenDictionary(MANUAL);

            const user = await chooseAction('TOKEN', 'Replace value');
            await user.type(screen.getByLabelText(/^Value/), 'n3w');
            await user.click(screen.getByRole('button', { name: 'Replace' }));

            await waitFor(() => expect(notify.error).toHaveBeenCalledWith(expect.any(Error), 'Failed to replace property value'));
            expect(screen.getByText('Replace Value')).not.toBeNull();
        });

        it('drops the mark of a property that is deleted', async () => {
            givenDictionary(TWO_PLAIN);

            const user = await markForEncryption('PLAIN');
            await user.click(screen.getByRole('button', { name: 'Actions for PLAIN' }));
            await user.click(screen.getByRole('menuitem', { name: 'Delete' }));
            await user.click(screen.getByRole('button', { name: 'Delete' }));

            await waitFor(() => expect(lastUpdate().data.properties).toEqual({ OTHER: 'also visible', TOKEN: MASK }));
            expect(lastUpdate().data.propertyOptions).toBeUndefined();
            expect(screen.queryByRole('button', { name: 'Save' })).toBeNull();
        });

        it('masks encrypted values with twelve bullets and badges each row', () => {
            givenDictionary(MANUAL);

            expect(screen.getByText('•'.repeat(12))).not.toBeNull();
            expect(within(screen.getByRole('row', { name: /TOKEN/ })).getByText('Encrypted')).not.toBeNull();
            expect(within(screen.getByRole('row', { name: /PLAIN/ })).getByText('Unencrypted')).not.toBeNull();
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

        it('stages a new property that is encrypted on save until Save', async () => {
            givenDictionary(MANUAL);

            const user = await stageNewProperty('NEW', 's3cret');

            expect(mutateAsync).not.toHaveBeenCalled();
            expect(within(screen.getByRole('row', { name: /NEW/ })).getByText('Encrypted on save')).not.toBeNull();
            expect(screen.queryByText('s3cret')).toBeNull();
            expect(screen.getByText(/^1 value will be encrypted on save\./)).not.toBeNull();

            await saveMarked(user);

            await waitFor(() =>
                expect(lastUpdate().data).toEqual(
                    expect.objectContaining({
                        properties: { PLAIN: 'visible', TOKEN: MASK, NEW: 's3cret' },
                        propertyOptions: { NEW: { encryptable: true } },
                    }),
                ),
            );
        });

        it('does not let a plain property be renamed onto a staged new key', async () => {
            givenDictionary(MANUAL);
            await stageNewProperty('NEW', 's3cret');

            const user = await chooseAction('PLAIN', 'Edit value');
            const key = screen.getByLabelText(/^Key/);
            await user.clear(key);
            await user.type(key, 'NEW');
            await user.click(screen.getByRole('button', { name: 'Update' }));

            expect(notify.error).toHaveBeenCalledWith('Property key "NEW" already exists');
            expect(mutateAsync).not.toHaveBeenCalled();
        });

        it('fails Save, instead of overwriting, when a staged key now exists on the server', async () => {
            givenDictionary(MANUAL);
            freshCopy = { ...MANUAL, properties: { ...MANUAL.properties, NEW: 'added elsewhere' } };

            const user = await stageNewProperty('NEW', 's3cret');
            await saveMarked(user);

            expect(() => lastUpdate()).toThrow('Property "NEW" already exists');
        });

        it('fails the undo of a staged new property when the key now exists on the server', async () => {
            givenDictionary(MANUAL);
            freshCopy = { ...MANUAL, properties: { ...MANUAL.properties, NEW: 'added elsewhere' } };
            await stageNewProperty('NEW', 's3cret');

            await chooseAction('NEW', 'Do not encrypt');

            await waitFor(() => expect(mutateAsync).toHaveBeenCalled());
            expect(() => lastUpdate()).toThrow('Property "NEW" already exists');
        });

        it('leaves through Back to Dictionaries without asking when nothing is staged', async () => {
            givenDictionary(MANUAL);

            await userEvent.setup().click(screen.getByRole('link', { name: 'Back to Dictionaries' }));

            expect(screen.getByText('Dictionary list')).not.toBeNull();
        });

        it('asks before leaving through Back to Dictionaries while something is staged', async () => {
            givenDictionary(MANUAL);
            const user = await markForEncryption('PLAIN');

            await user.click(screen.getByRole('link', { name: 'Back to Dictionaries' }));
            expect(screen.getByText('Unsaved changes')).not.toBeNull();
            expect(screen.queryByText('Dictionary list')).toBeNull();

            await user.click(screen.getByRole('button', { name: 'Leave' }));
            expect(screen.getByText('Dictionary list')).not.toBeNull();
        });

        it('warns before the page is closed or reloaded while an encryption is staged', async () => {
            givenDictionary(MANUAL);
            expect(window.dispatchEvent(new Event('beforeunload', { cancelable: true }))).toBe(true);

            await markForEncryption('PLAIN');

            expect(window.dispatchEvent(new Event('beforeunload', { cancelable: true }))).toBe(false);
        });

        it('keeps a staged new property as plain when its encryption is undone', async () => {
            givenDictionary(MANUAL);
            await stageNewProperty('NEW', 's3cret');

            const user = userEvent.setup();
            await user.click(screen.getByRole('button', { name: 'Actions for NEW' }));
            await user.click(screen.getByRole('menuitem', { name: 'Do not encrypt' }));

            await waitFor(() =>
                expect(lastUpdate().data).toEqual(
                    expect.objectContaining({ properties: { PLAIN: 'visible', TOKEN: MASK, NEW: 's3cret' } }),
                ),
            );
            expect(lastUpdate().data.propertyOptions).toBeUndefined();
            await waitFor(() => expect(screen.queryByText(/will be encrypted on save/)).toBeNull());
        });

        it('keeps a staged new property when saving its undo fails', async () => {
            givenDictionary(MANUAL);
            mutateAsync.mockRejectedValue(new Error('boom'));
            await stageNewProperty('NEW', 's3cret');

            const user = userEvent.setup();
            await user.click(screen.getByRole('button', { name: 'Actions for NEW' }));
            await user.click(screen.getByRole('menuitem', { name: 'Do not encrypt' }));

            await waitFor(() => expect(notify.error).toHaveBeenCalledWith(expect.any(Error), 'Failed to add property'));
            expect(screen.getByText('NEW')).not.toBeNull();
        });

        it('warns that Discard removes a staged new property, and drops it', async () => {
            givenDictionary(MANUAL);
            const user = await stageNewProperty('NEW', 's3cret');

            expect(screen.getByText(/Discard also removes the new property you added\./)).not.toBeNull();
            await user.click(screen.getByRole('button', { name: 'Discard' }));

            expect(screen.queryByText('NEW')).toBeNull();
            expect(mutateAsync).not.toHaveBeenCalled();
        });

        it('replaces the value of a staged new property without calling the API', async () => {
            givenDictionary(MANUAL);
            await stageNewProperty('NEW', 's3cret');

            const user = await chooseAction('NEW', 'Replace value');
            await user.type(screen.getByLabelText(/^Value/), 'n3w');
            await user.click(screen.getByRole('button', { name: 'Replace' }));
            expect(mutateAsync).not.toHaveBeenCalled();

            await saveMarked(user);
            await waitFor(() => expect(lastUpdate().data.properties).toEqual(expect.objectContaining({ NEW: 'n3w' })));
        });

        it('removes a staged new property locally when it is deleted', async () => {
            givenDictionary(MANUAL);
            await stageNewProperty('NEW', 's3cret');

            const user = await chooseAction('NEW', 'Delete');
            await user.click(screen.getByRole('button', { name: 'Delete' }));

            expect(screen.queryByText('NEW')).toBeNull();
            expect(mutateAsync).not.toHaveBeenCalled();
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
            expect(screen.getByText('•'.repeat(12))).not.toBeNull();
        });
    });

    describe('dynamic dictionary', () => {
        it('offers no Add Property, and only Encrypt on a plain value', async () => {
            givenDictionary(DYNAMIC);

            expect(screen.queryByRole('button', { name: 'Add Property' })).toBeNull();
            expect(screen.queryByRole('button', { name: 'Actions for b' })).toBeNull();
            const user = userEvent.setup();
            await user.click(screen.getByRole('button', { name: 'Actions for a' }));
            expect(screen.getAllByRole('menuitem').map(item => item.textContent)).toEqual(['Encrypt']);
        });

        it('stages an encryption and saves it with the provider and trigger kept', async () => {
            givenDictionary(DYNAMIC);

            const user = await markForEncryption('a');
            expect(within(screen.getByRole('row', { name: /^a / })).getByText('Encrypted on save')).not.toBeNull();
            expect(mutateAsync).not.toHaveBeenCalled();

            await saveMarked(user);

            await waitFor(() =>
                expect(lastUpdate().data).toEqual(
                    expect.objectContaining({
                        type: 'DYNAMIC',
                        properties: { a: '1', b: MASK },
                        propertyOptions: { a: { encryptable: true } },
                        provider: DYNAMIC.provider,
                        trigger: DYNAMIC.trigger,
                    }),
                ),
            );
        });

        it('offers no action to a user who cannot update', () => {
            givenDictionary(DYNAMIC, { canUpdate: false });

            expect(screen.queryByRole('button', { name: /Actions for/ })).toBeNull();
        });

        it('shows the encryption state and the Dynamic badge of each synced value', () => {
            givenDictionary(DYNAMIC);

            expect(within(screen.getByRole('row', { name: /^b / })).getByText('Encrypted')).not.toBeNull();
            expect(within(screen.getByRole('row', { name: /^a / })).getByText('Unencrypted')).not.toBeNull();
            expect(within(screen.getByRole('row', { name: /^a / })).getByText('Dynamic')).not.toBeNull();
        });

        it('shows the Encrypt fetched values policy read-only', () => {
            givenDictionary(DYNAMIC);

            expect(screen.getByTestId('encrypt-on-fetch').textContent).toBe('Yes');
        });
    });
});
