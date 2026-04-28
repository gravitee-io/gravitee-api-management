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
import { analyticsListBreadcrumb } from './analytics-breadcrumbs';

describe('analyticsListBreadcrumb', () => {
  it('should_return_breadcrumb_without_url_by_default', () => {
    expect(analyticsListBreadcrumb()).toEqual({ id: 'analytics', label: 'Analytics', url: undefined });
  });

  it('should_return_breadcrumb_with_url_when_includeLink_is_true', () => {
    expect(analyticsListBreadcrumb(true)).toEqual({ id: 'analytics', label: 'Analytics', url: '/dashboard/analytics' });
  });
});
