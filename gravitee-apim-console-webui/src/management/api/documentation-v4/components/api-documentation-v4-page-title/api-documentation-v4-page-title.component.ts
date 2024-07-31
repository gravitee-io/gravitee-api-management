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
import { MatAnchor, MatButtonModule } from '@angular/material/button';
import { MatTooltip } from '@angular/material/tooltip';

import { Constants } from '../../../../../entities/Constants';
import { Api } from '../../../../../entities/management-api-v2';

@Component({
  selector: 'api-documentation-page-title',
  templateUrl: './api-documentation-v4-page-title.component.html',
  styleUrls: ['./api-documentation-v4-page-title.component.scss'],
  standalone: true,
  imports: [MatAnchor, MatTooltip, MatButtonModule],
})
export class ApiDocumentationV4PageTitleComponent implements OnInit, OnChanges {
  @Input()
  api: Api;

  public apiPortalUrl: string;
  public apiNotInPortalTooltip: string;

  constructor(@Inject(Constants) public readonly constants: Constants) {}

  ngOnInit(): void {
    if (this.api) {
      this.calculateApiPortalUrl();
      this.apiNotInPortalTooltip = this.getApiNotInPortalTooltip();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.api) {
      this.api = changes.api.currentValue;
      this.ngOnInit();
    }
  }

  private calculateApiPortalUrl(): void {
    if (this.api.id && this.constants?.env?.settings?.portal?.url) {
      const root = this.constants.env.settings.portal.url;
      const apiPath = 'catalog/api/' + this.api.id;
      const connector = '/';

      this.apiPortalUrl = root.endsWith(connector) ? root + apiPath : root + connector + apiPath;
    }
  }

  private getApiNotInPortalTooltip(): string {
    switch (this.api.lifecycleState) {
      case 'DEPRECATED':
        return 'Deprecated APIs do not appear in the Developer Portal';
      case 'ARCHIVED':
        return 'Archived APIs do not appear in the Developer Portal';
      case 'CREATED':
      case 'UNPUBLISHED':
        return "Activate the Developer Portal by publishing your API under 'General > Info'";
      case 'PUBLISHED':
      default:
        return undefined;
    }
  }
}
