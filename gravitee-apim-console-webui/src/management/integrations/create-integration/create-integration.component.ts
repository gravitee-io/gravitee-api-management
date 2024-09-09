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

import { Component, DestroyRef, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY } from 'rxjs';

import { CreateIntegrationPayload, Integration, IntegrationProvider } from '../integrations.model';
import { IntegrationsService } from '../../../services-ngx/integrations.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

@Component({
  selector: 'app-create-integration',
  templateUrl: './create-integration.component.html',
  styleUrls: ['./create-integration.component.scss'],
})
export class CreateIntegrationComponent {
  private destroyRef: DestroyRef = inject(DestroyRef);
  public isLoading: boolean = false;
  public integrationProviders: { active?: IntegrationProvider[]; comingSoon?: IntegrationProvider[] } = {
    active: [
      { icon: 'aws-api-gateway', value: 'aws-api-gateway' },
      { icon: 'solace', value: 'solace' },
      { icon: 'apigee', value: 'apigee' },
      { icon: 'azure', value: 'azure-api-management' },
      { icon: 'ibm-api-connect', value: 'ibm-api-connect' },
      { icon: 'confluent', value: 'confluent-platform' },
    ],
    comingSoon: [
      { icon: 'kong', value: 'kong' },
      { icon: 'mulesoft', value: 'mulesoft' },
      { icon: 'dell-boomi', value: 'dell-boomi' },
    ],
  };
  public chooseProviderForm = this.formBuilder.group({
    provider: ['', Validators.required],
  });
  public addInformationForm = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(50), Validators.minLength(1)]],
    description: ['', Validators.maxLength(250)],
  });

  constructor(
    public readonly integrationsService: IntegrationsService,
    private readonly formBuilder: FormBuilder,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly snackBarService: SnackBarService,
  ) {}

  public onSubmit(): void {
    const payload: CreateIntegrationPayload = {
      name: this.addInformationForm.controls.name.getRawValue(),
      description: this.addInformationForm.controls.description.getRawValue(),
      provider: this.chooseProviderForm.controls.provider.getRawValue(),
    };

    this.isLoading = true;
    this.integrationsService
      .createIntegration(payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (integration: Integration) => {
          this.isLoading = false;
          this.snackBarService.success(`Integration ${payload.name} created successfully`);
          this.router.navigate([`../${integration.id}`], { relativeTo: this.activatedRoute });
        },
        error: (_) => {
          this.isLoading = false;
          this.snackBarService.error('An error occurred. Integration not created');
          return EMPTY;
        },
      });
  }
}
