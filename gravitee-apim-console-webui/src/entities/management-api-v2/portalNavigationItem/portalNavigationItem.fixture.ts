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
  NewPagePortalNavigationItem,
  NewFolderPortalNavigationItem,
  NewLinkPortalNavigationItem,
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
    configuration: {
      portalPageContentId: 'page-content-1',
    },
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
    configuration: {
      url: 'https://example.com',
    },
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
    order: 1,
    contentId: 'content-1',
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
    order: 1,
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
    order: 1,
    url: 'https://example.com',
  };

  if (isFunction(overrides)) {
    return overrides(base);
  }

  return {
    ...base,
    ...overrides,
  };
}
