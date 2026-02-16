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
import { EMPTY } from 'rxjs';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  GIO_DIALOG_WIDTH,
  GioConfirmAndValidateDialogComponent,
  GioConfirmAndValidateDialogData,
  GioFormFocusInvalidModule,
  GioSaveBarModule,
} from '@gravitee/ui-particles-angular';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule, DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBarModule } from '@angular/material/snack-bar';

import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ClusterService } from '../../../../services-ngx/cluster.service';
import { Cluster } from '../../../../entities/management-api-v2';
import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';

@Component({
  selector: 'cluster-general',
  templateUrl: './cluster-general.component.html',
  styleUrls: ['./cluster-general.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    DatePipe,
    GioSaveBarModule,
    GioFormFocusInvalidModule,
    MatInputModule,
    MatButtonModule,
    MatSnackBarModule,
    MatDialogModule,
    GioPermissionModule,
  ],
})
export class ClusterGeneralComponent implements OnInit {
  public initialCluster: Cluster;
  public clusterForm: FormGroup<{
    name: FormControl<string>;
    description: FormControl<string>;
  }>;
  public isLoadingData = true;
  public isReadOnly = false;
  public initialClusterGeneralFormsValue: unknown;

  private readonly destroyRef = inject(DestroyRef);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly clusterService = inject(ClusterService);
  private readonly snackBarService = inject(SnackBarService);
  private readonly matDialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly permissionService = inject(GioPermissionService);

  public ngOnInit() {
    this.isLoadingData = true;
    this.isReadOnly = !this.permissionService.hasAnyMatching(['cluster-definition-u']);
    this.clusterService
      .get(this.activatedRoute.snapshot.params.clusterId)
      .pipe(
        tap(cluster => {
          this.initialCluster = cluster;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.isLoadingData = false;

        this.clusterForm = new FormGroup({
          name: new FormControl({ value: this.initialCluster.name, disabled: this.isReadOnly }, [Validators.required]),
          description: new FormControl({ value: this.initialCluster.description, disabled: this.isReadOnly }),
        });

        this.initialClusterGeneralFormsValue = this.clusterForm.getRawValue();
      });
  }

  onSubmit() {
    const clusterToUpdate = {
      name: this.clusterForm.getRawValue().name,
      description: this.clusterForm.getRawValue().description,
      configuration: this.initialCluster.configuration,
    };

    this.clusterService
      .update(this.initialCluster.id, clusterToUpdate)
      .pipe(
        tap(() => this.snackBarService.success('Cluster details successfully updated!')),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.ngOnInit());
  }

  deleteCluster() {
    this.matDialog
      .open<GioConfirmAndValidateDialogComponent, GioConfirmAndValidateDialogData>(GioConfirmAndValidateDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: {
          title: `Delete Cluster`,
          content: `Are you sure you want to delete the Cluster?`,
          confirmButton: `Yes, delete it`,
          validationMessage: `Please, type in the name of the cluster <code>${this.initialCluster.name}</code> to confirm.`,
          validationValue: this.initialCluster.name,
          warning: `This operation is irreversible.`,
        },
        role: 'alertdialog',
        id: 'clusterDeleteDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.clusterService.delete(this.activatedRoute.snapshot.params.clusterId)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        map(() => this.snackBarService.success(`The Cluster has been deleted.`)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.router.navigate(['../../'], { relativeTo: this.activatedRoute });
      });
  }
}
