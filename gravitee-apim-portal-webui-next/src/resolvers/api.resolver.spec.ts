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
import { HttpErrorResponse } from '@angular/common/http';
import { ApplicationRef, Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { NavigationEnd, provideRouter, Router } from '@angular/router';
import { filter, firstValueFrom, take, throwError } from 'rxjs';

import { apiResolver } from './api.resolver';
import { ApiService } from '../services/api.service';

describe('apiResolver', () => {
  @Component({ standalone: true, template: '' })
  class StubComponent {}

  let router: Router;

  beforeEach(async () => {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'api/:apiId', component: StubComponent, resolve: { api: apiResolver } },
          { path: '404', component: StubComponent },
        ]),
        {
          provide: ApiService,
          useValue: {
            details: jest.fn().mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404, statusText: 'Not Found' }))),
          },
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
  });

  it('should end navigation on /404 when api details return 404', async () => {
    const navigatedTo404 = firstValueFrom(
      router.events.pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        filter(e => e.urlAfterRedirects === '/404'),
        take(1),
      ),
    );

    router.navigateByUrl('/api/unknown');

    await navigatedTo404;
    await TestBed.inject(ApplicationRef).whenStable();

    expect(router.url).toBe('/404');
  });
});
