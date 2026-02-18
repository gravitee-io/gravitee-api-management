/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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

import { Component, DestroyRef, EventEmitter, inject, Input, OnInit, Output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmAndValidateDialogComponent, GioConfirmAndValidateDialogData, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { EMPTY } from 'rxjs';
import { catchError, filter, map, switchMap, tap } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';

import { ApiProduct } from '../../../../entities/management-api-v2/api-product';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ApiProductV2Service } from '../../../../services-ngx/api-product-v2.service';

@Component({
  selector: 'api-product-danger-zone',
  templateUrl: './api-product-danger-zone.component.html',
  styleUrls: ['./api-product-danger-zone.component.scss'],
  standalone: false,
})
export class ApiProductDangerZoneComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly matDialog = inject(MatDialog);
  private readonly snackBarService = inject(SnackBarService);
  private readonly apiProductV2Service = inject(ApiProductV2Service);
  private readonly destroyRef = inject(DestroyRef);

  @Input()
  public apiProduct!: ApiProduct;

  @Output()
  public reloadDetails = new EventEmitter<void>();

  public isReadOnly = false;

  ngOnInit(): void {
    // TODO: Check permissions for read-only
  }

  removeApis(): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Remove all APIs',
          content: 'Are you sure you want to remove all the APIs from this API Product?',
          confirmButton: 'Yes, remove them',
         // warning: 'This operation is irreversible.',
        },
        role: 'alertdialog',
        id: 'removeApisDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => confirm === true),
        switchMap(() => this.apiProductV2Service.deleteAllApisFromApiProduct(this.apiProduct.id)),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message || 'An error occurred while removing APIs.');
          return EMPTY;
        }),
        map(() => this.snackBarService.success('All APIs have been removed from the API Product.')),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.reloadDetails.emit();
      });
  }

  deleteApiProduct(): void {
    this.matDialog
      .open<GioConfirmAndValidateDialogComponent, GioConfirmAndValidateDialogData>(GioConfirmAndValidateDialogComponent, {
        width: '500px',
        data: {
          title: 'Delete API Product',
          content: 'Are you sure you want to delete this API Product?',
          confirmButton: 'Yes, delete it',
          validationMessage: `Please, type in the name of the API Product <code>${this.apiProduct.name}</code> to confirm.`,
          validationValue: this.apiProduct.name,
          warning: 'This operation is irreversible.',
        },
        role: 'alertdialog',
        id: 'deleteApiProductDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => confirm === true),
        switchMap(() => this.apiProductV2Service.delete(this.apiProduct.id)),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message || 'An error occurred while deleting the API Product.');
          return EMPTY;
        }),
        map(() => this.snackBarService.success('The API Product has been deleted.')),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.router.navigate(['../../'], { relativeTo: this.activatedRoute });
      });
  }
}


