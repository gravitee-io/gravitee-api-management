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
import type { ComponentType } from 'react';

import { ApplicationDetailPlaceholderPage } from '../features/applications/components/detail';
import { ApplicationDetailGeneralPage } from '../pages/ApplicationDetailGeneralPage';
import { ApplicationDetailSubscriptionsPage } from '../pages/ApplicationDetailSubscriptionsPage';

/** Tab paths with a dedicated page implementation (keys drive routing and landing redirect). */
export const APPLICATION_DETAIL_PAGES = {
    general: ApplicationDetailGeneralPage,
    subscriptions: ApplicationDetailSubscriptionsPage,
} as const satisfies Record<string, ComponentType>;

export type ApplicationDetailImplementedPath = keyof typeof APPLICATION_DETAIL_PAGES;

export const APPLICATION_IMPLEMENTED_DETAIL_PATHS = new Set<string>(Object.keys(APPLICATION_DETAIL_PAGES));

export function applicationDetailTabElement(path: string, label: string) {
    const Page = APPLICATION_DETAIL_PAGES[path as ApplicationDetailImplementedPath];
    if (Page) {
        return <Page />;
    }
    return <ApplicationDetailPlaceholderPage title={label} />;
}
