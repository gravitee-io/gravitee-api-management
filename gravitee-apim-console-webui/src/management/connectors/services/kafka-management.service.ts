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
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Constants } from '../../../entities/Constants';

export interface KafkaConnectionRequest {
  bootstrapServers: string;
  securityProtocol: string;
  saslMechanism?: string;
  saslUsername?: string;
  saslPassword?: string;
}

export interface KafkaConnectionResponse {
  success: boolean;
  message: string;
}

export interface TopicInfo {
  id: string;
  name: string;
  partitions: number;
  replicas: number;
  messages: string;
}

export interface CreateTopicRequest {
  name: string;
  partitions: number;
  replicas: number;
  retentionMs?: number;
  cleanupPolicy?: string;
}

@Injectable({
  providedIn: 'root',
})
export class KafkaManagementService {
  constructor(
    private readonly http: HttpClient,
    private readonly constants: Constants,
  ) {}

  testConnection(request: KafkaConnectionRequest): Observable<KafkaConnectionResponse> {
    const url = `${this.constants.env.v2BaseURL}/kafka/test-connection`;
    return this.http.post<KafkaConnectionResponse>(url, request);
  }

  listTopics(clusterId: string, bootstrapServers: string, securityProtocol: string = 'PLAINTEXT'): Observable<TopicInfo[]> {
    const url = `${this.constants.env.v2BaseURL}/kafka/clusters/${clusterId}/topics`;
    let params = new HttpParams()
      .set('bootstrapServers', bootstrapServers)
      .set('securityProtocol', securityProtocol);

    return this.http.get<TopicInfo[]>(url, { params });
  }

  createTopic(
    clusterId: string,
    bootstrapServers: string,
    securityProtocol: string,
    topic: CreateTopicRequest,
  ): Observable<TopicInfo> {
    const url = `${this.constants.env.v2BaseURL}/kafka/clusters/${clusterId}/topics`;
    let params = new HttpParams()
      .set('bootstrapServers', bootstrapServers)
      .set('securityProtocol', securityProtocol);

    return this.http.post<TopicInfo>(url, topic, { params });
  }

  deleteTopic(clusterId: string, bootstrapServers: string, securityProtocol: string, topicName: string): Observable<void> {
    const url = `${this.constants.env.v2BaseURL}/kafka/clusters/${clusterId}/topics/${topicName}`;
    let params = new HttpParams()
      .set('bootstrapServers', bootstrapServers)
      .set('securityProtocol', securityProtocol);

    return this.http.delete<void>(url, { params });
  }
}
