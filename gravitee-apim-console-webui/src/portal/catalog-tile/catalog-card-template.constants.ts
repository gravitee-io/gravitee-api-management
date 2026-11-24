/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { PortalNavigationPage } from '../../entities/management-api-v2';

/** Console-only virtual nav node — not a content page; hosts the catalog card GMD editor. */
export const CATALOG_CARD_TEMPLATE_NAV_ID = '__portal-catalog-card-template__';

export function isCatalogCardTemplateNavId(id: string | null | undefined): boolean {
  return id === CATALOG_CARD_TEMPLATE_NAV_ID;
}

/** Shown as a Page so publish/unpublish matches other navigation pages. */
export function createCatalogCardTemplatePage(published = true): PortalNavigationPage {
  return {
    id: CATALOG_CARD_TEMPLATE_NAV_ID,
    organizationId: '',
    environmentId: '',
    title: 'Catalog',
    type: 'PAGE',
    order: -1,
    area: 'TOP_NAVBAR',
    published,
    visibility: 'PUBLIC',
    portalPageContentId: '',
  };
}
