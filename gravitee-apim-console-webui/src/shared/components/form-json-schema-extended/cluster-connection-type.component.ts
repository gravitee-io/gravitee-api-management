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
import { DeployedClusterConnection } from '../../../entities/management-api-v2';

@Component({
  selector: 'cluster-connection-type',
  templateUrl: './cluster-connection-type.component.html',
  styleUrls: ['./cluster-connection-type.component.scss'],
  standalone: false,
})
export class ClusterConnectionTypeComponent extends FieldType<FieldTypeConfig> implements OnInit {
  filteredConnections$: Observable<DeployedClusterConnection[]>;
  connectionNotFound$: Observable<boolean>;

  constructor(private readonly clusterService: ClusterService) {
    super();
  }

  ngOnInit() {
    const clusterCrossIdControl = this.form.get('clusterCrossId');
    if (!clusterCrossIdControl) {
      // The cluster is chosen in another form (e.g. a Message API Kafka endpoint group in CLUSTER mode, while this
      // field belongs to one of its endpoints): offer the connections of every deployed registered cluster. The
      // management API checks on save that the connection belongs to the group's cluster.
      this.initWithoutClusterField();
      return;
    }

    const allClusters$ = this.clusterService.listDeployed().pipe(
      catchError(() => of([])),
      shareReplay(1),
    );

    this.filteredConnections$ = clusterCrossIdControl.valueChanges.pipe(
      startWith(clusterCrossIdControl.value),
      switchMap(crossId => {
        if (!crossId) return of([]);
        return allClusters$.pipe(
          map(clusters => {
            const cluster = clusters.find(c => c.crossId === crossId);
            return cluster?.connections ?? [];
          }),
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

  private initWithoutClusterField() {
    const allConnections$: Observable<DeployedClusterConnection[]> = this.clusterService.listDeployed('KAFKA_CLUSTER').pipe(
      catchError(() => of([])),
      map(clusters =>
        clusters.flatMap(cluster =>
          (cluster.connections ?? []).map(connection => ({ ...connection, name: `${cluster.name} / ${connection.name}` })),
        ),
      ),
      shareReplay(1),
    );
    const currentValue$ = this.formControl.valueChanges.pipe(startWith(this.formControl.value ?? ''));

    this.filteredConnections$ = combineLatest([currentValue$, allConnections$]).pipe(
      map(([term, connections]) => {
        if (!term) return connections;
        const lowerTerm = `${term}`.toLowerCase();
        return connections.filter(c => c.name.toLowerCase().includes(lowerTerm) || c.crossId.toLowerCase().includes(lowerTerm));
      }),
      shareReplay(1),
    );
    this.connectionNotFound$ = combineLatest([currentValue$, allConnections$]).pipe(
      map(([term, connections]) => !!term && !connections.some(c => c.crossId === term)),
      shareReplay(1),
    );
  }
}
