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

  public inspectorVM: {
    input?: RequestDebugStep | ResponseDebugStep;
    output?: RequestDebugStep | ResponseDebugStep;
  };

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
        methodBadgeCSSClass: `gio-method-badge-${this.debugResponse?.request?.method?.toLowerCase()}` ?? '',
      };
    }
  }

  onSelectTimelineStep(timelineStep: TimelineStep) {
    switch (timelineStep.mode) {
      case 'POLICY_REQUEST':
      case 'POLICY_RESPONSE':
        this.onSelectPolicy(timelineStep);
        break;

      case 'REQUEST_INPUT':
      case 'REQUEST_OUTPUT':
        this.onSelectInputOutput(timelineStep, 'REQUEST');
        break;

      case 'RESPONSE_INPUT':
      case 'RESPONSE_OUTPUT':
        this.onSelectInputOutput(timelineStep, 'RESPONSE');
        break;

      case 'CLIENT_APP':
        this.onSelectClientApp();
        break;

      default:
        break;
    }
  }

  onSelectTimelineStepOverview(timelineStep: TimelineStep) {
    const elementList = document.getElementById('card_' + timelineStep.id);
    elementList.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'center' });

    this.onSelectTimelineStep(timelineStep);
  }

  private toTimelineSteps(debugResponse: DebugResponse) {
    const timelineSteps: TimelineStep[] = [
      {
        id: 'requestClientApp',
        mode: 'CLIENT_APP',
        selected: false,
      },
      {
        id: 'requestInput',
        mode: 'REQUEST_INPUT',
        selected: false,
      },
      ...debugResponse.requestPolicyDebugSteps.map((debugStep) => {
        const policy = this.listPolicies?.find((p) => p.id === debugStep.policyId);

        return {
          executionTime: debugStep.duration,
          executionStatus: debugStep.status,
          policyName: policy?.name ?? debugStep.policyId,
          mode: 'POLICY_REQUEST' as const,
          icon: policy?.icon,
          flowName: policyScopeToDisplayableLabel(debugStep.scope),
          id: debugStep.id,
          selected: false,
        };
      }),
      {
        id: 'requestOutput',
        mode: 'REQUEST_OUTPUT',
        selected: false,
      },
      {
        id: 'backendTarget',
        mode: 'BACKEND_TARGET',
        selected: false,
      },
      {
        id: 'responseInput',
        mode: 'RESPONSE_INPUT',
        selected: false,
      },
      ...debugResponse.responsePolicyDebugSteps.map((debugStep) => {
        const policy = this.listPolicies?.find((p) => p.id === debugStep.policyId);

        return {
          executionTime: debugStep.duration,
          executionStatus: debugStep.status,
          policyName: policy?.name ?? debugStep.policyId,
          mode: 'POLICY_RESPONSE' as const,
          icon: policy?.icon,
          flowName: policyScopeToDisplayableLabel(debugStep.scope),
          id: debugStep.id,
          selected: false,
        };
      }),
      {
        id: 'responseOutput',
        mode: 'RESPONSE_OUTPUT',
        selected: false,
      },
      {
        id: 'responseClientApp',
        mode: 'CLIENT_APP',
        selected: false,
      },
    ];
    return timelineSteps;
  }

  private onSelectPolicy(timelineStep: TimelineStep) {
    if (isPolicySkippedStep(timelineStep)) {
      return;
    }
    if (timelineStep.mode === 'POLICY_REQUEST') {
      const steps = this.debugResponse.requestPolicyDebugSteps;
      const outputIndex = steps.findIndex((value) => value.id === timelineStep.id);
      const output = steps[outputIndex];
      const input = outputIndex > 0 ? steps[outputIndex - 1] : this.debugResponse.requestDebugSteps.input;
      this.inspectorVM = { input, output };
    } else if (timelineStep.mode === 'POLICY_RESPONSE') {
      const steps = this.debugResponse.responsePolicyDebugSteps;
      const outputIndex = steps.findIndex((value) => value.id === timelineStep.id);
      const output = steps[outputIndex];
      const input = outputIndex > 0 ? steps[outputIndex - 1] : this.debugResponse.responseDebugSteps.input;
      this.inspectorVM = { input, output };
    }

    this.responseDisplayableVM = {
      ...this.responseDisplayableVM,
      timelineSteps: this.responseDisplayableVM.timelineSteps.map((step) => ({ ...step, selected: step.id === timelineStep.id })),
    };
  }

  private onSelectInputOutput(_timelineStep: TimelineStep, policyMode: 'REQUEST' | 'RESPONSE') {
    this.responseDisplayableVM = {
      ...this.responseDisplayableVM,
      timelineSteps: this.responseDisplayableVM.timelineSteps.map((step) => ({
        ...step,
        selected: [`POLICY_${policyMode}`, `${policyMode}_OUTPUT`, `${policyMode}_INPUT`].includes(step.mode) && !isPolicySkippedStep(step),
      })),
    };

    if (policyMode === 'REQUEST') {
      this.inspectorVM = { ...this.debugResponse.requestDebugSteps };
    } else if (policyMode === 'RESPONSE') {
      this.inspectorVM = { ...this.debugResponse.responseDebugSteps };
    }
  }

  private onSelectClientApp() {
    this.responseDisplayableVM = {
      ...this.responseDisplayableVM,
      timelineSteps: this.responseDisplayableVM.timelineSteps.map((t) => ({
        ...t,
        selected: !isPolicySkippedStep(t),
      })),
    };

    this.inspectorVM = { input: this.debugResponse.requestDebugSteps.input, output: this.debugResponse.responseDebugSteps.output };
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

const isPolicySkippedStep = (timelineStep: TimelineStep) => {
  return (timelineStep.mode === 'POLICY_REQUEST' || timelineStep.mode === 'POLICY_RESPONSE') && timelineStep.executionStatus === 'SKIPPED';
};
