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
import { Component, Inject, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';

import { Constants } from '../../../../../entities/Constants';
import { Api } from '../../../../../entities/management-api-v2';

@Component({
  selector: 'api-documentation-page-title',
  template: require('./api-documentation-v4-page-title.component.html'),
  styles: [require('./api-documentation-v4-page-title.component.scss')],
})
export class ApiDocumentationV4PageTitleComponent implements OnInit, OnChanges {
  @Input()
  api: Api;

  public apiPortalUrl: string;

  constructor(@Inject('Constants') public readonly constants: Constants) {}

  ngOnInit(): void {
    this.calculateApiPortalUrl();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.api) {
      this.api = changes.api.currentValue;
      this.calculateApiPortalUrl();
    }
  }

  private calculateApiPortalUrl(): void {
    if (this.api?.id && this.constants?.env?.settings?.portal?.url) {
      const root = this.constants.env.settings.portal.url;
      const apiPath = 'catalog/api/' + this.api.id;
      const connector = '/';

      this.apiPortalUrl = root.endsWith(connector) ? root + apiPath : root + connector + apiPath;
    }
  }
}
