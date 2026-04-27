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
import { CUSTOM_ELEMENTS_SCHEMA, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DebugModeModule } from '../../../../api/debug-mode/debug-mode.module';
import { DebugResponse } from '../../../../api/debug-mode/models/DebugResponse';
import { PolicyListItem } from '../../../../../entities/policy';
import { Trace } from '../../tracing.model';
import { traceToDebugResponse } from './trace-to-debug-response';

@Component({
  selector: 'app-debug-view',
  standalone: true,
  imports: [CommonModule, DebugModeModule],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <div class="debug-view-container">
      @if (debugResponse) {
        <div class="debug-view-slot">
          <debug-mode-response [debugResponse]="debugResponse" [listPolicies]="policyList"></debug-mode-response>
        </div>
      } @else {
        <div class="empty">
          <p>No debug data available for this trace.</p>
        </div>
      }
    </div>
  `,
  styles: [
    `
      :host {
        display: flex;
        flex-direction: column;
        flex: 1;
        min-height: 0;
        overflow: hidden;
      }
      .debug-view-container {
        flex: 1 1 auto;
        min-height: 700px;
        display: flex;
        overflow: auto;
      }
      /* .debug-mode-response sets height: 100% internally — give it a positioned slot so that height resolves. */
      .debug-view-slot {
        position: relative;
        flex: 1 1 auto;
        width: 100%;
        min-height: 700px;
      }
      :host ::ng-deep debug-mode-response {
        position: absolute;
        inset: 0;
        display: block;
      }
      .empty {
        padding: 60px 20px;
        text-align: center;
        color: #5c5959;
        flex: 1;
      }
    `,
  ],
})
export class DebugViewComponent implements OnChanges {
  @Input() trace!: Trace | null;

  debugResponse: DebugResponse | null = null;
  policyList: PolicyListItem[] = [];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['trace']) {
      this.debugResponse = traceToDebugResponse(this.trace);
      this.policyList = this.buildPolicyList();
    }
  }

  private buildPolicyList(): PolicyListItem[] {
    if (!this.debugResponse) return [];
    const ids = new Set<string>();
    for (const step of [...this.debugResponse.requestPolicyDebugSteps, ...this.debugResponse.responsePolicyDebugSteps]) {
      if (step.policyId) ids.add(step.policyId);
    }
    return Array.from(ids).map(id => ({
      id,
      name: prettifyPolicyName(id),
      description: '',
      version: '',
      schema: '',
      icon: '',
      onRequest: true,
      onResponse: true,
    }));
  }
}

function prettifyPolicyName(id: string): string {
  return id
    .replace(/^(policy|security)-/, '')
    .replace(/[-_]/g, ' ')
    .replace(/\b\w/g, m => m.toUpperCase());
}
