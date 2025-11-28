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
export type PortalNavigationItemType = 'PAGE' | 'FOLDER' | 'LINK';

interface BasePortalNavigationItem<T extends PortalNavigationItemType> {
  id: string;
  organizationId: string;
  environmentId: string;
  title: string;
  type: T;
  order: number;
  area: PortalArea;
  published: boolean;
  parentId?: string;
}

export interface PortalNavigationPage extends BasePortalNavigationItem<'PAGE'> {
  portalPageContentId: string;
}

export interface PortalNavigationFolder extends BasePortalNavigationItem<'FOLDER'> {}

export interface PortalNavigationLink extends BasePortalNavigationItem<'LINK'> {
  url: string;
}

export type PortalNavigationItem = PortalNavigationPage | PortalNavigationFolder | PortalNavigationLink;

interface BaseNewPortalNavigationItem<T extends PortalNavigationItemType> {
  title: string;
  type: T;
  area: PortalArea;
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

export type NewPortalNavigationItem = NewPagePortalNavigationItem | NewFolderPortalNavigationItem | NewLinkPortalNavigationItem;

interface BaseUpdatePortalNavigationItem<T extends PortalNavigationItemType> {
  title: string;
  type: T;
  parentId?: string;
  order?: number;
}

export interface UpdatePagePortalNavigationItem extends BaseUpdatePortalNavigationItem<'PAGE'> {}

export interface UpdateFolderPortalNavigationItem extends BaseUpdatePortalNavigationItem<'FOLDER'> {}

export interface UpdateLinkPortalNavigationItem extends BaseUpdatePortalNavigationItem<'LINK'> {
  url: string;
}

export type UpdatePortalNavigationItem = UpdatePagePortalNavigationItem | UpdateFolderPortalNavigationItem | UpdateLinkPortalNavigationItem;
