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
import { Component, computed, inject, input, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';

import { formatCurlCommandLine } from '../../../../../components/api-access/api-access.utils';
import { CopyCodeComponent } from '../../../../../components/copy-code/copy-code.component';
import { PlanSecurityEnum } from '../../../../../entities/plan/plan';
import { ApiProductSubscriptionApi } from '../../../../../entities/subscription/api-product-subscription-details';
import { ConfigService } from '../../../../../services/config.service';

@Component({
  selector: 'app-api-product-subscription-api-access',
  imports: [CopyCodeComponent, MatCardModule, MatFormFieldModule, MatSelectModule, RouterLink],
  templateUrl: './api-product-subscription-api-access.component.html',
  styleUrl: './api-product-subscription-api-access.component.scss',
})
export class ApiProductSubscriptionApiAccessComponent {
  private readonly configService = inject(ConfigService);

  readonly api = input.required<ApiProductSubscriptionApi>();
  readonly planSecurity = input<PlanSecurityEnum>();
  readonly apiKey = input<string>();
  readonly accessEnabled = input(false);

  protected readonly selectedEntrypoint = signal<string>('');
  protected readonly available = computed(() => this.api().availability === 'AVAILABLE');
  protected readonly entrypoints = computed(() => this.api().entrypoints ?? []);
  protected readonly selectedEntrypointValue = computed(() => {
    const selectedEntrypoint = this.selectedEntrypoint();
    const entrypoints = this.entrypoints();
    return selectedEntrypoint && entrypoints.includes(selectedEntrypoint) ? selectedEntrypoint : (entrypoints[0] ?? '');
  });
  protected readonly curlCommand = computed(() =>
    formatCurlCommandLine(
      this.selectedEntrypointValue(),
      this.planSecurity(),
      this.configService.configuration.portal?.apikeyHeader,
      this.apiKey(),
    ),
  );
}
