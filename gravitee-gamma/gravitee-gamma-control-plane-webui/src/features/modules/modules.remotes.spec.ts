/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { DELAY_REPORTED_AFTER_MS, loadRemoteModule, registerModuleRemotes, RETRY_DELAYS_MS, retryModuleNow } from './modules.remotes';
import type { GammaModule } from './modules.types';

const mockLoadRemote = jest.fn();
const mockRegisterRemotes = jest.fn();
jest.mock('@module-federation/runtime', () => ({
    loadRemote: (...args: unknown[]) => mockLoadRemote(...args),
    registerRemotes: (...args: unknown[]) => mockRegisterRemotes(...args),
}));

const MODULE: GammaModule = {
    id: 'aim',
    name: 'Agent Management',
    version: '1.14.0',
    remoteName: 'aim',
    exposedModule: 'App',
};
const GAMMA_BASE_URL = 'http://api.test/gamma';
const MANIFEST_URL = `${GAMMA_BASE_URL}/organizations/test-org/modules/aim/assets/mf-manifest.json`;
const ATTEMPTS = RETRY_DELAYS_MS.length + 1;
const REMOTE_EXPORT = { default: () => null };

describe('loadRemoteModule', () => {
    let warn: jest.SpyInstance;
    let info: jest.SpyInstance;
    let error: jest.SpyInstance;

    beforeEach(() => {
        jest.useFakeTimers();
        mockLoadRemote.mockReset();
        warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        info = jest.spyOn(console, 'info').mockImplementation(() => undefined);
        error = jest.spyOn(console, 'error').mockImplementation(() => undefined);
        registerModuleRemotes([MODULE], GAMMA_BASE_URL, 'test-org');
        mockRegisterRemotes.mockReset();
    });

    afterEach(() => {
        jest.useRealTimers();
        jest.restoreAllMocks();
    });

    it('should return the module when the first attempt succeeds', async () => {
        mockLoadRemote.mockResolvedValue(REMOTE_EXPORT);

        await expect(loadRemoteModule(MODULE)).resolves.toBe(REMOTE_EXPORT);

        expect(mockLoadRemote).toHaveBeenCalledWith('aim/App');
        expect(warn).not.toHaveBeenCalled();
        expect(mockRegisterRemotes).not.toHaveBeenCalled();
    });

    it('should wait before retrying a failed attempt, then return the module', async () => {
        const chunkError = new Error('Loading chunk 8201 failed.');
        mockLoadRemote.mockRejectedValueOnce(chunkError).mockResolvedValue(REMOTE_EXPORT);

        const loading = loadRemoteModule(MODULE);
        await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS[0] - 1);

        expect(mockLoadRemote).toHaveBeenCalledTimes(1);
        expect(warn).toHaveBeenCalledWith(expect.stringContaining(`"aim" (attempt 1 of ${ATTEMPTS})`), chunkError);

        await jest.advanceTimersByTimeAsync(1);

        await expect(loading).resolves.toBe(REMOTE_EXPORT);
        expect(mockLoadRemote).toHaveBeenCalledTimes(2);
        expect(info).toHaveBeenCalledWith(expect.stringContaining('"aim" after 2 attempts'));
    });

    it('should register the module under a manifest URL it has not used before each retry', async () => {
        mockLoadRemote
            .mockRejectedValueOnce(new Error('remoteEntryExports is undefined'))
            .mockRejectedValueOnce(new Error('Loading chunk 8201 failed.'))
            .mockResolvedValue(REMOTE_EXPORT);

        const loading = loadRemoteModule(MODULE);
        await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS[0] + RETRY_DELAYS_MS[1]);
        await loading;

        expect(mockRegisterRemotes).toHaveBeenCalledTimes(2);
        const entries = mockRegisterRemotes.mock.calls.map(([remotes, options]) => {
            expect(options).toEqual({ force: true });
            expect(remotes).toEqual([{ name: 'aim', entry: expect.stringMatching(/\?retry=/) }]);
            return remotes[0].entry as string;
        });
        entries.forEach(entry => expect(entry.startsWith(`${MANIFEST_URL}?`)).toBe(true));
        expect(new Set(entries).size).toBe(2);
    });

    it('should treat an empty result as a failed attempt', async () => {
        mockLoadRemote.mockResolvedValueOnce(undefined).mockResolvedValue(REMOTE_EXPORT);

        const loading = loadRemoteModule(MODULE);
        await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS[0]);

        await expect(loading).resolves.toBe(REMOTE_EXPORT);
        expect(warn).toHaveBeenCalledWith(
            expect.any(String),
            expect.objectContaining({ message: 'Failed to load remote module: aim/App' }),
        );
    });

    it('should not report a load that recovers within the first attempts', async () => {
        const onStatus = jest.fn();
        mockLoadRemote
            .mockRejectedValueOnce(new Error('Loading chunk 8201 failed.'))
            .mockRejectedValueOnce(new Error('Loading chunk 8201 failed.'))
            .mockResolvedValue(REMOTE_EXPORT);

        const loading = loadRemoteModule(MODULE, onStatus);
        await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS[0] + RETRY_DELAYS_MS[1]);

        await expect(loading).resolves.toBe(REMOTE_EXPORT);
        expect(onStatus).not.toHaveBeenCalled();
    });

    it('should report the load as delayed once the next attempt is at least 10 s away', async () => {
        const onStatus = jest.fn();
        mockLoadRemote.mockRejectedValue(new Error('Loading chunk 8201 failed.'));

        void loadRemoteModule(MODULE, onStatus).catch(() => undefined);
        await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS[0] + RETRY_DELAYS_MS[1] - 1);
        expect(onStatus).not.toHaveBeenCalled();

        await jest.advanceTimersByTimeAsync(1);

        expect(mockLoadRemote).toHaveBeenCalledTimes(3);
        expect(RETRY_DELAYS_MS[2]).toBeGreaterThanOrEqual(DELAY_REPORTED_AFTER_MS);
        expect(onStatus).toHaveBeenCalledWith('delayed');
    });

    it('should report attempts that fail slowly, and then wait at least 10 s before the next one', async () => {
        const onStatus = jest.fn();
        mockLoadRemote.mockImplementation(
            () => new Promise((_resolve, reject) => setTimeout(() => reject(new Error('Script load timed out')), DELAY_REPORTED_AFTER_MS)),
        );

        void loadRemoteModule(MODULE, onStatus).catch(() => undefined);
        await jest.advanceTimersByTimeAsync(DELAY_REPORTED_AFTER_MS);

        expect(onStatus).toHaveBeenCalledWith('delayed');
        await jest.advanceTimersByTimeAsync(DELAY_REPORTED_AFTER_MS - 1);
        expect(mockLoadRemote).toHaveBeenCalledTimes(1);
        await jest.advanceTimersByTimeAsync(1);
        expect(mockLoadRemote).toHaveBeenCalledTimes(2);
    });

    it('should report each attempt made once the load is delayed', async () => {
        const onStatus = jest.fn();
        const chunkError = new Error('Loading chunk 8201 failed.');
        mockLoadRemote
            .mockRejectedValueOnce(chunkError)
            .mockRejectedValueOnce(chunkError)
            .mockRejectedValueOnce(chunkError)
            .mockResolvedValue(REMOTE_EXPORT);

        const loading = loadRemoteModule(MODULE, onStatus);
        await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS[0] + RETRY_DELAYS_MS[1] + RETRY_DELAYS_MS[2]);
        await loading;

        expect(onStatus.mock.calls.map(([status]) => status)).toEqual(['delayed', 'attempting', 'ready']);
    });

    it('should start the next attempt at once when asked to retry now', async () => {
        const chunkError = new Error('Loading chunk 8201 failed.');
        mockLoadRemote
            .mockRejectedValueOnce(chunkError)
            .mockRejectedValueOnce(chunkError)
            .mockRejectedValueOnce(chunkError)
            .mockResolvedValue(REMOTE_EXPORT);

        const loading = loadRemoteModule(MODULE);
        await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS[0] + RETRY_DELAYS_MS[1]);
        expect(mockLoadRemote).toHaveBeenCalledTimes(3);

        retryModuleNow(MODULE.id);
        await jest.advanceTimersByTimeAsync(0);

        await expect(loading).resolves.toBe(REMOTE_EXPORT);
        expect(mockLoadRemote).toHaveBeenCalledTimes(4);
    });

    it('should give up with the last error once every retry has failed', async () => {
        const lastError = new Error('Loading chunk 8201 failed.');
        mockLoadRemote.mockRejectedValue(lastError);

        const loading = loadRemoteModule(MODULE);
        const rejection = expect(loading).rejects.toBe(lastError);
        await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS.reduce((total, delay) => total + delay, 0));
        await rejection;

        expect(mockLoadRemote).toHaveBeenCalledTimes(ATTEMPTS);
        expect(error).toHaveBeenCalledWith(expect.stringContaining(`"aim" after ${ATTEMPTS} attempts`), lastError);
    });
});
