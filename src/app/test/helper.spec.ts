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
import { TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { TestBed } from '@angular/core/testing';
import { CurrentUserService } from '../services/current-user.service';
import { User } from '@gravitee/ng-portal-webclient';

const translations: any = { CARDS_TITLE: 'This is a test' };

class FakeLoader implements TranslateLoader {
  getTranslation(lang: string): Observable<any> {
    return of(translations);
  }
}

export const TranslateTestingModule = TranslateModule.forRoot({
  loader: { provide: TranslateLoader, useClass: FakeLoader },
});

export function getTranslateServiceMock() {
  let translateService: jasmine.SpyObj<TranslateService>;
  translateService = TestBed.get(TranslateService);
  translateService.getBrowserLang.and.returnValue('fr');
  translateService.get.and.returnValue(of(''));
  return translateService;
}

export function getCurrentUserServiceMock(user?: User) {
  const subject = new BehaviorSubject<User>(user);
  let currentUserService: jasmine.SpyObj<CurrentUserService>;
  currentUserService = TestBed.get(CurrentUserService);
  // @ts-ignore
  currentUserService.get.and.returnValue(subject);
  return currentUserService;
}

