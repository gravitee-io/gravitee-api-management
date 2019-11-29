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
import { TestBed } from '@angular/core/testing';

import { RouteService, RouteType } from './route.service';
import { provideMock } from '../test/mock.helper.spec';
import { TranslateService } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';
import { getTranslateServiceMock, TranslateTestingModule } from '../test/helper.spec';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CatalogComponent } from '../pages/catalog/catalog.component';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { CurrentUserService } from './current-user.service';
import { FeatureGuardService } from './feature-guard.service';

describe('RouteService', () => {
  beforeEach(() => TestBed.configureTestingModule({
    imports: [
      TranslateTestingModule,
      HttpClientTestingModule,
      RouterTestingModule.withRoutes([
        { path: 'foobar', redirectTo: '' },
        { path: 'catalog', data: { type: RouteType.catalog }, redirectTo: '' },
        {
          path: 'catalogWithChildren',
          data: { type: RouteType.catalog },
          children: [{ path: 'catalogChild', data: { type: RouteType.catalog }, redirectTo: '' }, { path: 'otherChild', redirectTo: '' }]
        }
      ]),
    ],
    providers: [
      provideMock(TranslateService),
      provideMock(FeatureGuardService),
    ]
  }));

  it('should be created', () => {
    const service: RouteService = TestBed.get(RouteService);
    expect(service).toBeTruthy();
  });

  it('should get flattened routes by type', () => {
    let featureGuardServiceMock: jasmine.SpyObj<FeatureGuardService>;
    featureGuardServiceMock = TestBed.get(FeatureGuardService);
    featureGuardServiceMock.canActivate.and.returnValue(true);
    getTranslateServiceMock();
    const service: RouteService = TestBed.get(RouteService);
    const { catalog } = RouteType;
    const routes = service.getRoutes(catalog);
    expect(routes.length).toEqual(3);
    expect(routes.map((r) => r.path).sort()).toEqual(['catalog', 'catalogWithChildren/catalogChild', 'catalogWithChildren'].sort());
  });

});
