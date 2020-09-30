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
import { RouterTestingModule } from '@angular/router/testing';
import { UserService } from 'projects/portal-webclient-sdk/src/lib';
import { of } from 'rxjs';
import { AppComponent } from './app.component';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { NavRouteService } from './services/nav-route.service';
import { NotificationService } from './services/notification.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TranslateTestingModule } from './test/translate-testing-module';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { UserTestingModule } from './test/user-testing-module';

describe('AppComponent', () => {
  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        HttpClientTestingModule,
        TranslateTestingModule,
        BrowserAnimationsModule,
        UserTestingModule
      ],
      declarations: [
        AppComponent
      ],
      schemas: [
        CUSTOM_ELEMENTS_SCHEMA,
      ],
      providers: [
        AppComponent,
      ]
    }).compileComponents();
  }));

  let fixture;
  let app;
  let titleMock: Title;
  let notificationService;
  let navRouteService;
  let userService;
  beforeEach(() => {
    titleMock = TestBed.inject(Title);
    notificationService = TestBed.inject(NotificationService);
    navRouteService = TestBed.inject(NavRouteService);
    navRouteService.getUserNav = jasmine.createSpy().and.returnValue([]);
    userService = TestBed.inject(UserService);
    userService.getCurrentUserNotifications = jasmine.createSpy().and.returnValue(of({}));
    fixture = TestBed.createComponent(AppComponent);
    app = fixture.componentInstance;
  });

  it('should create the app', (done) => {
    fixture.whenStable().then(() => {
      fixture.detectChanges();
      expect(app).toBeTruthy();
      done();
    });
  });

});
