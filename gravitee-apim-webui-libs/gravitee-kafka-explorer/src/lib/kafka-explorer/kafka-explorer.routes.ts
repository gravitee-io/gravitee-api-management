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
import { Routes } from '@angular/router';

import { KafkaExplorerComponent } from './kafka-explorer.component';
import { BrokerDetailPageComponent } from '../broker-detail/broker-detail-page.component';
import { BrokersPageComponent } from '../brokers/brokers-page.component';
import { ConsumerGroupDetailPageComponent } from '../consumer-group-detail/consumer-group-detail-page.component';
import { ConsumerGroupsPageComponent } from '../consumer-groups/consumer-groups-page.component';
import { TopicDetailPageComponent } from '../topic-detail/topic-detail-page.component';
import { TopicsPageComponent } from '../topics/topics-page.component';

export const KAFKA_EXPLORER_ROUTES: Routes = [
  {
    path: '',
    component: KafkaExplorerComponent,
    children: [
      { path: '', redirectTo: 'brokers', pathMatch: 'full' },
      { path: 'brokers', component: BrokersPageComponent },
      { path: 'brokers/:brokerId', component: BrokerDetailPageComponent },
      { path: 'topics', component: TopicsPageComponent },
      { path: 'topics/:topicName', component: TopicDetailPageComponent },
      { path: 'consumer-groups', component: ConsumerGroupsPageComponent },
      { path: 'consumer-groups/:groupId', component: ConsumerGroupDetailPageComponent },
    ],
  },
];
