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
import { FileTextIcon, GithubIcon, GitlabIcon, GlobeIcon } from '@gravitee/graphene-core/icons';
import type { ReactNode } from 'react';

import { getNavTypeIcon } from '../../portal-shell/utils/nav-type-icons';
import type { OpenApiSpecSource } from '../../portals/types';

export type SpecSourceType = OpenApiSpecSource['type'];

export const SPEC_SOURCE_TYPES: readonly SpecSourceType[] = ['INLINE', 'API', 'HTTP', 'GITHUB', 'GITLAB'];

export const SPEC_SOURCE_LABELS: Record<SpecSourceType, string> = {
    INLINE: 'Inline',
    API: 'API',
    HTTP: 'HTTP',
    GITHUB: 'GitHub',
    GITLAB: 'GitLab',
};

export function isRemoteSpecSourceType(type: SpecSourceType): type is 'HTTP' | 'GITHUB' | 'GITLAB' {
    return type === 'HTTP' || type === 'GITHUB' || type === 'GITLAB';
}

export function isRemoteSpecSource(
    specSource: OpenApiSpecSource,
): specSource is Extract<OpenApiSpecSource, { type: 'HTTP' | 'GITHUB' | 'GITLAB' }> {
    return isRemoteSpecSourceType(specSource.type);
}

export function getSpecSourceIcon(type: SpecSourceType): ReactNode {
    switch (type) {
        case 'INLINE':
            return <FileTextIcon className="size-3.5 shrink-0" aria-hidden="true" />;
        case 'API':
            return (
                <span className="text-muted-foreground" aria-hidden="true">
                    {getNavTypeIcon('API')}
                </span>
            );
        case 'HTTP':
            return <GlobeIcon className="size-3.5 shrink-0" aria-hidden="true" />;
        case 'GITHUB':
            return <GithubIcon className="size-3.5 shrink-0" aria-hidden="true" />;
        case 'GITLAB':
            return <GitlabIcon className="size-3.5 shrink-0" aria-hidden="true" />;
    }
}
