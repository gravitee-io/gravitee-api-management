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
import type { PortalPageContentType } from '../portalPageContent/portalPageContent';

export type PortalArea = 'HOMEPAGE' | 'TOP_NAVBAR';
export type PortalNavigationItemType = 'PAGE' | 'FOLDER' | 'LINK' | 'API' | 'API_PRODUCT';
export type PortalVisibility = 'PUBLIC' | 'PRIVATE';

export interface PortalNavigationItemSource {
  type: string;
  configuration: unknown;
  useAutoFetch?: boolean;
  fetchCron?: string;
  /** Read-only, server-managed. Date of the last successful fetch. */
  lastFetchedAt?: string;
  /** Read-only, server-managed. Date of the last fetch attempt, successful or not. Dates lastFetchError. */
  lastFetchAttemptAt?: string;
  /** Read-only, server-managed. Absent when the last fetch succeeded. */
  lastFetchError?: string;
}

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
  source?: PortalNavigationItemSource;
}

export interface PortalNavigationFolder extends BasePortalNavigationItem<'FOLDER'> {
  source?: PortalNavigationItemSource;
}

export interface PortalNavigationLink extends BasePortalNavigationItem<'LINK'> {
  url: string;
}

export interface PortalNavigationApi extends BasePortalNavigationItem<'API'> {
  apiId: string;
  categoryIds?: string[];
}

export interface PortalNavigationApiProduct extends BasePortalNavigationItem<'API_PRODUCT'> {
  apiProductId: string;
}

export type PortalNavigationItem =
  | PortalNavigationPage
  | PortalNavigationFolder
  | PortalNavigationLink
  | PortalNavigationApi
  | PortalNavigationApiProduct;

/** Only PAGE and FOLDER items can carry an external source. */
export function getPortalNavigationItemSource(item: PortalNavigationItem): PortalNavigationItemSource | undefined {
  return item.type === 'PAGE' || item.type === 'FOLDER' ? item.source : undefined;
}

/**
 * Ids of the items carrying at least one sourced PAGE below them — exactly what the backend `_fetch`
 * endpoint walks. An item carrying a source itself but no sourced PAGE below it is absent from the
 * set: the endpoint rejects it with a 400.
 */
export function collectNodeIdsWithSourcedPageDescendants(items: PortalNavigationItem[] | null | undefined): Set<string> {
  const nodeIds = new Set<string>();

  if (!items || !Array.isArray(items)) {
    return nodeIds;
  }

  const itemsById = new Map<string, PortalNavigationItem>(items.map(item => [item.id, item]));

  for (const item of items) {
    if (item.type !== 'PAGE' || !item.source) {
      continue;
    }
    let ancestor = item.parentId ? itemsById.get(item.parentId) : undefined;
    // Stopping on an already-marked ancestor both short-circuits shared chains and ends parentId cycles
    while (ancestor && !nodeIds.has(ancestor.id)) {
      nodeIds.add(ancestor.id);
      ancestor = ancestor.parentId ? itemsById.get(ancestor.parentId) : undefined;
    }
  }

  return nodeIds;
}

export interface PortalNavigationItemFetchResult {
  navigationItemId: string;
  title: string;
  success: boolean;
  /** Why the fetch failed. Absent when the fetch succeeded. */
  error?: string;
}

export interface PortalNavigationItemsFetchSummary {
  succeeded: number;
  failed: number;
  results: PortalNavigationItemFetchResult[];
}

/** Exactly one of item or summary is set: item for a sourced PAGE, summary for a container's sourced descendants. */
export interface FetchPortalNavigationItemResponse {
  item?: PortalNavigationItem;
  summary?: PortalNavigationItemsFetchSummary;
}

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
  contentType?: PortalPageContentType;
  source?: PortalNavigationItemSource;
}

export interface NewFolderPortalNavigationItem extends BaseNewPortalNavigationItem<'FOLDER'> {
  source?: PortalNavigationItemSource;
}

export interface NewLinkPortalNavigationItem extends BaseNewPortalNavigationItem<'LINK'> {
  url: string;
}

export interface NewApiPortalNavigationItem extends BaseNewPortalNavigationItem<'API'> {
  apiId: string;
  categoryIds?: string[];
}

export interface NewApiProductPortalNavigationItem extends BaseNewPortalNavigationItem<'API_PRODUCT'> {
  apiProductId: string;
}

export type NewPortalNavigationItem =
  | NewPagePortalNavigationItem
  | NewFolderPortalNavigationItem
  | NewLinkPortalNavigationItem
  | NewApiPortalNavigationItem
  | NewApiProductPortalNavigationItem;

interface BaseUpdatePortalNavigationItem<T extends PortalNavigationItemType> {
  title: string;
  type: T;
  published: boolean;
  visibility: PortalVisibility;
  parentId?: string;
  order?: number;
}

export interface UpdatePagePortalNavigationItem extends BaseUpdatePortalNavigationItem<'PAGE'> {
  source?: PortalNavigationItemSource;
}

export interface UpdateFolderPortalNavigationItem extends BaseUpdatePortalNavigationItem<'FOLDER'> {
  source?: PortalNavigationItemSource;
}

export interface UpdateLinkPortalNavigationItem extends BaseUpdatePortalNavigationItem<'LINK'> {
  url: string;
}

export interface UpdateApiPortalNavigationItem extends BaseUpdatePortalNavigationItem<'API'> {
  apiId: string;
  categoryIds?: string[];
}

export interface UpdateApiProductPortalNavigationItem extends BaseUpdatePortalNavigationItem<'API_PRODUCT'> {}

export type UpdatePortalNavigationItem =
  | UpdatePagePortalNavigationItem
  | UpdateFolderPortalNavigationItem
  | UpdateLinkPortalNavigationItem
  | UpdateApiPortalNavigationItem
  | UpdateApiProductPortalNavigationItem;
