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
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router, RouterOutlet, provideRouter } from '@angular/router';

import { ApisGuard } from './apis.guard';

import { Constants } from '../../entities/Constants';
import { CONSTANTS_TESTING } from '../../shared/testing';

@Component({ template: '' })
class BlankComponent {}

@Component({ template: '<router-outlet></router-outlet>', imports: [RouterOutlet] })
class RootComponent {}

describe('ApisGuard.denyNativeApi', () => {
  let httpTestingController: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      // Not GioTestingModule: it registers `{ path: '**', redirectTo: '' }`, which swallows every
      // navigation and would make any routing assertion here vacuous.
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Constants, useValue: CONSTANTS_TESTING },
        // Mirrors the real nesting — the alerts routes are children of `:apiId`, which sits under
        // `:envHrid`. Asserting against a literal the guard also builds cannot catch a redirect that
        // omits the environment, which is exactly the bug this shape exists to expose.
        provideRouter([
          {
            path: ':envHrid',
            children: [
              {
                path: 'apis',
                children: [
                  {
                    path: ':apiId',
                    children: [
                      { path: '', component: BlankComponent },
                      { path: 'v4/alerts', component: BlankComponent, canActivate: [ApisGuard.denyNativeApi] },
                    ],
                  },
                ],
              },
            ],
          },
        ]),
      ],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    // The guard only runs on a real navigation, which needs a live outlet.
    TestBed.createComponent(RootComponent).detectChanges();
  });

  afterEach(() => httpTestingController.verify());

  function navigateToAlerts(api: Record<string, unknown>): void {
    router.navigateByUrl('/DEFAULT/apis/api-1/v4/alerts');
    tick();
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/api-1`).flush({ id: 'api-1', ...api });
    tick();
  }

  it('sends a NATIVE API to its own page, under the environment it came from', fakeAsync(() => {
    // The legacy Alerts screen offers HTTP conditions a Kafka gateway can never satisfy. Hiding the
    // menu entry left the route reachable by bookmark, which is the trust problem it means to close.
    navigateToAlerts({ definitionVersion: 'V4', type: 'NATIVE' });

    expect(router.url).toEqual('/DEFAULT/apis/api-1');
  }));

  it.each(['PROXY', 'MESSAGE', 'MCP_PROXY'])(
    'lets a V4 %s API through',
    fakeAsync((type: string) => {
      navigateToAlerts({ definitionVersion: 'V4', type });

      expect(router.url).toEqual('/DEFAULT/apis/api-1/v4/alerts');
    }),
  );

  it('lets a V2 API through, which carries no type at all', fakeAsync(() => {
    // NATIVE is a V4-only type, so the union has to be narrowed before `type` is read.
    navigateToAlerts({ definitionVersion: 'V2' });

    expect(router.url).toEqual('/DEFAULT/apis/api-1/v4/alerts');
  }));
});
