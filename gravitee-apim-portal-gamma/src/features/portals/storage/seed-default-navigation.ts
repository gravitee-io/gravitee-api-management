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
import { createDummyNavigation, createDummyPageContents } from './dummy-navigation';
import { saveNavItem } from './navigation-items.storage';
import { savePageContent } from './page-contents.storage';
import { seedRichAbcFitnessPages } from './rich-abc-fitness-pages';
import { seedRichPaymentPages } from './rich-payment-pages';

export async function seedDefaultNavigationForPortal(portalId: string): Promise<void> {
    const navItems = createDummyNavigation(portalId);
    const pageContents = createDummyPageContents(portalId, navItems);

    await Promise.all(navItems.map(item => saveNavItem(item)));
    await Promise.all(pageContents.map(content => savePageContent(content)));

    await seedRichPaymentPages(portalId);
    // await seedRichAbcFitnessPages(portalId);
}
