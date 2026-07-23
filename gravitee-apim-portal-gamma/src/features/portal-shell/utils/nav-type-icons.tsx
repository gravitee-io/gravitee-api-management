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
import type { ReactNode } from 'react';

import type { PageContentType, PortalNavigationItemType, PortalNavigationPage } from '../../portals/types';
import { getPageContentType } from '../../portals/utils/page-content-type';

const iconProps = {
    width: 14,
    height: 14,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 2,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
};

export function getNavTypeIcon(type: PortalNavigationItemType): ReactNode {
    switch (type) {
        case 'PAGE':
            return (
                <svg {...iconProps}>
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                    <polyline points="14 2 14 8 20 8" />
                    <line x1="16" y1="13" x2="8" y2="13" />
                    <line x1="16" y1="17" x2="8" y2="17" />
                </svg>
            );
        case 'FOLDER':
            return (
                <svg {...iconProps}>
                    <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
                </svg>
            );
        case 'LINK':
            return (
                <svg {...iconProps}>
                    <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
                    <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
                </svg>
            );
        case 'API':
            return (
                <svg {...iconProps}>
                    <polyline points="16 18 22 12 16 6" />
                    <polyline points="8 6 2 12 8 18" />
                </svg>
            );
        case 'API_PRODUCT':
            return (
                <svg {...iconProps}>
                    <rect x="3" y="8" width="18" height="12" rx="2" />
                    <rect x="7" y="4" width="10" height="6" rx="1" />
                    <line x1="12" y1="12" x2="12" y2="16" />
                </svg>
            );
        case 'AI_WORKSPACE':
            return (
                <svg {...iconProps}>
                    <path d="M12 3l1.9 4.6L18.5 9.5 13.9 11.4 12 16l-1.9-4.6L5.5 9.5l4.6-1.9z" />
                    <path d="M18 15l.7 1.8L20.5 17.5 18.7 18.2 18 20l-.7-1.8L15.5 17.5l1.8-.7z" />
                </svg>
            );
    }
}

export function getPageContentTypeIcon(contentType: PageContentType): ReactNode {
    switch (contentType) {
        case 'OPENAPI':
            return (
                <svg {...iconProps}>
                    <path d="M8 3H5a2 2 0 0 0-2 2v3" />
                    <path d="M21 8V5a2 2 0 0 0-2-2h-3" />
                    <path d="M3 16v3a2 2 0 0 0 2 2h3" />
                    <path d="M16 21h3a2 2 0 0 0 2-2v-3" />
                    <path d="M7 12h10" />
                </svg>
            );
        case 'HTML':
            return (
                <svg {...iconProps}>
                    <polyline points="16 18 22 12 16 6" />
                    <polyline points="8 6 2 12 8 18" />
                </svg>
            );
        case 'ASYNCAPI':
            return (
                <svg {...iconProps}>
                    <path d="M7 7h10" />
                    <path d="M7 12h10" />
                    <path d="M7 17h10" />
                    <path d="M3 7l2 2-2 2" />
                </svg>
            );
        case 'BLOCK':
        default:
            return getNavTypeIcon('PAGE');
    }
}

export function getPageNavIcon(page: PortalNavigationPage): ReactNode {
    return getPageContentTypeIcon(getPageContentType(page));
}

export function getPageContentTypeLabel(contentType: PageContentType): string {
    switch (contentType) {
        case 'OPENAPI':
            return 'OpenAPI page';
        case 'HTML':
            return 'HTML page';
        case 'ASYNCAPI':
            return 'AsyncAPI page';
        case 'BLOCK':
        default:
            return 'Block page';
    }
}
