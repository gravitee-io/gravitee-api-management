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

interface BasePortalNavigationItem {
  id: string;
  organizationId: string;
  environmentId: string;
  title: string;
  type: PortalNavigationItemType;
  order: number;
  area: PortalArea;
  parentId?: string;
}

export interface PortalNavigationPage extends BasePortalNavigationItem {
  configuration: {
    portalPageContentId: string;
  };
}

export interface PortalNavigationFolder extends BasePortalNavigationItem {}

export interface PortalNavigationLink extends BasePortalNavigationItem {
  configuration: {
    url: string;
  };
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
  contentId?: string;
}

export interface NewFolderPortalNavigationItem extends BaseNewPortalNavigationItem<'FOLDER'> {}

export interface NewLinkPortalNavigationItem extends BaseNewPortalNavigationItem<'LINK'> {
  url: string;
}

export type NewPortalNavigationItem = NewPagePortalNavigationItem | NewFolderPortalNavigationItem | NewLinkPortalNavigationItem;
