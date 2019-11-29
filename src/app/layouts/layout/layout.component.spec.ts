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

import { LayoutComponent } from './layout.component';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateTestingModule } from '../../test/helper.spec';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { provideMock } from '../../test/mock.helper.spec';
import { Title } from '@angular/platform-browser';
import { CurrentUserService } from '../../services/current-user.service';
import { TranslateService } from '@ngx-translate/core';
import any = jasmine.any;
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NotificationService } from '../../services/notification.service';
import { FeatureGuardService } from '../../services/feature-guard.service';

describe('LayoutComponent', () => {

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        TranslateTestingModule,
        HttpClientTestingModule,
      ],
      declarations: [
        LayoutComponent
      ],
      schemas: [
        CUSTOM_ELEMENTS_SCHEMA,
      ],
      providers: [
        LayoutComponent,
        provideMock(Title),
        provideMock(CurrentUserService),
        provideMock(TranslateService),
        provideMock(NotificationService),
        provideMock(FeatureGuardService),
      ]
    }).compileComponents();
  }));


  let fixture;
  let app;
  let titleMock: jasmine.SpyObj<Title>;
  beforeEach(() => {
    titleMock = TestBed.get(Title);
    fixture = TestBed.createComponent(LayoutComponent);
    app = fixture.componentInstance;
    fixture.whenStable().then(() => {
      fixture.detectChanges();
    });
  });

  it('should create the layout', () => {
    fixture.whenStable().then(() => {
      fixture.detectChanges();
      expect(app).toBeTruthy();
    });
  });

  it(`should create routes'`, () => {
    fixture.whenStable().then(() => {
      fixture.detectChanges();
      expect(app.routes).toBeDefined();
      expect(app.routes.length).toBeGreaterThan(1);
      expect(app.routes[0]).toEqual(any(Promise));
    });
  });
});
