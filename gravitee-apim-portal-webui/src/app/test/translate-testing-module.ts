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
import { AfterViewChecked, Directive, ElementRef, Input, NgModule, Pipe, PipeTransform, Injectable } from '@angular/core';
import { LangChangeEvent, TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';

export const TRANSLATED_STRING = 'i18n';

@Injectable()
export class TranslateServiceMock {
  onLangChangeSubject: Subject<LangChangeEvent> = new Subject();
  onTranslationChangeSubject: Subject<string> = new Subject();
  onDefaultLangChangeSubject: Subject<string> = new Subject();
  isLoadedSubject: BehaviorSubject<boolean> = new BehaviorSubject(true);

  onLangChange: Observable<LangChangeEvent> = this.onLangChangeSubject.asObservable();
  onTranslationChange: Observable<string> = this.onTranslationChangeSubject.asObservable();
  onDefaultLangChange: Observable<string> = this.onDefaultLangChangeSubject.asObservable();
  isLoaded: Observable<boolean> = this.isLoadedSubject.asObservable();

  currentLang: string;

  languages: string[] = ['de'];

  get(content: string): Observable<string> {
    return of(TRANSLATED_STRING + content);
  }

  use(lang: string): void {
    this.currentLang = lang;
    this.onLangChangeSubject.next({ lang } as LangChangeEvent);
  }

  addLangs(langs: string[]): void {
    this.languages = [...this.languages, ...langs];
  }

  getBrowserLang(): string {
    return '';
  }

  getLangs(): string[] {
    return this.languages;
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  getTranslation(): Observable<any> {
    return of({});
  }

  instant(key: string | string[]): string {
    return TRANSLATED_STRING + key.toString();
  }

  setDefaultLang(lang: string): void {
    this.onDefaultLangChangeSubject.next(lang);
  }
}

@Pipe({ name: 'translate' })
export class TranslateMockPipe implements PipeTransform {
  transform(text: string): string {
    return !text ? TRANSLATED_STRING : `${text}-${TRANSLATED_STRING}`;
  }
}

@Directive({
  // eslint-disable-next-line @angular-eslint/directive-selector
  selector: '[translate]',
})
/* eslint-disable @typescript-eslint/no-explicit-any */
export class TranslateMockDirective implements AfterViewChecked {
  @Input()
  translateParams: any;
  constructor(private readonly _element: ElementRef) {}

  ngAfterViewChecked(): void {
    this._element.nativeElement.innerText += TRANSLATED_STRING;
  }
}

@NgModule({
  declarations: [TranslateMockPipe, TranslateMockDirective],
  exports: [TranslateMockPipe, TranslateMockDirective],
  providers: [{ provide: TranslateService, useClass: TranslateServiceMock }],
})
export class TranslateTestingModule {}
