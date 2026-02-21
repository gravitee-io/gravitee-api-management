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

export type PortalArea = 'HOMEPAGE' | 'TOP_NAVBAR';

export type PortalNavigationItemType = 'PAGE' | 'FOLDER' | 'LINK' | 'API';

export interface BasePortalNavigationItem {
  id: string;
  organizationId: string;
  environmentId: string;
  title: string;
  type: PortalNavigationItemType;
  area: PortalArea;
  parentId?: string | null;
  order: number;
  published: boolean;
}

export interface PortalNavigationPage extends BasePortalNavigationItem {
  type: 'PAGE';
  portalPageContentId: string;
}

export interface PortalNavigationFolder extends BasePortalNavigationItem {
  type: 'FOLDER';
}

export interface PortalNavigationLink extends BasePortalNavigationItem {
  type: 'LINK';
  url: string;
}

export interface PortalNavigationApi extends BasePortalNavigationItem {
  type: 'API';
  apiId: string;
}

export type PortalNavigationItem = PortalNavigationPage | PortalNavigationFolder | PortalNavigationLink | PortalNavigationApi;
