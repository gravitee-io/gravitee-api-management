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
import { buildChunkLoadError, buildEntryExecutionError, buildModuleCodeError } from '../../testing/factories';

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
        const chunkError = buildChunkLoadError();
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
            .mockRejectedValueOnce(new Error('[ Federation Runtime ]: Failed to get remoteEntry exports. #RUNTIME-001'))
            .mockRejectedValueOnce(buildChunkLoadError())
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

    describe('files loaded by a failed attempt', () => {
        const page = globalThis as Record<string, unknown>;
        const LISTS = ['webpackChunkaim', 'rspackChunkaim', 'rspackChunkesm'];

        afterEach(() => {
            LISTS.forEach(name => delete page[name]);
        });

        it.each(['webpackChunkaim', 'rspackChunkaim'])('should not be reused by the next attempt (%s)', async listName => {
            const files: unknown[] = [[[92], {}]];
            page[listName] = files;
            const filesAtEachAttempt: number[] = [];
            mockLoadRemote.mockImplementation(() => {
                filesAtEachAttempt.push(files.length);
                return filesAtEachAttempt.length === 1 ? Promise.reject(buildChunkLoadError()) : Promise.resolve(REMOTE_EXPORT);
            });

            const loading = loadRemoteModule(MODULE);
            await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS[0]);
            await loading;

            expect(filesAtEachAttempt).toEqual([1, 0]);
        });

        it('should keep the list itself, which the copies of the module already running still use', async () => {
            const files: unknown[] = [[[92], {}]];
            const receive = jest.fn();
            files.push = receive;
            page.rspackChunkaim = files;
            mockLoadRemote.mockRejectedValueOnce(buildChunkLoadError()).mockResolvedValue(REMOTE_EXPORT);

            const loading = loadRemoteModule(MODULE);
            await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS[0]);
            await loading;

            expect(page.rspackChunkaim).toBe(files);
            expect(files.push).toBe(receive);
        });

        it('should leave the files of the other modules alone', async () => {
            const otherModuleFiles: unknown[] = [[[735], {}]];
            page.rspackChunkesm = otherModuleFiles;
            mockLoadRemote.mockRejectedValueOnce(buildChunkLoadError()).mockResolvedValue(REMOTE_EXPORT);

            const loading = loadRemoteModule(MODULE);
            await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS[0]);
            await loading;

            expect(otherModuleFiles).toHaveLength(1);
        });
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
            .mockRejectedValueOnce(buildChunkLoadError())
            .mockRejectedValueOnce(buildChunkLoadError())
            .mockResolvedValue(REMOTE_EXPORT);

        const loading = loadRemoteModule(MODULE, onStatus);
        await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS[0] + RETRY_DELAYS_MS[1]);

        await expect(loading).resolves.toBe(REMOTE_EXPORT);
        expect(onStatus).not.toHaveBeenCalled();
    });

    it('should report the load as delayed once the next attempt is at least 10 s away', async () => {
        const onStatus = jest.fn();
        mockLoadRemote.mockRejectedValue(buildChunkLoadError());

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
            () =>
                new Promise((_resolve, reject) =>
                    setTimeout(
                        () => reject(new Error('[ Federation Runtime ]: Failed to load script resources. #RUNTIME-008')),
                        DELAY_REPORTED_AFTER_MS,
                    ),
                ),
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
        const chunkError = buildChunkLoadError();
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
        const chunkError = buildChunkLoadError();
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
        const lastError = buildChunkLoadError();
        mockLoadRemote.mockRejectedValue(lastError);

        const loading = loadRemoteModule(MODULE);
        const rejection = expect(loading).rejects.toBe(lastError);
        await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS.reduce((total, delay) => total + delay, 0));
        await rejection;

        expect(mockLoadRemote).toHaveBeenCalledTimes(ATTEMPTS);
        expect(error).toHaveBeenCalledWith(expect.stringContaining(`"aim" after ${ATTEMPTS} attempts`), lastError);
    });

    it.each([
        ['a chunk', buildChunkLoadError()],
        ['a stylesheet', Object.assign(new Error('Loading CSS chunk 9086 failed.'), { code: 'CSS_CHUNK_LOAD_FAILED' })],
        ['the entry file', new Error('[ Federation Runtime ]: Failed to load script resources. #RUNTIME-008')],
        ['the exports of the entry file', new Error('[ Federation Runtime ]: Failed to get remoteEntry exports. #RUNTIME-001')],
        ['the manifest', new Error('[ Federation Runtime ]: Failed to get manifest. #RUNTIME-003')],
    ])('should keep retrying while %s cannot be fetched', async (_file, fetchError) => {
        mockLoadRemote.mockRejectedValue(fetchError);

        const loading = loadRemoteModule(MODULE);
        const rejection = expect(loading).rejects.toBe(fetchError);
        await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS.reduce((total, delay) => total + delay, 0));
        await rejection;

        expect(mockLoadRemote).toHaveBeenCalledTimes(ATTEMPTS);
    });

    describe('when the code of the module fails while it starts', () => {
        it.each([
            ['a chunk of it throws', buildModuleCodeError()],
            ['its entry file throws while it runs', buildEntryExecutionError()],
        ])('should try once more when %s, then give up without reporting a delay', async (_failure, codeError) => {
            const onStatus = jest.fn();
            mockLoadRemote.mockRejectedValue(codeError);

            const loading = loadRemoteModule(MODULE, onStatus);
            const rejection = expect(loading).rejects.toBe(codeError);
            await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS[0]);
            await rejection;

            expect(mockLoadRemote).toHaveBeenCalledTimes(2);
            expect(onStatus).not.toHaveBeenCalled();
        });

        it('should open the module when the next attempt starts it', async () => {
            mockLoadRemote.mockRejectedValueOnce(buildModuleCodeError()).mockResolvedValue(REMOTE_EXPORT);

            const loading = loadRemoteModule(MODULE);
            await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS[0]);

            await expect(loading).resolves.toBe(REMOTE_EXPORT);
        });

        it('should keep retrying while its files also fail to be fetched in between', async () => {
            mockLoadRemote
                .mockRejectedValueOnce(buildModuleCodeError())
                .mockRejectedValueOnce(buildChunkLoadError())
                .mockRejectedValueOnce(buildModuleCodeError())
                .mockResolvedValue(REMOTE_EXPORT);

            const loading = loadRemoteModule(MODULE);
            await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS[0] + RETRY_DELAYS_MS[1] + RETRY_DELAYS_MS[2]);

            await expect(loading).resolves.toBe(REMOTE_EXPORT);
            expect(mockLoadRemote).toHaveBeenCalledTimes(4);
        });

        it('should not blame an update in its logs', async () => {
            const codeError = buildModuleCodeError();
            mockLoadRemote.mockRejectedValue(codeError);

            const loading = loadRemoteModule(MODULE);
            const rejection = expect(loading).rejects.toBe(codeError);
            await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS[0]);
            await rejection;

            const logs = [...warn.mock.calls, ...error.mock.calls].map(([message]) => String(message));
            expect(logs).toHaveLength(2);
            logs.forEach(message => expect(message).not.toMatch(/updat/i));
        });
    });
});
