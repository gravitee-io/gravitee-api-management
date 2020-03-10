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
import { async, TestBed } from '@angular/core/testing';

import { CatalogSearchComponent } from './catalog-search.component';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateTestingModule } from 'src/app/test/helper.spec';
import { FormBuilder, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideMock } from '../../../test/mock.helper.spec';
import { NotificationService } from '../../../services/notification.service';
import { ConfigurationService } from '../../../services/configuration.service';
import { LoaderService } from '../../../services/loader.service';
import { ApiStatesPipe } from '../../../pipes/api-states.pipe';
import { ApiLabelsPipe } from '../../../pipes/api-labels.pipe';
import { of } from 'rxjs';
import { ApiService } from '@gravitee/ng-portal-webclient';
import { ActivatedRoute, Router } from '@angular/router';

describe('CatalogSearchComponent', () => {
  let component: CatalogSearchComponent;
  let configService: jasmine.SpyObj<ConfigurationService>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        TranslateTestingModule,
        FormsModule,
        ReactiveFormsModule,
        HttpClientTestingModule
      ],
      declarations: [
        CatalogSearchComponent, ApiStatesPipe, ApiLabelsPipe
      ],
      schemas: [
        CUSTOM_ELEMENTS_SCHEMA,
      ],
      providers: [
        provideMock(NotificationService),
        provideMock(ConfigurationService),
        provideMock(LoaderService), ApiStatesPipe, ApiLabelsPipe
      ]
    })
      .compileComponents();
  }));

  beforeEach(() => {

    configService = TestBed.get(ConfigurationService);
    configService.get.and.returnValue([5, 10, 15]);

    component = new CatalogSearchComponent(
      TestBed.get(FormBuilder),
      TestBed.get(ApiService),
      TestBed.get(ActivatedRoute),
      TestBed.get(Router),
      configService
    );
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
