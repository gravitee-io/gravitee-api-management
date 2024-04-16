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
import { FormBuilder, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { EMPTY } from 'rxjs';
import { catchError, filter, switchMap, tap } from 'rxjs/operators';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

import { Integration } from '../integrations.model';
import { IntegrationsService } from '../../../services-ngx/integrations.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

@Component({
  selector: 'app-integration-configuration',
  templateUrl: './integration-configuration.component.html',
  styleUrls: ['./integration-configuration.component.scss'],
})
export class IntegrationConfigurationComponent implements OnInit {
  private destroyRef: DestroyRef = inject(DestroyRef);
  public isLoading = true;
  public integration: Integration;

  public generalInformationForm = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(50), Validators.minLength(1)]],
    description: ['', Validators.maxLength(250)],
  });

  constructor(
    private integrationsService: IntegrationsService,
    private formBuilder: FormBuilder,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private snackBarService: SnackBarService,
    private matDialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.getIntegration();
  }

  private getIntegration(): void {
    const { integrationId } = this.activatedRoute.snapshot.params;
    this.integrationsService
      .getIntegration(integrationId)
      .pipe(
        catchError(({ error }) => {
          this.isLoading = false;
          this.snackBarService.error(error.message);
          this.router.navigate(['../..'], {
            relativeTo: this.activatedRoute,
          });
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((integration: Integration): void => {
        this.integration = integration;
        this.generalInformationForm.patchValue({ name: integration.name, description: integration.description });
        this.isLoading = false;
      });
  }

  public onSubmit(): void {
    this.isLoading = true;
    const { integrationId } = this.activatedRoute.snapshot.params;
    this.integrationsService
      .updateIntegration(this.generalInformationForm.getRawValue(), integrationId)
      .pipe(
        tap(() => {
          this.getIntegration();
          this.generalInformationForm.markAsPristine();
          this.snackBarService.success('Integration successfully updated!');
        }),
        catchError(({ error }) => {
          this.isLoading = false;
          this.snackBarService.error(`Something went wrong! ${error.message}`);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  public deleteIntegration(): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: GIO_DIALOG_WIDTH.SMALL,
        data: {
          title: 'Delete integration',
          content: 'Please note that once your integration is deleted, it cannot be restored.',
          confirmButton: 'Delete integration',
        },
        role: 'alertdialog',
        id: 'deleteIntegrationConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => !!confirm),
        switchMap(() => {
          this.isLoading = true;
          return this.integrationsService.deleteIntegration(this.integration.id);
        }),
        tap(() => {
          this.isLoading = false;
          this.router.navigate(['../..'], {
            relativeTo: this.activatedRoute,
          });
          this.snackBarService.success('Integration successfully deleted!');
        }),
        catchError(({ error }) => {
          this.isLoading = false;
          this.snackBarService.error(`Something went wrong! ${error.message}`);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }
}
