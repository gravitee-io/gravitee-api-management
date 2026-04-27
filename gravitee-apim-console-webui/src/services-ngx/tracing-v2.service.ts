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
import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';

import { Constants } from '../entities/Constants';
import { Trace, TracingGraph, TracingNode } from '../management/observability/tracing/tracing.model';

export interface SearchTracesParam {
  limit?: number;
  tags?: string;
  start?: number;
  end?: number;
}

@Injectable({
  providedIn: 'root',
})
export class TracingV2Service {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  searchTraces(param: SearchTracesParam = {}): Observable<Trace[]> {
    let params = new HttpParams().set('limit', String(param.limit ?? 20));
    if (param.tags) params = params.set('tags', param.tags);
    if (param.start != null) params = params.set('start', String(param.start));
    if (param.end != null) params = params.set('end', String(param.end));
    return this.http.get<Trace[]>(`${this.constants.env.v2BaseURL}/tracing/traces`, { params });
  }

  getTrace(traceId: string): Observable<Trace> {
    return this.http.get<Trace>(`${this.constants.env.v2BaseURL}/tracing/traces/${traceId}`);
  }

  getTracingGraph(traceId: string): Observable<TracingGraph> {
    return this.http
      .get<TracingGraph>(`${this.constants.env.v2BaseURL}/tracing/traces/${traceId}/graph`)
      .pipe(map(graph => this.addUiHints(graph)));
  }

  private addUiHints(graph: TracingGraph): TracingGraph {
    return {
      ...graph,
      nodes: graph.nodes.map(node => ({ ...node, icon: this.getIcon(node) })),
    };
  }

  private getIcon(node: TracingNode): string {
    switch (node.type) {
      case 'agent':
        return 'smart_toy';
      case 'llm':
        return 'psychology';
      case 'mcp_server':
        return 'dns';
      case 'api':
        return 'api';
      default:
        return 'circle';
    }
  }
}
