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
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { TranslateService } from '@ngx-translate/core';
import { ApiKeyModeEnum } from '../../../../../../projects/portal-webclient-sdk/src/lib';
import '@gravitee/ui-components/wc/gv-option';

@Component({
  selector: 'app-application-creation-step4',
  templateUrl: './application-creation-step4.component.html',
  styleUrls: ['../application-creation.component.css'],
})
export class ApplicationCreationStep4Component implements OnInit {
  @Input() apiKeyMode: ApiKeyModeEnum;
  @Output() updated = new EventEmitter<ApiKeyModeEnum>();

  apiKeyModeOptions: { id: string; title: string; description: string }[];

  constructor(private translateService: TranslateService) {}

  ngOnInit(): void {
    this.translateService
      .get([
        i18n('apiKeyMode.exclusive.title'),
        i18n('apiKeyMode.exclusive.description'),
        i18n('apiKeyMode.shared.title'),
        i18n('apiKeyMode.shared.description'),
      ])
      .toPromise()
      .then(_translations => {
        const translations: string[] = Object.values(_translations);
        this.apiKeyModeOptions = [
          {
            id: ApiKeyModeEnum.EXCLUSIVE,
            title: translations[0],
            description: translations[1],
          },
          {
            id: ApiKeyModeEnum.SHARED,
            title: translations[2],
            description: translations[3],
          },
        ];
      });
  }

  onModeChange({ detail }) {
    this.updated.emit(detail.id);
  }
}
