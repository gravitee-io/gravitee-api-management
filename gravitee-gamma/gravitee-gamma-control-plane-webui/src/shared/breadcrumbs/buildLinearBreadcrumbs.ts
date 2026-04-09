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
import type { BreadcrumbEntry } from '@gravitee/graphene';
import type { NavigateFunction } from 'react-router-dom';

/**
 * Builds a linear breadcrumb trail for ContentHeader / useLayoutConfig.
 * Signature matches the utility we intend to ship in @gravitee/graphene later — replace this import when available.
 */
export function buildLinearBreadcrumbs(
    navigate: NavigateFunction,
    segments: Array<{ readonly label: string; readonly to?: string }>,
): BreadcrumbEntry[] {
    if (segments.length === 0) {
        return [];
    }
    return segments.map((segment, index) => {
        const isLast = index === segments.length - 1;
        if (isLast) {
            return { label: segment.label };
        }
        if (segment.to === undefined) {
            return { label: segment.label };
        }
        const to = segment.to;
        return {
            label: segment.label,
            onClick: () => {
                void navigate(to);
            },
        };
    });
}
