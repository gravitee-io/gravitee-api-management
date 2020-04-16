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
import { TranslateTestingModule } from '../../../test/translate-testing-module';
import { UserTestingModule } from '../../../test/user-testing-module';

import { UserNotificationComponent } from './user-notification.component';
import { SafePipe } from '../../../pipes/safe.pipe';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideMock } from '../../../test/mock.helper.spec';
import { UserService } from '@gravitee/ng-portal-webclient';
import { Observable } from 'rxjs';

describe('UserNotificationComponent', () => {
  let component: UserNotificationComponent;
  let fixture: ComponentFixture<UserNotificationComponent>;
  let userService: UserService;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [UserNotificationComponent, SafePipe],
      imports: [TranslateTestingModule, UserTestingModule, HttpClientTestingModule, RouterTestingModule],
      schemas: [
        CUSTOM_ELEMENTS_SCHEMA,
      ],
      providers: [
        provideMock(UserService),
      ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(UserNotificationComponent);
    component = fixture.componentInstance;

    userService = TestBed.inject(UserService);
    userService.getCurrentUserNotifications = jasmine.createSpy().and.returnValue(new Observable());
    return userService;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
