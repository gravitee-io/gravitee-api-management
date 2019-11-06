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
import {Component, OnInit} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {environment} from '../environments/environment';
import {Title} from '@angular/platform-browser';
import {marker as i18n} from '@biesbjerg/ngx-translate-extract-marker';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html'
})
export class AppComponent implements OnInit {

    constructor(
        private titleService: Title,
        private translateService: TranslateService
    ) {
    }

    ngOnInit() {
        this.translateService.addLangs(environment.locales);
        this.translateService.setDefaultLang(environment.locales[0]);
        const browserLang = this.translateService.getBrowserLang();
        this.translateService.use(browserLang.match(/en|fr/) ? browserLang : 'en');
        this.translateService.get(i18n('site.title')).subscribe(title => this.titleService.setTitle(title));
    }


}
