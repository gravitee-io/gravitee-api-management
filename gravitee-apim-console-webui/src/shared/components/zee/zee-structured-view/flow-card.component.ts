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
import { Component, Input } from '@angular/core';

/** Minimal shape of a Flow step as returned by the LLM engine */
export interface FlowStep {
  name?: string;
  policy?: string;
  description?: string;
  enabled?: boolean;
}

/** Minimal shape of a Flow selector as returned by the LLM engine */
export interface FlowSelector {
  type?: string;
  path?: string;
  channel?: string;
}

/** Minimal shape of the Flow resource returned by the LLM engine */
export interface GeneratedFlow {
  name?: string;
  enabled?: boolean;
  request?: FlowStep[];
  response?: FlowStep[];
  subscribe?: FlowStep[];
  publish?: FlowStep[];
  selectors?: FlowSelector[];
  tags?: string[];
}

@Component({
  selector: 'zee-flow-card',
  templateUrl: './flow-card.component.html',
  styleUrls: ['./flow-card.component.scss'],
  standalone: false,
})
export class FlowCardComponent {
  @Input() flow!: GeneratedFlow;

  /** Sections with steps to render (skips empties) */
  readonly stepSections = [
    { label: 'Request Steps', key: 'request' as const },
    { label: 'Response Steps', key: 'response' as const },
    { label: 'Subscribe Steps', key: 'subscribe' as const },
    { label: 'Publish Steps', key: 'publish' as const },
  ];
}
