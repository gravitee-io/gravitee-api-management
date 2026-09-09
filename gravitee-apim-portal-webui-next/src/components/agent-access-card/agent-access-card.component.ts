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
import { Component, computed, input } from '@angular/core';

import { PlanSecurityEnum } from '../../entities/plan/plan';
import { Subscription } from '../../entities/subscription';
import { formatCurlCommandLine } from '../api-access/api-access.utils';
import { CopyCodeComponent } from '../copy-code/copy-code.component';

@Component({
  selector: 'app-agent-access-card',
  imports: [CopyCodeComponent],
  templateUrl: './agent-access-card.component.html',
  styleUrl: './agent-access-card.component.scss',
})
export class AgentAccessCardComponent {
  planSecurity = input.required<PlanSecurityEnum>();
  subscription = input.required<Subscription>();
  applicationName = input('');
  showApplicationName = input(false);
  entrypointUrl = input('');
  apiKey = input('');
  clientId = input('');
  clientSecret = input('');
  apiKeyHeader = input('');

  protected readonly isAccepted = computed(() => this.subscription().status === 'ACCEPTED');
  protected readonly curlCmd = computed(() =>
    formatCurlCommandLine(this.entrypointUrl(), this.planSecurity(), this.apiKeyHeader(), this.apiKey()),
  );
}
