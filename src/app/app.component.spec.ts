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
import { TestBed, async } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AppComponent } from './app.component';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { provideMagicalMock } from './test/mock.helper.spec';
import { Observable, of } from 'rxjs';
import any = jasmine.any;
import { CurrentUserService } from './services/current-user.service';
import { TranslateTestingModule } from './test/helper.spec';
import { UserService } from '@gravitee/ng-portal-webclient';

describe('AppComponent', () => {
  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        TranslateTestingModule
      ],
      declarations: [
        AppComponent
      ],
      schemas: [
        CUSTOM_ELEMENTS_SCHEMA,
      ],
      providers: [
        AppComponent,
        provideMagicalMock(Title),
        provideMagicalMock(UserService),
        provideMagicalMock(CurrentUserService),
        provideMagicalMock(TranslateService)
      ]
    }).compileComponents();
  }));


  let fixture;
  let app;
  let titleMock: jasmine.SpyObj<Title>;
  beforeEach(() => {
    titleMock = TestBed.get(Title);
    fixture = TestBed.createComponent(AppComponent);
    app = fixture.componentInstance;
    fixture.whenStable().then(() => {
      fixture.detectChanges();
    });
  });

  it('should create the app', () => {
    fixture.whenStable().then(() => {
      fixture.detectChanges();
      expect(app).toBeTruthy();
    });
  });

});
