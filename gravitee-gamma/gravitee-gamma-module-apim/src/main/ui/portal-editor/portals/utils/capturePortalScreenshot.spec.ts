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
jest.mock('html-to-image', () => ({
    toPng: jest.fn(),
}));

import { toPng } from 'html-to-image';

import { createDefaultPortalScreenshot } from '../storage/dummy-portals';
import { capturePortalScreenshot, shouldIncludeInPortalScreenshot } from './capturePortalScreenshot';

const mockedToPng = toPng as jest.MockedFunction<typeof toPng>;

describe('capturePortalScreenshot', () => {
    beforeEach(() => {
        mockedToPng.mockReset();
    });

    it('should capture a downscaled thumbnail without busting image caches', async () => {
        mockedToPng.mockResolvedValue('data:image/png;base64,capture');

        const element = document.createElement('div');
        Object.defineProperty(element, 'clientWidth', { value: 1200 });
        Object.defineProperty(element, 'offsetWidth', { value: 1200 });
        Object.defineProperty(element, 'offsetHeight', { value: 800 });

        const result = await capturePortalScreenshot(element, 'Fallback Portal');

        expect(mockedToPng).toHaveBeenCalledWith(
            element,
            expect.objectContaining({
                cacheBust: false,
                pixelRatio: 1,
                skipFonts: true,
                width: 480,
                height: 320,
                filter: shouldIncludeInPortalScreenshot,
            }),
        );
        expect(result).toBe('data:image/png;base64,capture');
    });

    it('should return placeholder screenshot when capture fails', async () => {
        mockedToPng.mockRejectedValue(new Error('capture failed'));

        const element = document.createElement('div');
        const result = await capturePortalScreenshot(element, 'My Portal');

        expect(result).toBe(createDefaultPortalScreenshot('My Portal'));
    });

    it('should exclude monaco editor nodes from the capture', () => {
        const monacoRoot = document.createElement('div');
        monacoRoot.className = 'monaco-editor';

        expect(shouldIncludeInPortalScreenshot(monacoRoot)).toBe(false);
        expect(shouldIncludeInPortalScreenshot(document.createElement('div'))).toBe(true);
    });
});
