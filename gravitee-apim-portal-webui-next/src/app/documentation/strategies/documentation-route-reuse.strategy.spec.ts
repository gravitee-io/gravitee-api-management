/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { ActivatedRouteSnapshot } from '@angular/router';

import { DocumentationRouteReuseStrategy } from './documentation-route-reuse.strategy';

function createRoute(
  routeConfig: {
    path: string;
  },
  params: Partial<{
    pathParam: string;
    queryParam: string | null;
  }> = {
    pathParam: '1',
    queryParam: '2',
  },
) {
  return {
    routeConfig,
    parent: {
      routeConfig: {
        path: 'documentation',
      },
    },
    paramMap: new Map<string, string | undefined>([['navId', params.pathParam]]),
    queryParamMap: new Map<string, string | null | undefined>([['pageId', params.queryParam]]),
  } as unknown as ActivatedRouteSnapshot;
}

describe('test route reuse', () => {
  let strategy: DocumentationRouteReuseStrategy;

  beforeEach(() => {
    strategy = new DocumentationRouteReuseStrategy();
  });

  describe('should not reuse route', () => {
    it('when pageId was reset', () => {
      const routeConfig = { path: ':navId' };
      const currentRoute = createRoute(routeConfig);
      const futureRoute = createRoute(routeConfig, {
        pathParam: '1',
        queryParam: null,
      });

      expect(strategy.shouldReuseRoute(futureRoute, currentRoute)).toEqual(false);
    });
  });

  describe('should fallback to default behavior', () => {
    it('when nothing has changed', () => {
      const routeConfig = { path: ':navId' };
      const currentRoute = createRoute(routeConfig);
      const futureRoute = createRoute(routeConfig);

      expect(strategy.shouldReuseRoute(futureRoute, currentRoute)).toEqual(true);
    });

    it('when path has changed', () => {
      const routeConfig = { path: ':navId' };
      const currentRoute = createRoute(routeConfig);
      const futureRoute = createRoute(routeConfig, {
        pathParam: '11',
        queryParam: null,
      });

      expect(strategy.shouldReuseRoute(futureRoute, currentRoute)).toEqual(true);
    });
  });
});
