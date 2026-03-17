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
import { PortalNavigationItem } from '../entities/portal-navigation/portal-navigation-item';
import {
  fakePortalNavigationApi,
  fakePortalNavigationFolder,
  fakePortalNavigationLink,
  fakePortalNavigationPage,
} from '../entities/portal-navigation/portal-navigation-item.fixture';

export const makeItem = (
  id: string,
  type: PortalNavigationItem['type'],
  title: string,
  order?: number,
  parentId?: string | null,
  rootId?: string,
): PortalNavigationItem => {
  const root = rootId ?? id;
  switch (type) {
    case 'FOLDER':
      return fakePortalNavigationFolder({ id, title, order, parentId, rootId: root });
    case 'LINK':
      return fakePortalNavigationLink({ id, title, order, parentId, rootId: root });
    case 'API':
      return fakePortalNavigationApi({ id, title, order, parentId, rootId: root, apiId: `api-${id}` });
    case 'PAGE':
    default:
      return fakePortalNavigationPage({ id, title, order, parentId, rootId: root });
  }
};

export const MOCK_ITEMS = [
  makeItem('f1', 'FOLDER', 'Folder 1', 0), // root
  makeItem('f2', 'FOLDER', 'Folder 2', 0, 'f1', 'f1'),
  makeItem('p1', 'PAGE', 'Page 1', 0, 'f2', 'f1'),
  makeItem('p2', 'PAGE', 'Page 2', 1, 'f1', 'f1'),
  makeItem('p3', 'PAGE', 'Page 3', 1), // root
  makeItem('api1', 'API', 'API 1', 0, 'f1', 'f1'),
  makeItem('p-api1', 'PAGE', 'API 1 Documentation', 0, 'api1'),
];
