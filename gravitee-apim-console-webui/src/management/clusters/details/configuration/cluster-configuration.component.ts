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
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';
import { tap } from 'rxjs/operators';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { GioFormFocusInvalidModule, GioFormJsonSchemaModule, GioSaveBarModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatSnackBarModule } from '@angular/material/snack-bar';

import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ClusterService } from '../../../../services-ngx/cluster.service';
import { Cluster, UpdateCluster } from '../../../../entities/management-api-v2';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';

@Component({
  selector: 'cluster-configuration',
  templateUrl: './cluster-configuration.component.html',
  styleUrls: ['./cluster-configuration.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    GioSaveBarModule,
    GioFormFocusInvalidModule,
    MatSnackBarModule,
    GioFormJsonSchemaModule,
    GioIconsModule,
  ],
})
export class ClusterConfigurationComponent implements OnInit {
  public initialCluster: Cluster;
  public configForm: FormGroup<{
    config: FormControl<unknown>;
  }>;
  public isLoadingData = true;
  public isReadOnly = false;
  public securityJsonSchema: unknown;

  private readonly destroyRef = inject(DestroyRef);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly clusterService = inject(ClusterService);
  private readonly snackBarService = inject(SnackBarService);
  private readonly permissionService = inject(GioPermissionService);

  public ngOnInit() {
    this.isLoadingData = true;
    this.isReadOnly = !this.permissionService.hasAnyMatching(['cluster-configuration-u']);
    forkJoin([this.clusterService.get(this.activatedRoute.snapshot.params.clusterId), this.clusterService.getConfigurationSchema()])
      .pipe(
        tap(([cluster, schema]) => {
          this.initialCluster = cluster;
          this.securityJsonSchema = schema;
          this.isLoadingData = false;

          this.configForm = new FormGroup({
            config: new FormControl({ value: cluster.configuration, disabled: this.isReadOnly }),
          });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  onSubmit() {
    const configToUpdate: UpdateCluster = {
      name: this.initialCluster.name,
      description: this.initialCluster.description,
      configuration: this.configForm.get('config').value,
    };

    this.clusterService
      .update(this.initialCluster.id, configToUpdate)
      .pipe(
        tap(() => this.snackBarService.success('Cluster configuration successfully updated!')),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.ngOnInit());
  }

  protected onReset() {
    this.ngOnInit();
  }
}
