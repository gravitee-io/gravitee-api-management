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
export type PortalArea = 'HOMEPAGE' | 'TOP_NAVBAR';
export type PortalNavigationItemType = 'PAGE' | 'FOLDER' | 'LINK' | 'API';
export type PortalVisibility = 'PUBLIC' | 'PRIVATE';

interface BasePortalNavigationItem<T extends PortalNavigationItemType> {
  id: string;
  organizationId: string;
  environmentId: string;
  title: string;
  type: T;
  order: number;
  area: PortalArea;
  published: boolean;
  visibility: PortalVisibility;
  parentId?: string;
}

export interface PortalNavigationPage extends BasePortalNavigationItem<'PAGE'> {
  portalPageContentId: string;
}

export interface PortalNavigationFolder extends BasePortalNavigationItem<'FOLDER'> {}

export interface PortalNavigationLink extends BasePortalNavigationItem<'LINK'> {
  url: string;
}

export interface PortalNavigationApi extends BasePortalNavigationItem<'API'> {
  apiId: string;
}

export type PortalNavigationItem = PortalNavigationPage | PortalNavigationFolder | PortalNavigationLink | PortalNavigationApi;

interface BaseNewPortalNavigationItem<T extends PortalNavigationItemType> {
  title: string;
  type: T;
  area: PortalArea;
  visibility: PortalVisibility;
  parentId?: string;
  order?: number;
}

export interface NewPagePortalNavigationItem extends BaseNewPortalNavigationItem<'PAGE'> {
  portalPageContentId?: string;
}

export interface NewFolderPortalNavigationItem extends BaseNewPortalNavigationItem<'FOLDER'> {}

export interface NewLinkPortalNavigationItem extends BaseNewPortalNavigationItem<'LINK'> {
  url: string;
}

export interface NewApiPortalNavigationItem extends BaseNewPortalNavigationItem<'API'> {
  apiId: string;
}

export type NewPortalNavigationItem =
  | NewPagePortalNavigationItem
  | NewFolderPortalNavigationItem
  | NewLinkPortalNavigationItem
  | NewApiPortalNavigationItem;

interface BaseUpdatePortalNavigationItem<T extends PortalNavigationItemType> {
  title: string;
  type: T;
  published: boolean;
  visibility: PortalVisibility;
  parentId?: string;
  order?: number;
}

export interface UpdatePagePortalNavigationItem extends BaseUpdatePortalNavigationItem<'PAGE'> {}

export interface UpdateFolderPortalNavigationItem extends BaseUpdatePortalNavigationItem<'FOLDER'> {}

export interface UpdateLinkPortalNavigationItem extends BaseUpdatePortalNavigationItem<'LINK'> {
  url: string;
}

export interface UpdateApiPortalNavigationItem extends BaseUpdatePortalNavigationItem<'API'> {
  apiId: string;
}

export type UpdatePortalNavigationItem =
  | UpdatePagePortalNavigationItem
  | UpdateFolderPortalNavigationItem
  | UpdateLinkPortalNavigationItem
  | UpdateApiPortalNavigationItem;
