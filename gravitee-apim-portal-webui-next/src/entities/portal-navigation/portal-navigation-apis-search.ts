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
import { PortalNavigationItem } from './portal-navigation-item';
import { Api } from '../api/api';
import { ApiLinks } from '../api/api-links';
import { Mcp } from '../api/mcp';
import { Links } from '../pagination/links';

export interface PortalNavigationApiSearchItem {
  id: string;
  name: string;
  version: string;
  description: string;
  _links?: ApiLinks;
  mcp?: Mcp;
  labels?: string[];
  rootId: string;
  navItemId: string;
}

export interface PortalNavigationSearchMetadata {
  pagination: {
    current_page: number;
    size: number;
    total: number;
    total_pages: number;
  };
}

export interface PortalNavigationItemsSearchResponse {
  data?: PortalNavigationItem[];
  apis?: Api[];
  links?: Links;
  metadata?: PortalNavigationSearchMetadata;
}

export interface PortalNavigationApisSearchResponse {
  data: PortalNavigationApiSearchItem[];
  metadata?: PortalNavigationSearchMetadata;
  links?: Links;
}
