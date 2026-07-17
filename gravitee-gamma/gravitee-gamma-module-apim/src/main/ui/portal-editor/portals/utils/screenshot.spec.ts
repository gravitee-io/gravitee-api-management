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
import {
    isPlaceholderScreenshot,
    isRealPortalScreenshot,
    PORTAL_SCREENSHOT_HEIGHT,
    PORTAL_SCREENSHOT_WIDTH,
} from './screenshot';

describe('screenshot utils', () => {
    it('should use a 5:3 aspect ratio for stored thumbnails', () => {
        expect(PORTAL_SCREENSHOT_WIDTH / PORTAL_SCREENSHOT_HEIGHT).toBeCloseTo(5 / 3);
    });

    it('should treat empty values as placeholders', () => {
        expect(isPlaceholderScreenshot('')).toBe(true);
        expect(isRealPortalScreenshot('')).toBe(false);
    });

    it('should treat svg data urls as placeholders', () => {
        const svg = 'data:image/svg+xml,%3Csvg%3E%3C/svg%3E';
        expect(isPlaceholderScreenshot(svg)).toBe(true);
        expect(isRealPortalScreenshot(svg)).toBe(false);
    });

    it('should treat png data urls as real screenshots', () => {
        const png = 'data:image/png;base64,abc';
        expect(isPlaceholderScreenshot(png)).toBe(false);
        expect(isRealPortalScreenshot(png)).toBe(true);
    });
});
