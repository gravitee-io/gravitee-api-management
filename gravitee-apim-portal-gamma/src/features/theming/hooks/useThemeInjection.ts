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
import { useEffect } from 'react';

import { computeCssVars } from '../engine/compute-css-vars';
import type { PortalThemeDocument } from '../types';

export function useThemeInjection(
    rootEl: HTMLElement | null,
    theme: PortalThemeDocument | null | undefined,
    resolvedDark: boolean,
    ready = true,
): void {
    useEffect(() => {
        if (!ready || !rootEl || !theme) {
            return;
        }

        const vars = computeCssVars(theme, resolvedDark);

        for (const [prop, value] of vars) {
            rootEl.style.setProperty(prop, value);
        }

        if (resolvedDark) {
            rootEl.classList.add('dark');
        } else {
            rootEl.classList.remove('dark');
        }

        return () => {
            for (const prop of vars.keys()) {
                rootEl.style.removeProperty(prop);
            }
            rootEl.classList.remove('dark');
        };
    }, [rootEl, theme, resolvedDark, ready]);
}
