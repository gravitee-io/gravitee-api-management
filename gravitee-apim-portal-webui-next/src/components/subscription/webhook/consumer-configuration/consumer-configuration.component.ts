/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatCard, MatCardContent, MatCardHeader } from '@angular/material/card';
import { MatFormFieldModule, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { tap } from 'rxjs';

import { SubscriptionConsumerConfiguration } from '../../../../entities/subscription';
import { SubscriptionService } from '../../../../services/subscription.service';

@Component({
  imports: [MatCard, MatCardHeader, MatCardContent, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatLabel, MatIcon, RouterLink],
  selector: 'app-consumer-configuration',
  standalone: true,
  templateUrl: './consumer-configuration.component.html',
  styleUrls: ['./consumer-configuration.component.scss'],
})
export class ConsumerConfigurationComponent {
  consumerConfigurationForm: FormGroup | undefined;

  constructor(
    private readonly subscriptionService: SubscriptionService,
    private readonly route: ActivatedRoute,
  ) {
    this.subscriptionService
      .get(this.route.snapshot.params['subscriptionId'])
      .pipe(
        tap(subscription => {
          const consumerConfiguration = subscription.consumerConfiguration;
          this.initForm(consumerConfiguration);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private initForm(consumerConfiguration: SubscriptionConsumerConfiguration | undefined): void {
    if (consumerConfiguration) {
      const entrypointConfiguration = consumerConfiguration.entrypointConfiguration;
      this.consumerConfigurationForm = new FormGroup({
        channel: new FormControl(consumerConfiguration.channel),
        consumerConfiguration: new FormGroup({
          callbackUrl: new FormControl(entrypointConfiguration?.callbackUrl),
        }),
      });
    }
  }
}
