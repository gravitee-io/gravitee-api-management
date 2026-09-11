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
import { CUSTOM_ELEMENTS_SCHEMA, Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';

import { ApiKeyModeEnum } from '../../../../../../projects/portal-webclient-sdk/src/lib';
import '@gravitee/ui-components/wc/gv-option';

@Component({
  selector: 'app-application-creation-step4',
  templateUrl: './application-creation-step4.component.html',
  styleUrls: ['../application-creation.component.css'],
  imports: [TranslatePipe],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class ApplicationCreationStep4Component implements OnInit {
  private translateService = inject(TranslateService);

  @Input() apiKeyMode: ApiKeyModeEnum;
  @Output() updated = new EventEmitter<ApiKeyModeEnum>();

  apiKeyModeOptions: { id: string; title: string; description: string }[];

  ngOnInit(): void {
    this.translateService
      .get(['apiKeyMode.exclusive.title', 'apiKeyMode.exclusive.description', 'apiKeyMode.shared.title', 'apiKeyMode.shared.description'])
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
