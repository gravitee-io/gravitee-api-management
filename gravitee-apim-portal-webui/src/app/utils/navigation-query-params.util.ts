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
import { Params, PRIMARY_OUTLET, Router } from '@angular/router';

import { SearchQueryParam } from './search-query-param.enum';

/** Query params that describe where the user came from (catalog, search, application, etc.). */
export const NAVIGATION_CONTEXT_QUERY_PARAMS: readonly string[] = [
  SearchQueryParam.QUERY,
  SearchQueryParam.CATEGORY,
  SearchQueryParam.APPLICATION,
  SearchQueryParam.API_QUERY,
];

export function getNavigationContextQueryParams(queryParams: Params): Params {
  return NAVIGATION_CONTEXT_QUERY_PARAMS.reduce<Params>((acc, key) => {
    const value = queryParams[key];
    if (value != null && value !== '') {
      acc[key] = value;
    }
    return acc;
  }, {});
}

/** Parses a pagination query param string. Returns `defaultValue` when the string is absent or not a positive integer. */
export function parsePaginationParam(value: string | null, defaultValue: number): number {
  const parsed = value !== null ? parseInt(value, 10) : NaN;
  return Number.isFinite(parsed) && parsed > 0 ? parsed : defaultValue;
}

/**
 * Navigates to `routePath` while preserving the current navigation context query params.
 * Query params carried by `routePath` take precedence over the preserved context.
 */
export function navigateWithNavigationContext(router: Router, routePath: string): Promise<boolean> {
  const urlTree = router.parseUrl(routePath);
  const primary = urlTree.root.children[PRIMARY_OUTLET];
  const path = primary?.segments.map(segment => segment.toString()).join('/') ?? '';
  const queryParams = { ...getNavigationContextQueryParams(router.parseUrl(router.url).queryParams), ...urlTree.queryParams };
  return router.navigate([path], { queryParams });
}
