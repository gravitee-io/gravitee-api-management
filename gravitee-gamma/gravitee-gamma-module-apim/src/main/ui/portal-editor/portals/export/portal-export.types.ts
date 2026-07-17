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
import type { DeveloperPortal, PageContent, PortalNavigationItem } from '../types';
import type { PortalTheme } from '../../theming/types';

export const PORTAL_EXPORT_FORMAT_VERSION = '1' as const;

export type PortalExportPortal = Omit<DeveloperPortal, 'screenshotDataUrl'>;

export interface PortalExportBundle {
    readonly formatVersion: typeof PORTAL_EXPORT_FORMAT_VERSION;
    readonly exportedAt: string;
    readonly portal: PortalExportPortal;
    readonly navigation: readonly PortalNavigationItem[];
    readonly pageContents: readonly PageContent[];
    readonly theme: PortalTheme;
}

export interface K8sResourceDocument {
    readonly apiVersion: string;
    readonly kind: string;
    readonly metadata: { readonly name: string };
    readonly spec: Record<string, unknown>;
}
