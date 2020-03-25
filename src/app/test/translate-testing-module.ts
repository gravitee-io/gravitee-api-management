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
import { Injectable, NgModule, Pipe, PipeTransform } from '@angular/core';
import { TranslateLoader, TranslateModule, TranslatePipe, TranslateService } from '@ngx-translate/core';
import { Observable, of } from 'rxjs';

const translations: any = {};

export class FakeLoader implements TranslateLoader {
  getTranslation(lang: string): Observable<any> {
    return of(translations);
  }
}

@Pipe({
  name: 'translate'
})
export class TranslatePipeMock implements PipeTransform {
  public name = 'translate';

  public transform(query: string, ...args: any[]): any {
    return query;
  }
}

@Injectable()
export class TranslateServiceStub {
  public get<T>(key: T): Observable<T> {
    return of(key);
  }
}

@NgModule({
  declarations: [
    TranslatePipeMock
  ],
  providers: [
    { provide: TranslateService, useClass: TranslateServiceStub },
    { provide: TranslatePipe, useClass: TranslatePipeMock },
  ],
  imports: [
    TranslateModule.forRoot({
      loader: { provide: TranslateLoader, useClass: FakeLoader },
    })
  ],
  exports: [
    TranslatePipeMock,
    TranslateModule
  ]
})
export class TranslateTestingModule {

}
