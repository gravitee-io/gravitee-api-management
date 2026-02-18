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
import { isFunction } from 'lodash';

import {
  PortalNavigationPage,
  PortalNavigationFolder,
  PortalNavigationLink,
  PortalNavigationApi,
  NewPagePortalNavigationItem,
  NewFolderPortalNavigationItem,
  NewLinkPortalNavigationItem,
  NewApiPortalNavigationItem,
  UpdatePagePortalNavigationItem,
  UpdateLinkPortalNavigationItem,
  UpdateFolderPortalNavigationItem,
} from './portalNavigationItem';
import { PortalNavigationItemsResponse } from './portalNavigationItemsResponse';

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
    visibility: 'PUBLIC',
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
    visibility: 'PUBLIC',
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
    visibility: 'PUBLIC',
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
    title: 'API',
    type: 'API',
    order: 1,
    area: 'TOP_NAVBAR',
    apiId: 'api-1',
    published: true,
    visibility: 'PUBLIC',
  };

  if (isFunction(overrides)) {
    return overrides(base);
  }

  return {
    ...base,
    ...overrides,
  };
}

export function fakePortalNavigationItemsResponse(overrides?: Partial<PortalNavigationItemsResponse>): PortalNavigationItemsResponse {
  const base: PortalNavigationItemsResponse = {
    items: [fakePortalNavigationPage()],
  };

  if (isFunction(overrides)) {
    return overrides(base);
  }

  return {
    ...base,
    ...overrides,
  };
}

export function fakeNewPagePortalNavigationItem(overrides?: Partial<NewPagePortalNavigationItem>): NewPagePortalNavigationItem {
  const base: NewPagePortalNavigationItem = {
    title: 'New Page',
    type: 'PAGE',
    area: 'HOMEPAGE',
    visibility: 'PUBLIC',
  };

  if (isFunction(overrides)) {
    return overrides(base);
  }

  return {
    ...base,
    ...overrides,
  };
}

export function fakeNewFolderPortalNavigationItem(overrides?: Partial<NewFolderPortalNavigationItem>): NewFolderPortalNavigationItem {
  const base: NewFolderPortalNavigationItem = {
    title: 'New Folder',
    type: 'FOLDER',
    area: 'HOMEPAGE',
    visibility: 'PUBLIC',
  };

  if (isFunction(overrides)) {
    return overrides(base);
  }

  return {
    ...base,
    ...overrides,
  };
}

export function fakeNewLinkPortalNavigationItem(overrides?: Partial<NewLinkPortalNavigationItem>): NewLinkPortalNavigationItem {
  const base: NewLinkPortalNavigationItem = {
    title: 'New Link',
    type: 'LINK',
    area: 'HOMEPAGE',
    url: 'https://example.com',
    visibility: 'PUBLIC',
  };

  if (isFunction(overrides)) {
    return overrides(base);
  }

  return {
    ...base,
    ...overrides,
  };
}

export function fakeNewApiPortalNavigationItem(overrides?: Partial<NewApiPortalNavigationItem>): NewApiPortalNavigationItem {
  const base: NewApiPortalNavigationItem = {
    title: 'New API',
    type: 'API',
    area: 'TOP_NAVBAR',
    apiId: 'api-1',
    visibility: 'PUBLIC',
  };

  if (isFunction(overrides)) {
    return overrides(base);
  }

  return {
    ...base,
    ...overrides,
  };
}

export function fakeUpdatePagePortalNavigationItem(overrides?: Partial<UpdatePagePortalNavigationItem>): UpdatePagePortalNavigationItem {
  const base: UpdatePagePortalNavigationItem = {
    published: false,
    type: 'PAGE',
    title: 'Updated Page',
    visibility: 'PUBLIC',
  };

  if (isFunction(overrides)) {
    return overrides(base);
  }

  return {
    ...base,
    ...overrides,
  };
}

export function fakeUpdateLinkPortalNavigationItem(overrides?: Partial<UpdateLinkPortalNavigationItem>): UpdateLinkPortalNavigationItem {
  const base: UpdateLinkPortalNavigationItem = {
    published: false,
    type: 'LINK',
    title: 'Updated Link',
    visibility: 'PUBLIC',
    url: 'https://updated-example.com',
  };

  if (isFunction(overrides)) {
    return overrides(base);
  }

  return {
    ...base,
    ...overrides,
  };
}

export function fakeUpdateFolderPortalNavigationItem(
  overrides?: Partial<UpdateFolderPortalNavigationItem>,
): UpdateFolderPortalNavigationItem {
  const base: UpdateFolderPortalNavigationItem = {
    published: false,
    type: 'FOLDER',
    title: 'Updated Folder',
    visibility: 'PUBLIC',
  };

  if (isFunction(overrides)) {
    return overrides(base);
  }

  return {
    ...base,
    ...overrides,
  };
}
