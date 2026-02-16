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

import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormArray, FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY } from 'rxjs';

import { A2A_PROVIDER, CreateIntegrationPayload, Integration } from '../integrations.model';
import { IntegrationsService } from '../../../services-ngx/integrations.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { IntegrationProviderService } from '../integration-provider.service';

@Component({
  selector: 'app-create-integration',
  templateUrl: './create-integration.component.html',
  styleUrls: ['./create-integration.component.scss'],
  standalone: false,
})
export class CreateIntegrationComponent implements OnInit {
  private destroyRef: DestroyRef = inject(DestroyRef);
  public isLoading: boolean = false;
  public chooseProviderForm = this.formBuilder.group({
    provider: ['', Validators.required],
  });
  public addInformationForm = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(50), Validators.minLength(1)]],
    description: ['', Validators.maxLength(250)],
    wellKnownUrls: new FormArray<FormGroup<{ url: FormControl<string> }>>([]),
  });

  constructor(
    public readonly integrationsService: IntegrationsService,
    private readonly formBuilder: FormBuilder,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly snackBarService: SnackBarService,
    protected readonly integrationProviderService: IntegrationProviderService,
  ) {}

  ngOnInit(): void {
    this.chooseProviderForm.valueChanges.subscribe(e => {
      const wellKnownUrls = this.addInformationForm.get('wellKnownUrls');
      if (e.provider === A2A_PROVIDER) {
        wellKnownUrls.addValidators([Validators.required]);
        wellKnownUrls.enable();
      } else {
        wellKnownUrls.removeValidators([Validators.required]);
        wellKnownUrls.disable();
      }
    });
  }

  public onSubmit(): void {
    const payload: CreateIntegrationPayload = {
      name: this.addInformationForm.controls.name.getRawValue(),
      description: this.addInformationForm.controls.description.getRawValue(),
      provider: this.chooseProviderForm.controls.provider.getRawValue(),
      wellKnownUrls:
        A2A_PROVIDER === this.chooseProviderForm.getRawValue().provider
          ? this.addInformationForm.controls.wellKnownUrls.getRawValue()
          : undefined,
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
        error: _ => {
          this.isLoading = false;
          this.snackBarService.error('An error occurred. Integration not created');
          return EMPTY;
        },
      });
  }

  public addWellKnownUrl(): void {
    const wellKnownUrls = this.addInformationForm.get('wellKnownUrls') as FormArray<FormGroup<{ url: FormControl<string> }>>;
    if (wellKnownUrls.enabled) {
      wellKnownUrls.push(
        this.formBuilder.group({
          url: ['', [Validators.required, Validators.pattern(/(http|https)?:\/\/(\S+)/)]],
        }),
      );
    }
  }

  public removeWellKnownUrl(idx: number): void {
    const wellKnownUrls = this.addInformationForm.get('wellKnownUrls') as FormArray<FormGroup<{ url: FormControl<string> }>>;
    if (wellKnownUrls?.enabled) {
      wellKnownUrls.removeAt(idx);
    }
  }
}
