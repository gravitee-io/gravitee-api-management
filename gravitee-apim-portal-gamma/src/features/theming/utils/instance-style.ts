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
import type { CSSProperties } from 'react';

import { sanitizeCustomVarName } from '../registry/var-names';

export function toInstanceInlineStyle(
    instanceStyle?: Record<string, string>,
): CSSProperties | undefined {
    if (!instanceStyle || Object.keys(instanceStyle).length === 0) {
        return undefined;
    }

    return Object.fromEntries(
        Object.entries(instanceStyle).map(([prop, varName]) => [
            `--_portal-instance-${prop}`,
            `var(--portal-custom-${sanitizeCustomVarName(varName)})`,
        ]),
    );
}

export interface CustomizeInstanceTarget {
    readonly instanceId: string;
    /** True when bindings are stored on the block's `instanceStyle` prop (content blocks). */
    readonly usesBlockStorage: boolean;
}

export function resolveCustomizeInstanceTarget(
    element: HTMLElement,
    elementId: string,
): CustomizeInstanceTarget {
    const explicitInstanceId = element.getAttribute('data-instance-id');
    if (explicitInstanceId) {
        return { instanceId: explicitInstanceId, usesBlockStorage: false };
    }

    const blockId = element.closest('[data-node-type]')?.getAttribute('data-id') ?? undefined;
    if (blockId) {
        return { instanceId: blockId, usesBlockStorage: true };
    }

    return { instanceId: `shell:${elementId}`, usesBlockStorage: false };
}

/** @deprecated Use {@link resolveCustomizeInstanceTarget} */
export function resolveCustomizeInstanceId(
    element: HTMLElement,
    elementId: string,
    blockId?: string,
): string {
    const explicitId = element.getAttribute('data-instance-id');
    if (explicitId) {
        return explicitId;
    }
    if (blockId) {
        return blockId;
    }
    return `shell:${elementId}`;
}

/** @deprecated Use {@link CustomizeInstanceTarget.usesBlockStorage} */
export function isBlockInstanceId(instanceId: string): boolean {
    return !instanceId.startsWith('shell:');
}
