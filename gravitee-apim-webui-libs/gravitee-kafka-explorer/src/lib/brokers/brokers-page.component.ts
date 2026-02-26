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
import { Component, inject } from '@angular/core';

import { BrokersComponent } from './brokers.component';
import { KafkaExplorerStore } from '../services/kafka-explorer-store.service';

@Component({
  selector: 'gke-brokers-page',
  standalone: true,
  imports: [BrokersComponent],
  template: `
    @if (store.clusterInfo(); as info) {
      <gke-brokers
        [nodes]="info.nodes"
        [controllerId]="info.controller.id"
        [clusterId]="info.clusterId"
        [controller]="info.controller"
        [totalTopics]="info.totalTopics"
        [totalPartitions]="info.totalPartitions"
      />
    }
  `,
})
export class BrokersPageComponent {
  store = inject(KafkaExplorerStore);
}
