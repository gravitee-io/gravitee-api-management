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
import { copyTextToClipboard, copyTextToClipboardWithNotify, copyTextToClipboardWithNotifyHandler } from './copyToClipboard';
import { notify } from './notify';

jest.mock('./notify', () => ({
    notify: {
        info: jest.fn(),
        error: jest.fn(),
    },
}));

const mockNotify = jest.mocked(notify);

describe('copyTextToClipboard', () => {
    afterEach(() => {
        jest.restoreAllMocks();
        jest.clearAllMocks();
    });

    it('writes text when the clipboard API is available', async () => {
        const writeText = jest.fn().mockResolvedValue(undefined);
        Object.assign(navigator, { clipboard: { writeText } });

        await copyTextToClipboard('secret');

        expect(writeText).toHaveBeenCalledWith('secret');
    });

    it('rejects when the clipboard API is unavailable', async () => {
        const clipboard = navigator.clipboard;
        Object.assign(navigator, { clipboard: undefined });

        await expect(copyTextToClipboard('secret')).rejects.toThrow('Clipboard API unavailable');

        Object.assign(navigator, { clipboard });
    });
});

describe('copyTextToClipboardWithNotify', () => {
    afterEach(() => {
        jest.restoreAllMocks();
        jest.clearAllMocks();
    });

    it('shows success toast after a successful copy', async () => {
        jest.spyOn(navigator.clipboard, 'writeText').mockResolvedValue(undefined);

        await copyTextToClipboardWithNotify('key-1', 'Copied');

        expect(mockNotify.info).toHaveBeenCalledWith('Copied');
        expect(mockNotify.error).not.toHaveBeenCalled();
    });

    it('shows error toast and rejects when copy fails', async () => {
        const denied = new Error('denied');
        jest.spyOn(navigator.clipboard, 'writeText').mockRejectedValue(denied);

        await expect(copyTextToClipboardWithNotify('key-1', 'Copied', 'Copy failed')).rejects.toBe(denied);

        expect(mockNotify.error).toHaveBeenCalledWith(denied, 'Copy failed');
        expect(mockNotify.info).not.toHaveBeenCalled();
    });
});

describe('copyTextToClipboardWithNotifyHandler', () => {
    afterEach(() => {
        jest.restoreAllMocks();
        jest.clearAllMocks();
    });

    it('delegates to the async copy helper without returning a promise', async () => {
        jest.spyOn(navigator.clipboard, 'writeText').mockResolvedValue(undefined);

        const result = copyTextToClipboardWithNotifyHandler('key-1', 'Copied');

        expect(result).toBeUndefined();

        await new Promise(resolve => setTimeout(resolve, 0));

        expect(mockNotify.info).toHaveBeenCalledWith('Copied');
    });
});
