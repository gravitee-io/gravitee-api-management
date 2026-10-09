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
import { workspaceListBreadcrumb } from './workspace-breadcrumbs';

describe('workspaceListBreadcrumb', () => {
  it('omits the link on the list page', () => {
    expect(workspaceListBreadcrumb()).toEqual({
      id: 'workspaces',
      label: 'My Workspace',
      url: undefined,
    });
  });

  it('links back to the list from a detail page', () => {
    expect(workspaceListBreadcrumb(true).url).toBe('/dashboard/workspaces');
  });
});
