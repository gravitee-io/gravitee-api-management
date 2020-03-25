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

import { provideMock } from '../../../test/mock.helper.spec';
import { PortalService } from '@gravitee/ng-portal-webclient';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ApiContactComponent } from './api-contact.component';
import { TranslateTestingModule } from '../../../test/translate-testing-module';

describe('ApiContactComponent', () => {
  let component: ApiContactComponent;
  let fixture: ComponentFixture<ApiContactComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ApiContactComponent ],
      imports: [ TranslateTestingModule, HttpClientTestingModule, RouterTestingModule ],
      providers: [provideMock(PortalService)],
      schemas: [
        CUSTOM_ELEMENTS_SCHEMA,
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ApiContactComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
