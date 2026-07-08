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
export type PreviewViewport = 'desktop' | 'tablet' | 'mobile';

export const PREVIEW_VIEWPORT_WIDTHS: Record<PreviewViewport, string> = {
    desktop: '100%',
    tablet: '768px',
    mobile: '390px',
};

export const PREVIEW_VIEWPORT_STORAGE_KEY = 'gravitee-portal-gamma-preview-viewport';

export function readStoredPreviewViewport(): PreviewViewport {
    const stored = localStorage.getItem(PREVIEW_VIEWPORT_STORAGE_KEY);
    if (stored === 'desktop' || stored === 'tablet' || stored === 'mobile') {
        return stored;
    }
    return 'desktop';
}
