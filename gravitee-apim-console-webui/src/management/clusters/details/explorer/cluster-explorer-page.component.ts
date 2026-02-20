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
import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { KafkaExplorerComponent } from '@gravitee/gravitee-kafka-explorer';

import { Constants } from '../../../../entities/Constants';

@Component({
  selector: 'cluster-explorer-page',
  standalone: true,
  imports: [KafkaExplorerComponent],
  template: `<gke-kafka-explorer [baseURL]="baseURL" [clusterId]="clusterId" />`,
  styleUrls: ['./cluster-explorer-page.component.scss'],
})
export class ClusterExplorerPageComponent {
  baseURL: string;
  clusterId: string;

  constructor() {
    this.baseURL = inject(Constants).env.v2BaseURL;
    this.clusterId = inject(ActivatedRoute).snapshot.params.clusterId;
  }
}
