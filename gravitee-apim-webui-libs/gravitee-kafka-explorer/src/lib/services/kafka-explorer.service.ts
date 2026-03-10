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
import { HttpClient, HttpEventType } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  BrowseMessagesResponse,
  DescribeBrokerResponse,
  DescribeClusterResponse,
  DescribeTopicResponse,
  KafkaMessage,
  ListTopicsResponse,
  DescribeConsumerGroupResponse,
  ListConsumerGroupsResponse,
  OffsetMode,
} from '../models/kafka-cluster.model';

@Injectable({
  providedIn: 'root',
})
export class KafkaExplorerService {
  private readonly http = inject(HttpClient);

  describeCluster(baseURL: string, clusterId: string): Observable<DescribeClusterResponse> {
    return this.http.post<DescribeClusterResponse>(`${baseURL}/kafka-explorer/describe-cluster`, { clusterId });
  }

  listTopics(
    baseURL: string,
    clusterId: string,
    nameFilter?: string,
    page = 1,
    perPage = 25,
    sortBy?: string,
    sortOrder?: string,
  ): Observable<ListTopicsResponse> {
    const params: Record<string, string> = { page: page.toString(), perPage: perPage.toString() };
    if (sortBy) params['sortBy'] = sortBy;
    if (sortOrder) params['sortOrder'] = sortOrder;
    return this.http.post<ListTopicsResponse>(`${baseURL}/kafka-explorer/list-topics`, { clusterId, nameFilter }, { params });
  }

  describeTopic(baseURL: string, clusterId: string, topicName: string): Observable<DescribeTopicResponse> {
    return this.http.post<DescribeTopicResponse>(`${baseURL}/kafka-explorer/describe-topic`, { clusterId, topicName });
  }

  describeBroker(baseURL: string, clusterId: string, brokerId: number): Observable<DescribeBrokerResponse> {
    return this.http.post<DescribeBrokerResponse>(`${baseURL}/kafka-explorer/describe-broker`, { clusterId, brokerId });
  }

  listConsumerGroups(
    baseURL: string,
    clusterId: string,
    nameFilter?: string,
    page = 1,
    perPage = 25,
    topicFilter?: string,
    sortBy?: string,
    sortOrder?: string,
  ): Observable<ListConsumerGroupsResponse> {
    const body: Record<string, unknown> = { clusterId, nameFilter };
    if (topicFilter) body['topicFilter'] = topicFilter;
    const params: Record<string, string> = { page: page.toString(), perPage: perPage.toString() };
    if (sortBy) params['sortBy'] = sortBy;
    if (sortOrder) params['sortOrder'] = sortOrder;
    return this.http.post<ListConsumerGroupsResponse>(`${baseURL}/kafka-explorer/list-consumer-groups`, body, { params });
  }

  describeConsumerGroup(baseURL: string, clusterId: string, groupId: string): Observable<DescribeConsumerGroupResponse> {
    return this.http.post<DescribeConsumerGroupResponse>(`${baseURL}/kafka-explorer/describe-consumer-group`, { clusterId, groupId });
  }

  browseMessages(
    baseURL: string,
    clusterId: string,
    topicName: string,
    options?: {
      partition?: number;
      offsetMode?: OffsetMode;
      offsetValue?: number;
      keyFilter?: string;
      valueFilter?: string;
      limit?: number;
    },
  ): Observable<BrowseMessagesResponse> {
    const limit = options?.limit ?? 50;
    const body: Record<string, unknown> = { clusterId, topicName };
    if (options?.partition != null) body['partition'] = options.partition;
    if (options?.offsetMode) body['offsetMode'] = options.offsetMode;
    if (options?.offsetValue != null) body['offsetValue'] = options.offsetValue;
    if (options?.keyFilter) body['keyFilter'] = options.keyFilter;
    if (options?.valueFilter) body['valueFilter'] = options.valueFilter;
    return this.http.post<BrowseMessagesResponse>(`${baseURL}/kafka-explorer/browse-messages`, body, {
      params: { limit: limit.toString() },
    });
  }

  tailMessages(
    baseURL: string,
    clusterId: string,
    topicName: string,
    options?: { partition?: number; keyFilter?: string; valueFilter?: string; maxMessages?: number; durationSeconds?: number },
  ): Observable<KafkaMessage> {
    const queryParams: Record<string, string> = { clusterId, topicName };
    if (options?.partition != null) queryParams['partition'] = options.partition.toString();
    if (options?.keyFilter) queryParams['keyFilter'] = options.keyFilter;
    if (options?.valueFilter) queryParams['valueFilter'] = options.valueFilter;
    if (options?.maxMessages) queryParams['maxMessages'] = options.maxMessages.toString();
    if (options?.durationSeconds) queryParams['durationSeconds'] = options.durationSeconds.toString();

    return new Observable<KafkaMessage>(subscriber => {
      let lastParsedLength = 0;
      let buffer = '';

      const subscription = this.http
        .get(`${baseURL}/kafka-explorer/tail-messages`, {
          params: queryParams,
          responseType: 'text',
          observe: 'events',
          reportProgress: true,
        })
        .subscribe({
          next: event => {
            if (event.type === HttpEventType.DownloadProgress && event.partialText) {
              const newText = event.partialText.substring(lastParsedLength);
              lastParsedLength = event.partialText.length;
              buffer = this.processSseBuffer(buffer + newText, subscriber);
            } else if (event.type === HttpEventType.Response) {
              const remaining = typeof event.body === 'string' ? event.body.substring(lastParsedLength) : '';
              if (remaining || buffer) {
                this.processSseBuffer(buffer + remaining, subscriber);
              }
              subscriber.complete();
            }
          },
          error: err => subscriber.error(err),
        });

      return () => subscription.unsubscribe();
    });
  }

  private processSseBuffer(
    buffer: string,
    subscriber: { next: (msg: KafkaMessage) => void; complete: () => void; error: (err: unknown) => void },
  ): string {
    const blocks = buffer.split('\n\n');
    const incomplete = blocks.pop() ?? '';

    for (const block of blocks) {
      if (!block.trim()) continue;

      let eventName = '';
      let data = '';
      for (const line of block.split('\n')) {
        if (line.startsWith('event:')) eventName = line.slice(6).trim();
        else if (line.startsWith('data:')) data = line.slice(5).trim();
      }

      if (eventName === 'message' && data) {
        try {
          subscriber.next(JSON.parse(data));
        } catch (e) {
          subscriber.error(new Error('Failed to parse SSE message data'));
          return '';
        }
      } else if (eventName === 'done') {
        subscriber.complete();
        return '';
      } else if (eventName === 'error') {
        subscriber.error(new Error(data || 'SSE stream error'));
        return '';
      }
    }

    return incomplete;
  }
}
