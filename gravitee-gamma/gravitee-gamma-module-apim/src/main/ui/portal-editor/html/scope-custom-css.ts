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

const BLOCKED_SELECTORS = [':root', 'html', 'body'];

export function scopeCustomCss(css: string, scopeSelector: string): string {
    if (!css.trim()) {
        return '';
    }

    const rules = css.split('}').map(chunk => chunk.trim()).filter(Boolean);
    const scoped: string[] = [];

    for (const rule of rules) {
        const braceIndex = rule.indexOf('{');
        if (braceIndex === -1) continue;

        const selectorPart = rule.slice(0, braceIndex).trim();
        const bodyPart = rule.slice(braceIndex + 1).trim();

        if (selectorPart.startsWith('@')) {
            scoped.push(`${selectorPart} { ${bodyPart} }`);
            continue;
        }

        if (BLOCKED_SELECTORS.some(blocked => selectorPart.includes(blocked))) {
            continue;
        }

        const selectors = selectorPart.split(',').map(s => s.trim()).filter(Boolean);
        const prefixed = selectors
            .map(sel => `${scopeSelector} ${sel}`)
            .join(', ');
        scoped.push(`${prefixed} { ${bodyPart} }`);
    }

    return scoped.join('\n');
}
