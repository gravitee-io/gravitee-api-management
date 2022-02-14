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

import { Component, Input, OnChanges } from '@angular/core';

import { PolicyListItem } from '../../../../../../entities/policy';
import { DebugResponse } from '../../models/DebugResponse';
import { HttpStatusCodeDescription } from '../../models/HttpStatusCodeDescription';
import { TimelineStep } from '../policy-studio-debug-timeline-card/policy-studio-debug-timeline-card.component';
import { PolicyScope, RequestDebugStep, ResponseDebugStep } from '../../models/DebugStep';

type ResponseDisplayableVM = {
  statusCode: number;
  statusCodeDescription: string;
  methodBadgeCSSClass: string;
  successfulRequest: boolean;
  errorRequest: boolean;
  timelineSteps: TimelineStep[];
};

@Component({
  selector: 'policy-studio-debug-response',
  template: require('./policy-studio-debug-response.component.html'),
  styles: [require('./policy-studio-debug-response.component.scss')],
})
export class PolicyStudioDebugResponseComponent implements OnChanges {
  @Input()
  public debugResponse?: DebugResponse;

  @Input()
  public listPolicies?: PolicyListItem[];

  public responseDisplayableVM: ResponseDisplayableVM;

  public selectedStep: { id: string; step?: RequestDebugStep | ResponseDebugStep | '🚧' };

  ngOnChanges(): void {
    if (this.debugResponse && !this.debugResponse.isLoading) {
      this.responseDisplayableVM = {
        statusCode: this.debugResponse.response?.statusCode,
        statusCodeDescription: this.debugResponse.response?.statusCode
          ? HttpStatusCodeDescription[this.debugResponse.response.statusCode].description
          : '',
        successfulRequest: 200 <= this.debugResponse.response?.statusCode && this.debugResponse.response?.statusCode < 300,
        errorRequest: 400 <= this.debugResponse.response?.statusCode && this.debugResponse.response?.statusCode < 600,
        timelineSteps: this.toTimelineSteps(this.debugResponse),
        methodBadgeCSSClass: `gio-method-badge-${this.debugResponse?.request.method.toLowerCase()}` ?? '',
      };
    }
  }

  onSelectTimelineStep(timelineStep: TimelineStep) {
    this.selectedStep = { id: timelineStep.id, step: undefined };

    if (timelineStep.mode === 'POLICY') {
      this.selectedStep.step = [...this.debugResponse.requestDebugSteps, ...this.debugResponse.responseDebugSteps].find(
        (value) => value.id === timelineStep.id,
      );
      return;
    }
    this.selectedStep.step = '🚧';
  }

  private toTimelineSteps(debugResponse: DebugResponse) {
    const timelineSteps: TimelineStep[] = [
      {
        id: 'requestClientApp',
        mode: 'CLIENT_APP',
      },
      {
        id: 'requestInput',
        mode: 'REQUEST_INPUT',
      },
      ...debugResponse.requestDebugSteps.map((debugStep) => {
        const policy = this.listPolicies?.find((p) => p.id === debugStep.policyId);

        return {
          executionTime: debugStep.duration,
          executionStatus: debugStep.status,
          policyName: policy?.name ?? debugStep.policyId,
          mode: 'POLICY' as const,
          icon: policy?.icon,
          flowName: policyScopeToDisplayableLabel(debugStep.scope),
          id: debugStep.id,
        };
      }),
      {
        id: 'requestOutput',
        mode: 'REQUEST_OUTPUT',
      },
      {
        id: 'backendTarget',
        mode: 'BACKEND_TARGET',
      },
      {
        id: 'responseInput',
        mode: 'RESPONSE_INPUT',
      },
      ...debugResponse.responseDebugSteps.map((debugStep) => {
        const policy = this.listPolicies?.find((p) => p.id === debugStep.policyId);

        return {
          executionTime: debugStep.duration,
          executionStatus: debugStep.status,
          policyName: policy?.name ?? debugStep.policyId,
          mode: 'POLICY' as const,
          icon: policy?.icon,
          flowName: policyScopeToDisplayableLabel(debugStep.scope),
          id: debugStep.id,
        };
      }),
      {
        id: 'responseOutput',
        mode: 'RESPONSE_OUTPUT',
      },
      {
        id: 'responseClientApp',
        mode: 'CLIENT_APP',
      },
    ];
    return timelineSteps;
  }
}

const policyScopeToDisplayableLabel = (scope: PolicyScope) => {
  switch (scope) {
    case 'ON_REQUEST':
    case 'ON_RESPONSE':
      return 'Header';

    case 'ON_REQUEST_CONTENT':
    case 'ON_RESPONSE_CONTENT':
      return 'Body';
  }
};
