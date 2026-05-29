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

/** Centered, compact width for General-page Export / Duplicate dialogs. */
export const API_ACTION_DIALOG_CONTENT_CLASS = 'w-[min(calc(100vw-2rem),560px)] max-w-[560px]';

/** Overrides Graphene Dialog default max-width when Tailwind utilities lose specificity. */
export const API_ACTION_DIALOG_CONTENT_STYLE = {
    width: 'min(calc(100vw - 2rem), 560px)',
    maxWidth: 'min(calc(100vw - 2rem), 560px)',
} as const;

/** Version field — max 32 chars in API definition. */
export const API_ACTION_DIALOG_VERSION_FIELD_CLASS = 'w-full sm:w-32 shrink-0';
