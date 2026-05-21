import type { BreadcrumbEntry } from '@gravitee/graphene-core';
import type { NavigateFunction } from 'react-router-dom';

/**
 * Duplicate of gravitee-gamma-control-plane-webui/src/shared/breadcrumbs/buildLinearBreadcrumbs.ts —
 * TODO(graphene): replace this file with `import { buildLinearBreadcrumbs } from '@gravitee/graphene-core'` when available.
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
