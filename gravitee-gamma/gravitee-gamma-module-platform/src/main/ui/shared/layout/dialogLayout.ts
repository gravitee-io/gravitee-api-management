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

/*
 * The design system ships a prebuilt stylesheet, so arbitrary-value utilities such as
 * `w-[min(90rem,calc(100vw-4rem))]` are never compiled and silently do nothing.
 * Dialogs that need more room than the default therefore size themselves inline.
 */

/** Width for dialogs showing JSON source or diffs, capped so the page stays visible around it. */
export const WIDE_DIALOG_STYLE = { width: '90rem', maxWidth: 'calc(100vw - 4rem)' } as const;

/** Height of the scrollable area inside a wide dialog. */
export const WIDE_DIALOG_CONTENT_STYLE = { height: '58vh', maxHeight: '32rem' } as const;
