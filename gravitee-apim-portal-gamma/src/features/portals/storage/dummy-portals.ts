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
import type { DeveloperPortal } from '../types';

export function createPlaceholderScreenshot(label: string, bgColor: string): string {
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="300" height="180">
  <rect width="300" height="180" fill="${bgColor}"/>
  <text x="150" y="90" text-anchor="middle" fill="white" font-family="sans-serif" font-size="14">${label}</text>
</svg>`;
    return `data:image/svg+xml,${encodeURIComponent(svg)}`;
}

export function createDefaultPortalScreenshot(name: string): string {
    return createPlaceholderScreenshot(name, '#64748b');
}

export function createDummyPortals(): DeveloperPortal[] {
    const now = new Date().toISOString();

    return [
        {
            id: 'portal-payments',
            name: 'Payments API Portal',
            screenshotDataUrl: createPlaceholderScreenshot('Payments API', '#2563eb'),
            updatedAt: now,
        },
        {
            id: 'portal-internal',
            name: 'Internal Dev Portal',
            screenshotDataUrl: createPlaceholderScreenshot('Internal Dev', '#059669'),
            updatedAt: now,
        },
        {
            id: 'portal-partners',
            name: 'Partner Sandbox',
            screenshotDataUrl: createPlaceholderScreenshot('Partner Sandbox', '#7c3aed'),
            updatedAt: now,
        },
    ];
}
