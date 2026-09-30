/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export function filterChipSuggestions(suggestions: readonly string[], values: string[], draft: string): string[] {
    if (suggestions.length === 0) {
        return [];
    }
    const query = draft.trim().toLowerCase();
    return suggestions.filter(suggestion => {
        if (values.includes(suggestion)) {
            return false;
        }
        return !query || suggestion.toLowerCase().includes(query);
    });
}

export function commitChipDraft(values: string[], draft: string, onChange: (next: string[]) => void): boolean {
    const trimmed = draft.trim();
    if (!trimmed || values.includes(trimmed)) {
        return false;
    }
    onChange([...values, trimmed]);
    return true;
}
