/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { isFunction } from 'lodash';

import { PortalNavigationPage, PortalNavigationFolder, PortalNavigationLink, PortalNavigationApi } from './portal-navigation-item';

export function fakePortalNavigationPage(overrides?: Partial<PortalNavigationPage>): PortalNavigationPage {
  const base: PortalNavigationPage = {
    id: 'nav-item-1',
    organizationId: 'org-1',
    environmentId: 'env-1',
    title: 'Home',
    type: 'PAGE',
    order: 1,
    area: 'HOMEPAGE',
    portalPageContentId: 'page-content-1',
    published: true,
  };

  if (isFunction(overrides)) {
    return overrides(base);
  }

  return {
    ...base,
    ...overrides,
  };
}

export function fakePortalNavigationFolder(overrides?: Partial<PortalNavigationFolder>): PortalNavigationFolder {
  const base: PortalNavigationFolder = {
    id: 'nav-folder-1',
    organizationId: 'org-1',
    environmentId: 'env-1',
    title: 'Folder',
    type: 'FOLDER',
    order: 1,
    area: 'HOMEPAGE',
    published: true,
  };

  if (isFunction(overrides)) {
    return overrides(base);
  }

  return {
    ...base,
    ...overrides,
  };
}

export function fakePortalNavigationLink(overrides?: Partial<PortalNavigationLink>): PortalNavigationLink {
  const base: PortalNavigationLink = {
    id: 'nav-link-1',
    organizationId: 'org-1',
    environmentId: 'env-1',
    title: 'External Link',
    type: 'LINK',
    order: 1,
    area: 'HOMEPAGE',
    url: 'https://example.com',
    published: true,
  };

  if (isFunction(overrides)) {
    return overrides(base);
  }

  return {
    ...base,
    ...overrides,
  };
}

export function fakePortalNavigationApi(overrides?: Partial<PortalNavigationApi>): PortalNavigationApi {
  const base: PortalNavigationApi = {
    id: 'nav-api-1',
    organizationId: 'org-1',
    environmentId: 'env-1',
    title: 'API 1',
    type: 'API',
    order: 1,
    area: 'HOMEPAGE',
    apiId: 'api-1',
    published: true,
  };

  if (isFunction(overrides)) {
    return overrides(base);
  }

  return {
    ...base,
    ...overrides,
  };
}
