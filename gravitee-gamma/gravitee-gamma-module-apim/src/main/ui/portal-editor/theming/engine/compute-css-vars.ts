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
import { FOUNDATION_BASELINE_KEYS, resolveFoundation } from '../defaults/foundation-defaults';
import type { ElementModeTokens, ElementTokens, PortalThemeDocument } from '../model/theme-document';
import { POC_ELEMENT_REGISTRY, resolvePartCssVariant } from '../registry/element-registry';
import { resolveSizeValue } from '../registry/size-presets';
import { buildElementVarName, customVarCssName, foundationTokenToCssVar } from '../registry/var-names';

function isElementModeTokens(value: ElementTokens | ElementModeTokens): value is ElementModeTokens {
    return 'light' in value && 'dark' in value;
}

function getElementOverrides(
    doc: PortalThemeDocument,
    elementId: string,
    variant: string | undefined,
    mode: 'light' | 'dark',
): Record<string, string> {
    const elementEntry = doc.elements[elementId];
    if (!elementEntry) {
        return {};
    }

    if (variant) {
        const variantEntry = (elementEntry as Record<string, ElementModeTokens>)[variant];
        return variantEntry?.[mode] ?? {};
    }

    if (isElementModeTokens(elementEntry)) {
        return elementEntry[mode] ?? {};
    }

    return {};
}

export function computeCssVars(doc: PortalThemeDocument, isDark: boolean): Map<string, string> {
    const vars = new Map<string, string>();
    const mode = isDark ? 'dark' : 'light';
    const foundationOverrides = doc.foundation[mode] ?? {};
    const foundation = resolveFoundation(foundationOverrides, mode);

    for (const [key, value] of Object.entries(foundation)) {
        const foundationKey = key as keyof typeof foundation;
        const isExplicit = foundationKey in foundationOverrides;
        if (!isExplicit && !FOUNDATION_BASELINE_KEYS.has(foundationKey)) {
            continue;
        }
        const cssValue = resolveSizeValue(key, String(value));
        vars.set(foundationTokenToCssVar(key), cssValue);
    }

    for (const element of POC_ELEMENT_REGISTRY) {
        if (element.parts?.length) {
            for (const part of element.parts) {
                const overrides = getElementOverrides(doc, element.id, part.id, mode);
                const cssVariant = resolvePartCssVariant(part.id);
                for (const [prop, rawValue] of Object.entries(overrides)) {
                    const cssValue = resolveSizeValue(prop, rawValue);
                    vars.set(buildElementVarName(element.id, cssVariant, prop), cssValue);
                }
            }
        } else if (element.variants) {
            for (const variant of element.variants) {
                const overrides = getElementOverrides(doc, element.id, variant, mode);
                for (const [prop, rawValue] of Object.entries(overrides)) {
                    const cssValue = resolveSizeValue(prop, rawValue);
                    vars.set(buildElementVarName(element.id, variant, prop), cssValue);
                }
            }
        } else {
            const overrides = getElementOverrides(doc, element.id, undefined, mode);
            for (const [prop, rawValue] of Object.entries(overrides)) {
                const cssValue = resolveSizeValue(prop, rawValue);
                vars.set(buildElementVarName(element.id, undefined, prop), cssValue);
            }
        }
    }

    for (const cv of doc.customVariables) {
        vars.set(customVarCssName(cv.name), isDark ? cv.darkValue : cv.lightValue);
    }

    return vars;
}
