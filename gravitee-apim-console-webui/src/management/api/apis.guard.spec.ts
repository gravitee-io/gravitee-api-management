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
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { EnvironmentInjector, runInInjectionContext } from '@angular/core';
import { Observable } from 'rxjs';

import { ApisGuard } from './apis.guard';

import { CONSTANTS_TESTING, GioTestingModule } from '../../shared/testing';

describe('ApisGuard.denyNativeApi', () => {
  let httpTestingController: HttpTestingController;
  let injector: EnvironmentInjector;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [GioTestingModule] });
    httpTestingController = TestBed.inject(HttpTestingController);
    injector = TestBed.inject(EnvironmentInjector);
  });

  afterEach(() => httpTestingController.verify());

  function activate(api: Record<string, unknown>): Promise<boolean | UrlTree> {
    const route = { params: { apiId: 'api-1' } } as unknown as ActivatedRouteSnapshot;
    const result = runInInjectionContext(injector, () => ApisGuard.denyNativeApi(route, {} as RouterStateSnapshot)) as Observable<
      boolean | UrlTree
    >;

    const promise = new Promise<boolean | UrlTree>(resolve => result.subscribe(resolve));
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/api-1`).flush({ id: 'api-1', ...api });
    return promise;
  }

  it('sends a NATIVE API away from the route', async () => {
    // The legacy Alerts screen offers HTTP conditions a Kafka gateway can never satisfy. Hiding the
    // menu entry left the route reachable by bookmark, which is the trust problem it meant to close.
    const result = await activate({ definitionVersion: 'V4', type: 'NATIVE' });

    expect(result).toEqual(TestBed.inject(Router).parseUrl('/apis/api-1'));
  });

  it.each(['PROXY', 'MESSAGE', 'MCP_PROXY'])('lets a V4 %s API through', async type => {
    expect(await activate({ definitionVersion: 'V4', type })).toBe(true);
  });

  it('lets a V2 API through, which carries no type at all', async () => {
    // NATIVE is a V4-only type, so the union has to be narrowed before `type` is read.
    expect(await activate({ definitionVersion: 'V2' })).toBe(true);
  });
});
