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
import { Component, Input, OnInit } from '@angular/core';
import { MatCard, MatCardContent, MatCardHeader } from '@angular/material/card';

import { PlanSecurityEnum } from '../../entities/plan/plan';
import { SubscriptionStatusEnum } from '../../entities/subscription/subscription';
import { ConfigService } from '../../services/config.service';
import { CopyCodeComponent } from '../copy-code/copy-code.component';

@Component({
  selector: 'app-api-access',
  standalone: true,
  imports: [MatCard, MatCardContent, MatCardHeader, CopyCodeComponent],
  templateUrl: './api-access.component.html',
  styleUrl: './api-access.component.scss',
})
export class ApiAccessComponent implements OnInit {
  @Input()
  planSecurity!: PlanSecurityEnum;

  @Input()
  subscriptionStatus?: SubscriptionStatusEnum;

  @Input()
  apiKey?: string;

  @Input()
  entrypointUrl?: string;

  @Input()
  clientId?: string;

  @Input()
  clientSecret?: string;

  curlCmd: string = '';

  constructor(private configService: ConfigService) {}

  ngOnInit(): void {
    if (this.entrypointUrl && this.apiKey) {
      this.curlCmd = this.formatCurlCommandLine(this.entrypointUrl, this.apiKey);
    }
  }

  private formatCurlCommandLine(entrypointUrl: string, apiKey?: string): string {
    if (!entrypointUrl) {
      return '';
    }
    const apiKeyHeader =
      apiKey && this.configService.configuration.portal?.apikeyHeader
        ? `--header "${this.configService.configuration.portal.apikeyHeader}: ${apiKey}" `
        : '';
    return `curl ${apiKeyHeader}${entrypointUrl}`;
  }
}
