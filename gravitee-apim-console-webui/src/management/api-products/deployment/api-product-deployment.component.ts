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

import { Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { EMPTY, merge, of, Subject } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { GioSaveBarModule } from '@gravitee/ui-particles-angular';

import { ApiProduct, UpdateApiProduct } from '../../../entities/management-api-v2/api-product';
import { ApiProductV2Service } from '../../../services-ngx/api-product-v2.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { TagService } from '../../../services-ngx/tag.service';
import { Tag } from '../../../entities/tag/tag';

interface DeploymentForm {
  tags: FormControl<string[]>;
}

@Component({
  selector: 'api-product-deployment',
  templateUrl: './api-product-deployment.component.html',
  styleUrls: ['./api-product-deployment.component.scss'],
  standalone: true,
  imports: [ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatSelectModule, GioSaveBarModule],
})
export class ApiProductDeploymentComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly apiProductV2Service = inject(ApiProductV2Service);
  private readonly permissionService = inject(GioPermissionService);
  private readonly snackBarService = inject(SnackBarService);
  private readonly tagService = inject(TagService);

  protected readonly isReadOnly = !this.permissionService.hasAnyMatching(['api_product-definition-u']);

  protected readonly shardingTags = toSignal(this.tagService.list().pipe(catchError(() => of([] as Tag[]))), {
    initialValue: [] as Tag[],
  });

  readonly form = new FormGroup<DeploymentForm>({
    tags: new FormControl<string[]>({ value: [], disabled: this.isReadOnly }, { nonNullable: true }),
  });
  readonly initialFormValue = signal<Record<string, unknown> | null>(null);
  private readonly reload$ = new Subject<void>();

  private readonly apiProductId = toSignal(this.activatedRoute.params.pipe(map(p => p['apiProductId'] ?? null)), {
    initialValue: null as string | null,
  });

  readonly apiProduct = toSignal(
    merge(toObservable(this.apiProductId), this.reload$).pipe(
      switchMap(() => {
        const id = this.apiProductId();
        return id
          ? this.apiProductV2Service.get(id).pipe(
              catchError(error => {
                this.snackBarService.error(error.error?.message || 'An error occurred while loading the API Product');
                return of(null);
              }),
            )
          : of(null);
      }),
    ),
    { initialValue: null as ApiProduct | null },
  );

  private readonly _formSync = effect(() => {
    const product = this.apiProduct();
    if (!product) return;

    this.form.reset({ tags: product.tags ?? [] });
    if (this.isReadOnly) {
      this.form.disable({ emitEvent: false });
    } else {
      this.form.enable({ emitEvent: false });
    }
    this.initialFormValue.set(this.form.getRawValue());
  });

  onSubmit(): void {
    const product = this.apiProduct();
    if (!product) return;

    const updateApiProduct: UpdateApiProduct = {
      name: product.name,
      version: product.version,
      description: product.description,
      apiIds: product.apiIds,
      groups: product.groups,
      disableMembershipNotifications: product.disableMembershipNotifications,
      tags: this.form.getRawValue().tags ?? [],
    };

    this.apiProductV2Service
      .update(product.id, updateApiProduct)
      .pipe(
        tap(() => {
          this.apiProductV2Service.notifyApiProductChanged();
          this.initialFormValue.set(this.form.getRawValue());
          this.snackBarService.success('Configuration successfully saved!');
          this.reload$.next();
        }),
        catchError(error => {
          this.snackBarService.error(error.error?.message || error.message || 'An error occurred while updating the API Product');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  onReset(): void {
    const initial = this.initialFormValue();
    if (!initial) return;
    this.form.reset(initial);
  }
}
