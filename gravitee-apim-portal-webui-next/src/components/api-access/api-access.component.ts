/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Component, computed, effect, Input, model } from '@angular/core';
import { MatCard, MatCardContent, MatCardHeader } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';

import { NativeKafkaApiAccessComponent } from './native-kafka-api-access/native-kafka-api-access.component';
import { ApiType } from '../../entities/api/api';
import { PlanSecurityEnum } from '../../entities/plan/plan';
import { Subscription } from '../../entities/subscription/subscription';
import { ConfigService } from '../../services/config.service';
import { CopyCodeIconComponent } from '../copy-code/copy-code-icon/copy-code-icon/copy-code-icon.component';
import { CopyCodeComponent } from '../copy-code/copy-code.component';

@Component({
  selector: 'app-api-access',
  imports: [
    MatCard,
    MatCardContent,
    MatCardHeader,
    CopyCodeComponent,
    NativeKafkaApiAccessComponent,
    MatFormFieldModule,
    MatSelectModule,
    CopyCodeIconComponent,
  ],
  templateUrl: './api-access.component.html',
  styleUrl: './api-access.component.scss',
})
export class ApiAccessComponent {
  @Input()
  planSecurity!: PlanSecurityEnum;

  @Input()
  subscription?: Subscription;

  @Input()
  apiKey?: string;

  @Input()
  apiKeyConfigUsername?: string;

  @Input()
  apiType?: ApiType;

  @Input()
  entrypointUrls?: string[];

  @Input()
  clientId?: string;

  @Input()
  clientSecret?: string;

  selectedEntrypointUrl = model<string>('');

  curlCmd = computed(() => this.formatCurlCommandLine(this.selectedEntrypointUrl(), this.planSecurity, this.apiKey));

  constructor(private configService: ConfigService) {
    effect(() => {
      this.selectedEntrypointUrl.set(this.entrypointUrls?.length ? this.entrypointUrls[0] : this.selectedEntrypointUrl());
    });
  }

  private formatCurlCommandLine(entrypointUrl: string, planSecurity: PlanSecurityEnum, apiKey?: string): string {
    if (!entrypointUrl) {
      return '';
    }
    let curlHeader = '';
    switch (planSecurity) {
      case 'JWT':
      case 'OAUTH2':
        curlHeader = '--header "Authorization: Bearer {{ ACCESS_TOKEN }}" ';
        break;
      case 'API_KEY':
        if (this.configService.configuration.portal?.apikeyHeader) {
          curlHeader = `--header "${this.configService.configuration.portal.apikeyHeader}: ${apiKey ?? '{{ API_KEY }}'}" `;
        }
        break;
    }

    return `curl ${curlHeader}${entrypointUrl}`;
  }
}
