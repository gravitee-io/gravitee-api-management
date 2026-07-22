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
import type { PageWidth } from '../editor/constants/page-width';

export type {
    AsyncApiSpecSource,
    BaseNavigationItem,
    OpenApiRenderer,
    OpenApiSpecSource,
    PageContentType,
    PortalNavigationApi,
    PortalNavigationApiProduct,
    PortalNavigationArea,
    PortalNavigationAsyncApiPage,
    PortalNavigationBlockPage,
    PortalNavigationFolder,
    PortalNavigationHtmlPage,
    PortalNavigationItem,
    PortalNavigationItemType,
    PortalNavigationLink,
    PortalNavigationOpenApiPage,
    PortalNavigationPage,
} from './types/navigation-item.types';

export type {
    AsyncApiPageContent,
    BlockNoteDocument,
    BlockPageContent,
    HtmlPageContent,
    OpenApiPageContent,
    PageContent,
} from './types/page-content.types';

export type { BlockStyleOverrides } from '../theming/types/block-styles.types';

export type PortalLayout = 'header-content-footer' | 'sidebar-content';

export const DEFAULT_PORTAL_LABEL = 'Developer Portal';

export interface FooterLink {
    readonly id: string;
    readonly label: string;
    readonly url: string;
}

export interface UserMenuItem {
    readonly id: string;
    readonly label: string;
    readonly url: string;
}

export type PortalDocumentationViewer = 'swagger' | 'redoc' | 'in-house';

export const PORTAL_DOCUMENTATION_VIEWERS: readonly PortalDocumentationViewer[] = [
    'swagger',
    'redoc',
    'in-house',
];

export const PORTAL_DOCUMENTATION_VIEWER_LABELS: Record<PortalDocumentationViewer, string> = {
    swagger: 'Swagger',
    redoc: 'Redoc',
    'in-house': 'Gravitee Renderer',
};

export const DEFAULT_DOCUMENTATION_VIEWER: PortalDocumentationViewer = 'swagger';

export interface DeveloperPortal {
    readonly id: string;
    readonly name: string;
    readonly description?: string;
    readonly screenshotDataUrl: string;
    readonly updatedAt: string;
    readonly layout: PortalLayout;
    readonly showFooter: boolean;
    readonly pageWidth: PageWidth;
    readonly portalIconUrl: string;
    readonly portalLabel: string;
    readonly footerLinks: readonly FooterLink[];
    readonly userMenuItems: readonly UserMenuItem[];
    /** Public URL of this developer portal. */
    readonly portalUrl?: string;
    /** Default API documentation renderer for this portal. */
    readonly documentationViewer?: PortalDocumentationViewer;
}
