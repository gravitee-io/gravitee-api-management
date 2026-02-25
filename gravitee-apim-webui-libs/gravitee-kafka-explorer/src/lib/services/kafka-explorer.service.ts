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
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { DescribeClusterResponse, DescribeTopicResponse, ListTopicsResponse } from '../models/kafka-cluster.model';

@Injectable({
  providedIn: 'root',
})
export class KafkaExplorerService {
  private readonly http = inject(HttpClient);

  describeCluster(baseURL: string, clusterId: string): Observable<DescribeClusterResponse> {
    return this.http.post<DescribeClusterResponse>(`${baseURL}/kafka-explorer/describe-cluster`, { clusterId });
  }

  listTopics(baseURL: string, clusterId: string, nameFilter?: string, page = 1, perPage = 10): Observable<ListTopicsResponse> {
    return this.http.post<ListTopicsResponse>(
      `${baseURL}/kafka-explorer/list-topics`,
      { clusterId, nameFilter },
      {
        params: { page: page.toString(), perPage: perPage.toString() },
      },
    );
  }

  describeTopic(baseURL: string, clusterId: string, topicName: string): Observable<DescribeTopicResponse> {
    return this.http.post<DescribeTopicResponse>(`${baseURL}/kafka-explorer/describe-topic`, { clusterId, topicName });
  }
}
