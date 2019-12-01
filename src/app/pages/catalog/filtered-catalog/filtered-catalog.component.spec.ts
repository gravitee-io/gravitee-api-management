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
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FilteredCatalogComponent } from './filtered-catalog.component';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TranslateTestingModule } from '../../../test/helper.spec';
import { RouterTestingModule } from '@angular/router/testing';
import { CatalogComponent } from '../catalog.component';
import { ApiStatesPipe } from '../../../pipes/api-states.pipe';
import { ApiLabelsPipe } from '../../../pipes/api-labels.pipe';
import { CatalogSearchComponent } from '../search/catalog-search.component';

describe('FilteredCatalogComponent', () => {
  let component: FilteredCatalogComponent;
  let fixture: ComponentFixture<FilteredCatalogComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        RouterTestingModule,
        TranslateTestingModule
      ],
      declarations: [FilteredCatalogComponent, ApiStatesPipe, ApiLabelsPipe],
      schemas: [
        CUSTOM_ELEMENTS_SCHEMA,
      ],
      providers: [ApiStatesPipe, ApiLabelsPipe]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FilteredCatalogComponent);
    component = fixture.componentInstance;
    fixture.whenStable().then(() => {
      fixture.detectChanges();
    });
  });

  it('should create', () => {
    fixture.whenStable().then(() => {
      fixture.detectChanges();
      expect(component).toBeTruthy();
    });
  });
});
