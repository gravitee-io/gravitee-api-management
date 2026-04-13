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
import { combineLatest, Observable, of } from 'rxjs';
import { catchError, map, shareReplay, startWith, switchMap } from 'rxjs/operators';

import { ClusterService } from '../../../services-ngx/cluster.service';
import { KafkaClusterConnection } from '../../../entities/management-api-v2';

@Component({
  selector: 'cluster-connection-type',
  templateUrl: './cluster-connection-type.component.html',
  styleUrls: ['./cluster-connection-type.component.scss'],
  standalone: false,
})
export class ClusterConnectionTypeComponent extends FieldType<FieldTypeConfig> implements OnInit {
  filteredConnections$: Observable<KafkaClusterConnection[]>;
  connectionNotFound$: Observable<boolean>;

  constructor(private readonly clusterService: ClusterService) {
    super();
  }

  ngOnInit() {
    const clusterCrossIdControl = this.form.get('clusterCrossId');
    if (!clusterCrossIdControl) {
      this.filteredConnections$ = of([]);
      this.connectionNotFound$ = of(false);
      return;
    }

    this.filteredConnections$ = clusterCrossIdControl.valueChanges.pipe(
      startWith(clusterCrossIdControl.value),
      switchMap(crossId => {
        if (!crossId) {
          return of([]);
        }
        return this.clusterService.list(crossId, undefined, 1, 50, 'KAFKA_CLUSTER').pipe(
          map(result => {
            const cluster = result.data?.find(c => c.crossId === crossId);
            if (!cluster || !cluster.configuration) return [];
            const config = cluster.configuration as { connections?: KafkaClusterConnection[] };
            return config.connections ?? [];
          }),
          catchError(() => of([])),
        );
      }),
      shareReplay(1),
    );

    const currentValue$ = this.formControl.valueChanges.pipe(startWith(this.formControl.value ?? ''));

    this.connectionNotFound$ = combineLatest([currentValue$, this.filteredConnections$]).pipe(
      map(([term, connections]) => {
        if (!term) return false;
        return !connections.some(c => c.crossId === term);
      }),
      shareReplay(1),
    );
  }
}
