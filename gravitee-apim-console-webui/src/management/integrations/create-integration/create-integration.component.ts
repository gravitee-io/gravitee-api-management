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
import { catchError, tap } from 'rxjs/operators';
import { EMPTY } from 'rxjs';

import { CreateIntegrationPayload } from '../integrations.model';
import { IntegrationsService } from '../../../services-ngx/integrations.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

@Component({
  selector: 'app-create-integration',
  templateUrl: './create-integration.component.html',
  styleUrls: ['./create-integration.component.scss'],
})
export class CreateIntegrationComponent {
  public isLoading = false;
  private destroyRef = inject(DestroyRef);

  public informationForm = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(50), Validators.minLength(1)]],
    description: ['', Validators.maxLength(250)],
  });

  constructor(
    private integrationsService: IntegrationsService,
    private formBuilder: FormBuilder,
    private readonly router: Router,
    private activatedRoute: ActivatedRoute,
    private snackBarService: SnackBarService,
  ) {}

  public onSubmit(): void {
    const payload: CreateIntegrationPayload = {
      name: this.informationForm.controls.name.getRawValue(),
      description: this.informationForm.controls.description.getRawValue(),
      provider: 'AWS',
    };

    this.isLoading = true;
    this.integrationsService
      .createIntegration(payload)
      .pipe(
        tap(() => {
          this.isLoading = false;
          this.snackBarService.success(`Integration ${payload.name} created successfully`);
        }),
        catchError((_) => {
          this.isLoading = false;
          this.snackBarService.error('An error occurred. Integration not created');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.isLoading = false;
        this.router.navigate(['..'], { relativeTo: this.activatedRoute });
      });
  }
}
