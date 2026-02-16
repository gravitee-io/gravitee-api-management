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

import { Component, DestroyRef } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, filter, map, switchMap, tap } from 'rxjs/operators';
import { EMPTY } from 'rxjs';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

import { A2A_PROVIDER, FederatedAPIsResponse, Integration } from '../../integrations.model';
import { IntegrationsService } from '../../../../services-ngx/integrations.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

@Component({
  selector: 'app-integration-general-configuration',
  templateUrl: './integration-general-configuration.component.html',
  styleUrl: './integration-general-configuration.component.scss',
  standalone: false,
})
export class IntegrationGeneralConfigurationComponent {
  public isLoading = true;
  public integration: Integration;
  public hasFederatedAPIs: boolean = false;
  public isA2A: boolean = false;
  public wellKnownUrls = [];
  public generalInformationForm: FormGroup = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(50), Validators.minLength(1)]],
    description: ['', Validators.maxLength(250)],
  });

  constructor(
    public readonly integrationsService: IntegrationsService,
    private readonly formBuilder: FormBuilder,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
    private readonly destroyRef: DestroyRef,
  ) {}

  ngOnInit(): void {
    this.getIntegration();
    this.checkIfHasFederatedAPIs();
  }

  public wellKnownUrlsArrayControl() {
    return this.generalInformationForm.get('wellKnownUrls') as FormArray;
  }

  public enterKeyDown(event: KeyboardEvent, i: number) {
    if (event.key === 'Enter') {
      this.deleteWellKnownUrl(i);
    }
  }

  public deleteWellKnownUrl(index: number) {
    if (this.generalInformationForm.getRawValue().wellKnownUrls.filter((url: string) => !!url).length > 1) {
      this.wellKnownUrlsArrayControl().removeAt(index);
      this.generalInformationForm.markAsDirty();
    } else {
      this.snackBarService.error('You must have at least one well-known URL');
    }
  }

  public onSubmit(): void {
    this.integrationsService
      .updateIntegration(
        {
          description: this.generalInformationForm.getRawValue().description,
          name: this.generalInformationForm.getRawValue().name,
          groups: this.integration.groups,
          ...(this.isA2A ? this.getWellKnownUrlsPayload() : {}),
        },
        this.activatedRoute.snapshot.params.integrationId,
      )
      .pipe(
        switchMap(() => this.integrationsService.getIntegration(this.activatedRoute.snapshot.params.integrationId)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: integration => {
          this.integration = integration;
          this.generalInformationForm.patchValue({ name: integration.name, description: integration.description });
          this.createUrlControls(integration);
          this.generalInformationForm.markAsPristine();
          this.snackBarService.success('Integration successfully updated!');
        },
        error: ({ error }) => {
          this.snackBarService.error(`Something went wrong! ${error.message}`);
          return EMPTY;
        },
      });
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
        filter(confirm => !!confirm),
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

  public deleteFederatedApis(): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: GIO_DIALOG_WIDTH.SMALL,
        data: {
          title: 'Delete APIs',
          content: "Published APIs won't be deleted. Deleted APIs cannot be restored.",
          confirmButton: 'Delete APIs',
        },
        role: 'alertdialog',
        id: 'deleteAPIsConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm),
        switchMap(() => {
          this.isLoading = true;
          this.snackBarService.success('We’re deleting Federated APIs from this integration...');
          return this.integrationsService.deleteFederatedAPIs(this.integration.id);
        }),
        tap(deletedApisResponse => {
          this.isLoading = false;
          this.snackBarService.success(
            `Federated APIs have been deleted.\n` +
              `  \u2022 Deleted: ${deletedApisResponse.deleted}\n` +
              `  \u2022 Not deleted: ${deletedApisResponse.skipped}\n` +
              `  \u2022 Errors: ${deletedApisResponse.errors}`,
          );
          this.checkIfHasFederatedAPIs();
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

  private getWellKnownUrlsPayload() {
    return {
      wellKnownUrls: this.generalInformationForm
        .getRawValue()
        .wellKnownUrls.filter((url: string) => !!url)
        .map((url: string): { url: string } => ({ url })),
    };
  }

  private checkIfHasFederatedAPIs(): void {
    this.integrationsService
      .getFederatedAPIs(this.activatedRoute.snapshot.params.integrationId)
      .pipe(
        map((res: FederatedAPIsResponse) => !!res.data.length),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((hasFederatedAPIs: boolean) => (this.hasFederatedAPIs = hasFederatedAPIs));
  }

  private getIntegration(): void {
    this.isLoading = true;
    this.integrationsService
      .getIntegration(this.activatedRoute.snapshot.params.integrationId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (integration: Integration): void => {
          this.integration = integration;
          this.isA2A = integration.provider === A2A_PROVIDER;
          this.generalInformationForm.patchValue({ name: integration.name, description: integration.description });
          this.createUrlControls(integration);
          this.isLoading = false;
        },
        error: ({ error }) => {
          this.snackBarService.error(error.message);
          this.router.navigate(['../..'], {
            relativeTo: this.activatedRoute,
          });
          return EMPTY;
        },
      });
  }

  private createUrlControls(integration: Integration): void {
    if (this.isA2A && 'wellKnownUrls' in integration) {
      this.generalInformationForm.removeControl('wellKnownUrls');
      this.generalInformationForm.addControl('wellKnownUrls', this.formBuilder.array([]));
      for (const wellKnownUrl of integration.wellKnownUrls) {
        (this.generalInformationForm.get('wellKnownUrls') as FormArray).push(
          this.formBuilder.control(wellKnownUrl.url, [Validators.required, Validators.pattern(/(http|https)?:\/\/(\S+)/)]),
        );
      }
      (this.generalInformationForm.get('wellKnownUrls') as FormArray).push(
        this.formBuilder.control('', [Validators.minLength(8), Validators.pattern(/(http|https)?:\/\/(\S+)/)]),
      );
    }
  }
}
