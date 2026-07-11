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
import { toPng } from 'html-to-image';

import { createDefaultPortalScreenshot } from '../storage/dummy-portals';

const THUMBNAIL_MAX_WIDTH = 480;

export function shouldIncludeInPortalScreenshot(node: Node): boolean {
    if (!(node instanceof HTMLElement)) {
        return true;
    }

    return !node.classList.contains('monaco-editor');
}

function buildScreenshotOptions(element: HTMLElement) {
    const sourceWidth = element.clientWidth || element.offsetWidth;
    const captureWidth = sourceWidth > 0 ? Math.min(sourceWidth, THUMBNAIL_MAX_WIDTH) : THUMBNAIL_MAX_WIDTH;
    const scale = sourceWidth > 0 ? captureWidth / sourceWidth : 1;
    const sourceHeight = element.offsetHeight;
    const captureHeight = sourceHeight > 0 ? Math.round(sourceHeight * scale) : undefined;

    return {
        cacheBust: false,
        pixelRatio: 1,
        skipFonts: true,
        width: captureWidth,
        ...(captureHeight ? { height: captureHeight } : {}),
        ...(scale < 1
            ? {
                  style: {
                      transform: `scale(${scale})`,
                      transformOrigin: 'top left',
                  },
              }
            : {}),
        filter: shouldIncludeInPortalScreenshot,
    };
}

export async function capturePortalScreenshot(element: HTMLElement, fallbackName: string): Promise<string> {
    try {
        return await toPng(element, buildScreenshotOptions(element));
    } catch {
        return createDefaultPortalScreenshot(fallbackName);
    }
}
