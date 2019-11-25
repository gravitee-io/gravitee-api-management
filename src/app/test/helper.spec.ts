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
import { Observable, of } from 'rxjs';
import { TestBed } from '@angular/core/testing';

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
  let translateServiceMock: jasmine.SpyObj<TranslateService>;
  translateServiceMock = TestBed.get(TranslateService);
  translateServiceMock.getBrowserLang.and.returnValue('fr');
  translateServiceMock.get.and.returnValue(of(''));
  return translateServiceMock;
}

