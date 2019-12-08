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

import { AccountComponent } from './account.component';
import { provideMock } from '../../test/mock.helper.spec';
import { CurrentUserService } from '../../services/current-user.service';
import { UserService } from '@gravitee/ng-portal-webclient';
import { TranslateTestingModule } from '../../test/helper.spec';
import { UserAvatarComponent } from '../../components/user-avatar/user-avatar.component';
import { SafePipe } from '../../pipes/safe.pipe';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

describe('AccountComponent', () => {
  let component: AccountComponent;
  let fixture: ComponentFixture<AccountComponent>;

  let userServiceMock: jasmine.SpyObj<UserService>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [AccountComponent, UserAvatarComponent, SafePipe],
      imports: [TranslateTestingModule],
      providers: [provideMock(UserService), provideMock(CurrentUserService)],
      schemas: [
        CUSTOM_ELEMENTS_SCHEMA,
      ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    userServiceMock = TestBed.get(UserService);

    fixture = TestBed.createComponent(AccountComponent);
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
