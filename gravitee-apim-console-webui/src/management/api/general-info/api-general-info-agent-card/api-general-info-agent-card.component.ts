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
import { Component, Input } from '@angular/core';
import { MatCard, MatCardContent, MatCardHeader } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { GioCardEmptyStateModule, GioClipboardModule } from '@gravitee/ui-particles-angular';
import { NgIf } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatExpansionPanel, MatExpansionPanelHeader } from '@angular/material/expansion';
import { MatDivider } from '@angular/material/divider';

import { ApiFederatedAgent } from '../../../../entities/management-api-v2';

@Component({
  selector: 'api-general-info-agent-card',
  templateUrl: './api-general-info-agent-card.component.html',
  styleUrls: ['./api-general-info-agent-card.component.scss'],
  imports: [
    MatCardContent,
    MatCard,
    GioClipboardModule,
    MatCardHeader,
    MatIcon,
    NgIf,
    RouterModule,
    MatTooltipModule,
    GioCardEmptyStateModule,
    MatExpansionPanelHeader,
    MatExpansionPanel,
    MatDivider,
  ],
})
export class ApiGeneralInfoAgentCardComponent {
  @Input()
  public api: ApiFederatedAgent;

  @Input()
  public integrationId: string;

  @Input()
  public integrationName: string;

  getSecuritySchemesKeys(): string[] {
    if (this.api.definitionVersion === 'FEDERATED_AGENT') {
      return Object.keys(this.api.securitySchemes ?? {});
    }
    return [];
  }

  getSecuritySchemesProps(schemeKey: string): string[] {
    if (this.api.definitionVersion === 'FEDERATED_AGENT') {
      return Object.keys(this.api.securitySchemes?.[schemeKey] ?? {});
    }
    return [];
  }

  getSecuritySchemesValue(schemeKey: string, propKey: string): any {
    if (this.api.definitionVersion === 'FEDERATED_AGENT') {
      return this.api.securitySchemes?.[schemeKey]?.[propKey];
    }
    return null;
  }
}
