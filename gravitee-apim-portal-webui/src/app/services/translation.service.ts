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
import { Injectable } from '@angular/core';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { addTranslations, setLanguage } from '@gravitee/ui-components/src/lib/i18n';
import { TranslateService } from '@ngx-translate/core';
import { Title } from '@angular/platform-browser';

import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class TranslationService {
  constructor(private translateService: TranslateService, private titleService: Title) {}

  load() {
    return new Promise(resolve => {
      this.translateService.addLangs(environment.locales);
      const defaultLang = environment.locales[0];
      this.translateService.setDefaultLang(defaultLang);
      const browserLang = this.translateService.getBrowserLang();
      this.translateService.use(environment.locales.includes(browserLang) ? browserLang : defaultLang).subscribe(translations => {
        setLanguage(this.translateService.currentLang);
        addTranslations(this.translateService.currentLang, translations, this.translateService.currentLang);
        this.translateService.get(i18n('site.title')).subscribe(title => this.titleService.setTitle(title));
        resolve(true);
      });
    });
  }
}
