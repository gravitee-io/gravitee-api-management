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
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { catchError, filter, map, switchMap, tap } from 'rxjs/operators';
import { combineLatest, EMPTY } from 'rxjs';
import { FormControl, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  GIO_DIALOG_WIDTH,
  GioConfirmAndValidateDialogComponent,
  GioConfirmAndValidateDialogData,
  GioConfirmDialogComponent,
  GioConfirmDialogData,
  Header,
} from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import {
  AddCertificateDialogComponent,
  AddCertificateDialogData,
  AddCertificateDialogResult,
} from './add-certificate-dialog/add-certificate-dialog.component';

import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ApplicationService } from '../../../../services-ngx/application.service';
import { Application, ApplicationType } from '../../../../entities/application/Application';
import { ClientCertificate, ClientCertificateStatus } from '../../../../entities/application/ClientCertificate';
import { toDictionary, toGioFormHeader, uniqueKeysValidator } from '../../../../util/gio-form-header.util';

@Component({
  selector: 'application-general',
  templateUrl: './application-general.component.html',
  styleUrls: ['./application-general.component.scss'],
  standalone: false,
})
export class ApplicationGeneralComponent implements OnInit {
  public initialApplication: Application;
  public applicationType: ApplicationType;
  public applicationForm: UntypedFormGroup;
  public isLoadingData = true;
  public isReadOnly = false;
  public certificates: ClientCertificate[] = [];
  public certificatesDisplayedColumns = ['name', 'createdAt', 'endsAt', 'status', 'actions'];
  public isLoadingCertificates = false;
  public initialApplicationGeneralFormsValue: unknown;
  private destroyRef = inject(DestroyRef);

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly applicationService: ApplicationService,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
    private readonly router: Router,
  ) {}

  public ngOnInit() {
    this.isLoadingData = true;
    combineLatest([
      this.applicationService.getLastApplicationFetch(this.activatedRoute.snapshot.params.applicationId),
      this.applicationService.getApplicationType(this.activatedRoute.snapshot.params.applicationId),
    ])
      .pipe(
        tap(([application, applicationType]) => {
          this.initialApplication = application;
          this.applicationType = applicationType;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.isLoadingData = false;
        this.isReadOnly = this.initialApplication.status === 'ARCHIVED' || this.initialApplication.origin === 'KUBERNETES';

        this.applicationForm = new UntypedFormGroup({
          details: new UntypedFormGroup({
            name: new UntypedFormControl({ value: this.initialApplication.name, disabled: this.isReadOnly }),
            description: new UntypedFormControl({ value: this.initialApplication.description, disabled: this.isReadOnly }),
            domain: new UntypedFormControl({ value: this.initialApplication.domain, disabled: this.isReadOnly }),
          }),
          images: new UntypedFormGroup({
            picture: new UntypedFormControl({
              value: this.initialApplication.picture ? [this.initialApplication.picture] : undefined,
              disabled: this.isReadOnly,
            }),
            background: new UntypedFormControl({
              value: this.initialApplication.background ? [this.initialApplication.background] : undefined,
              disabled: this.isReadOnly,
            }),
          }),
        });

        if (this.initialApplication.type === 'SIMPLE') {
          this.applicationForm.addControl(
            'OAuth2Form',
            new UntypedFormGroup({
              client_id: new UntypedFormControl({
                value: this.initialApplication.settings?.app?.client_id ? this.initialApplication.settings.app.client_id : undefined,
                disabled: this.isReadOnly,
              }),
            }),
          );
        }

        if (this.initialApplication.type !== 'SIMPLE') {
          this.applicationForm.addControl(
            'OpenIDForm',
            new UntypedFormGroup({
              client_id: new UntypedFormControl({
                value: this.initialApplication.settings?.oauth?.client_id ? this.initialApplication.settings.oauth.client_id : undefined,
                disabled: this.isReadOnly,
              }),
              client_secret: new UntypedFormControl({
                value: this.initialApplication.settings?.oauth?.client_secret
                  ? this.initialApplication.settings.oauth.client_secret
                  : undefined,
                disabled: this.isReadOnly,
              }),
              grant_types: new UntypedFormControl(
                {
                  value: this.initialApplication.settings?.oauth?.grant_types
                    ? this.initialApplication.settings.oauth.grant_types
                    : undefined,
                  disabled: this.isReadOnly,
                },
                [Validators.required],
              ),
              redirect_uris: new UntypedFormControl({
                value: this.initialApplication.settings?.oauth?.redirect_uris
                  ? this.initialApplication.settings.oauth.redirect_uris
                  : undefined,
                disabled: this.isReadOnly,
              }),
              additional_client_metadata: new FormControl<Header[]>(
                {
                  value: toGioFormHeader(this.initialApplication.settings?.oauth?.additional_client_metadata),
                  disabled: this.isReadOnly,
                },
                [uniqueKeysValidator()],
              ),
            }),
          );
        }

        this.initialApplicationGeneralFormsValue = this.applicationForm.getRawValue();
        this.loadCertificates();
      });
  }

  onSubmit() {
    const imagesValue = this.applicationForm.getRawValue().images;

    const pictureUrl = imagesValue?.picture?.[0]?.dataUrl || (imagesValue?.picture?.length === 0 ? null : this.initialApplication.picture);
    const backgroundUrl =
      imagesValue?.background?.[0]?.dataUrl || (imagesValue?.background?.length === 0 ? null : this.initialApplication.background);

    const applicationToUpdate = {
      ...this.initialApplication,
      ...this.applicationForm.getRawValue().details,
      settings:
        this.initialApplication.type === 'SIMPLE'
          ? {
              app: {
                ...this.applicationForm.getRawValue().OAuth2Form,
              },
            }
          : {
              oauth: {
                ...this.initialApplication.settings.oauth,
                ...this.applicationForm.getRawValue().OpenIDForm,
                additional_client_metadata: toDictionary(this.applicationForm.getRawValue().OpenIDForm.additional_client_metadata),
                application_type: this.applicationType.id,
              },
            },
      picture: pictureUrl,
      background: backgroundUrl,
    };

    this.applicationService
      .update(applicationToUpdate)
      .pipe(
        tap(() => this.snackBarService.success('Application details successfully updated!')),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.ngOnInit());
  }

  loadCertificates(): void {
    this.isLoadingCertificates = true;
    this.applicationService
      .listCertificates(this.activatedRoute.snapshot.params.applicationId, 1, 100)
      .pipe(
        tap(result => {
          this.certificates = result.data ?? [];
          this.isLoadingCertificates = false;
        }),
        catchError(() => {
          this.certificates = [];
          this.isLoadingCertificates = false;
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  openAddCertificateDialog(): void {
    const sortedByNewest = [...this.certificates].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    const activeCert =
      sortedByNewest.find(c => c.status === ClientCertificateStatus.ACTIVE) ??
      sortedByNewest.find(c => c.status === ClientCertificateStatus.ACTIVE_WITH_END);

    this.matDialog
      .open<AddCertificateDialogComponent, AddCertificateDialogData, AddCertificateDialogResult>(AddCertificateDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: {
          hasActiveCertificates: !!activeCert,
          activeCertificateId: activeCert?.id,
        },
        role: 'dialog',
        id: 'addCertificateDialog',
      })
      .afterClosed()
      .pipe(
        filter(result => !!result),
        switchMap(result => {
          const create$ = this.applicationService.createCertificate(this.activatedRoute.snapshot.params.applicationId, {
            name: result.name,
            certificate: result.certificate,
            endsAt: result.endsAt,
          });

          if (result.gracePeriodEnd && result.activeCertificateId) {
            return create$.pipe(
              switchMap(() =>
                this.applicationService.updateCertificate(this.activatedRoute.snapshot.params.applicationId, result.activeCertificateId, {
                  name: activeCert.name,
                  endsAt: result.gracePeriodEnd,
                }),
              ),
            );
          }
          return create$;
        }),
        tap(() => {
          this.snackBarService.success('Certificate added successfully.');
          this.loadCertificates();
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ?? 'Failed to add certificate.');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  deleteCertificate(cert: ClientCertificate): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: {
          title: 'Delete Certificate',
          content: `Are you sure you want to delete the certificate "<b>${cert.name}</b>"?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'confirmCertificateDeleteDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.applicationService.deleteCertificate(this.activatedRoute.snapshot.params.applicationId, cert.id)),
        tap(() => {
          this.snackBarService.success(`Certificate "${cert.name}" has been deleted.`);
          this.loadCertificates();
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ?? 'Failed to delete certificate.');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  deleteApplication() {
    this.matDialog
      .open<GioConfirmAndValidateDialogComponent, GioConfirmAndValidateDialogData>(GioConfirmAndValidateDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: {
          title: `Delete Application`,
          content: `Are you sure you want to delete the Application?`,
          confirmButton: `Yes, delete it`,
          validationMessage: `Please, type in the name of the application <code>${this.initialApplication.name}</code> to confirm.`,
          validationValue: this.initialApplication.name,
          warning: `This operation is irreversible.`,
        },
        role: 'alertdialog',
        id: 'applicationDeleteDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.applicationService.delete(this.activatedRoute.snapshot.params.applicationId)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        map(() => this.snackBarService.success(`The Application has been deleted.`)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.router.navigate(['../../'], { relativeTo: this.activatedRoute });
      });
  }
}
