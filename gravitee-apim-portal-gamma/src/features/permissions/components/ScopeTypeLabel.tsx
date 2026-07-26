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
    BoxesIcon,
    GlobeIcon,
    SparklesIcon,
    WaypointsIcon,
    type LucideIcon,
} from '@gravitee/graphene-core/icons';

import {
    PORTAL_GRANT_SCOPE_TYPE_LABELS,
    type PortalGrantScopeType,
} from '../types/permissions.types';

export const PORTAL_GRANT_SCOPE_TYPE_ICONS: Record<PortalGrantScopeType, LucideIcon> = {
    PORTAL: GlobeIcon,
    API: WaypointsIcon,
    API_PRODUCT: BoxesIcon,
    AI_WORKSPACE: SparklesIcon,
};

interface ScopeTypeLabelProps {
    readonly scopeType: PortalGrantScopeType;
    readonly className?: string;
    /** When false, render only the icon (e.g. dense table cells). */
    readonly showLabel?: boolean;
}

export function ScopeTypeLabel({
    scopeType,
    className,
    showLabel = true,
}: ScopeTypeLabelProps) {
    const Icon = PORTAL_GRANT_SCOPE_TYPE_ICONS[scopeType];
    const label = PORTAL_GRANT_SCOPE_TYPE_LABELS[scopeType];

    return (
        <span className={`inline-flex items-center gap-2 ${className ?? ''}`}>
            <Icon className="size-4 shrink-0" aria-hidden />
            {showLabel ? <span>{label}</span> : <span className="sr-only">{label}</span>}
        </span>
    );
}
