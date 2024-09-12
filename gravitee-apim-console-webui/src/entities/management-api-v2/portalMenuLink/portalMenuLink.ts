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

import { Pagination } from '../pagination';
import { Links } from '../links';

export const PORTAL_MENU_LINK_TYPES = ['EXTERNAL'] as const;
export type PortalMenuLinkType = (typeof PORTAL_MENU_LINK_TYPES)[number];
export const toReadableMenuLinkType = (menuLinkType: PortalMenuLinkType): string => {
  switch (menuLinkType) {
    case 'EXTERNAL':
      return 'Website';
  }
};

export const PORTAL_MENU_LINK_VISIBILITIES = ['PUBLIC', 'PRIVATE'] as const;
export type PortalMenuLinkVisibility = (typeof PORTAL_MENU_LINK_VISIBILITIES)[number];
export const toReadableMenuLinkVisibility = (menuLinkVisibility: PortalMenuLinkVisibility): string => {
  switch (menuLinkVisibility) {
    case 'PUBLIC':
      return 'Public';
    case 'PRIVATE':
      return 'Private';
  }
};

export interface PortalMenuLink {
  id: string;
  name: string;
  type: PortalMenuLinkType;
  target?: string;
  visibility: PortalMenuLinkVisibility;
  order: number;
}

export interface PortalMenuLinksResponse {
  data: PortalMenuLink[];
  pagination: Pagination;
  links: Links;
}

export interface CreatePortalMenuLink {
  name: string;
  type: PortalMenuLinkType;
  target?: string;
  visibility: PortalMenuLinkVisibility;
}

export interface UpdatePortalMenuLink {
  name: string;
  target?: string;
  visibility: PortalMenuLinkVisibility;
  order: number;
}
