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
import { Component, OnInit } from '@angular/core';
import { FieldType, FieldTypeConfig } from '@ngx-formly/core';
import { Observable, of } from 'rxjs';
import { catchError, map, shareReplay, startWith, switchMap } from 'rxjs/operators';

import { ClusterService } from '../../../services-ngx/cluster.service';
import { DeployedCluster } from '../../../entities/management-api-v2';

@Component({
  selector: 'cluster-type',
  templateUrl: './cluster-type.component.html',
  styleUrls: ['./cluster-type.component.scss'],
  standalone: false,
})
export class ClusterTypeComponent extends FieldType<FieldTypeConfig> implements OnInit {
  filteredClusters$: Observable<DeployedCluster[]>;
  clusterNotFound$: Observable<boolean>;

  constructor(private readonly clusterService: ClusterService) {
    super();
  }

  ngOnInit() {
    const allClusters$ = this.clusterService.listDeployed().pipe(
      catchError(() => of([])),
      shareReplay(1),
    );

    this.filteredClusters$ = this.formControl.valueChanges.pipe(
      startWith(this.formControl.value ?? ''),
      switchMap(term => {
        if (!term) return allClusters$;
        return allClusters$.pipe(
          map(clusters =>
            clusters.filter(c => c.name.toLowerCase().includes(term.toLowerCase()) || c.crossId.toLowerCase().includes(term.toLowerCase())),
          ),
        );
      }),
    );

    this.clusterNotFound$ = this.formControl.valueChanges.pipe(
      startWith(this.formControl.value ?? ''),
      switchMap(term => {
        if (!term) return of(false);
        return allClusters$.pipe(map(clusters => !clusters.some(c => c.crossId === term)));
      }),
      shareReplay(1),
    );
  }
}
