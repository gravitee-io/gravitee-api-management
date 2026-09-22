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

/** Builds the URL to the portal-next editor for the given environment. */
export function buildPortalNextEditorUrl(consoleUrl: string, envHrid: string): string {
    const origin = consoleUrl.endsWith('/') ? consoleUrl.slice(0, -1) : consoleUrl;
    return `${origin}/#!/${encodeURIComponent(envHrid)}/_portal/navigation`;
}

/** Replaces the current page with the portal-next editor in the classic console. */
export function redirectToPortalNextEditor(consoleUrl: string, envHrid: string): void {
    window.location.replace(buildPortalNextEditorUrl(consoleUrl, envHrid));
}
