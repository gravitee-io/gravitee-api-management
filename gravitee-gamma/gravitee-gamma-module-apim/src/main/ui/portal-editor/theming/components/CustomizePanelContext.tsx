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
import { createContext, useContext } from 'react';

import { getElementDef } from '../registry/element-registry';
import { resolveCustomizeInstanceTarget } from '../utils/instance-style';

export interface CustomizeTarget {
    element: HTMLElement;
    elementId: string;
    variant?: string;
    instanceId: string;
    usesBlockStorage: boolean;
}

export interface CustomizePanelController {
    openCustomizePanel: (element: HTMLElement, position: { x: number; y: number }) => void;
}

export const CustomizePanelContext = createContext<CustomizePanelController | null>(null);

export function useCustomizePanel(): CustomizePanelController | null {
    return useContext(CustomizePanelContext);
}

export function resolveCustomizeTarget(element: HTMLElement): CustomizeTarget | null {
    const styleTarget = element.closest('[data-style-target]') as HTMLElement | null;
    if (!styleTarget) {
        return null;
    }

    const elementId = styleTarget.getAttribute('data-style-target') ?? '';
    const variant = styleTarget.getAttribute('data-style-variant')
        ?? styleTarget.getAttribute('data-style-part')
        ?? undefined;
    if (!getElementDef(elementId)) {
        return null;
    }

    const { instanceId, usesBlockStorage } = resolveCustomizeInstanceTarget(styleTarget, elementId);
    return { element: styleTarget, elementId, variant, instanceId, usesBlockStorage };
}
