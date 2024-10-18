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
import { FlowPhase as PolicyStudioFlowPhase } from '@gravitee/ui-policy-studio-angular';

export type FlowPhase = 'REQUEST' | 'RESPONSE' | 'MESSAGE_REQUEST' | 'MESSAGE_RESPONSE' | 'INTERACT' | 'CONNECT' | 'PUBLISH' | 'SUBSCRIBE';

export const toPolicyStudioFlowPhase = (flowPhase: FlowPhase): PolicyStudioFlowPhase => {
  switch (flowPhase) {
    case 'REQUEST':
      return 'REQUEST';
    case 'RESPONSE':
      return 'RESPONSE';
    case 'PUBLISH':
    case 'MESSAGE_REQUEST':
      return 'PUBLISH';
    case 'SUBSCRIBE':
    case 'MESSAGE_RESPONSE':
      return 'SUBSCRIBE';
    case 'INTERACT':
      return 'INTERACT';
    case 'CONNECT':
      return 'CONNECT';
  }
};

export const toReadableFlowPhase = (flowPhase: FlowPhase): string => {
  switch (flowPhase) {
    case 'REQUEST':
      return 'Request';
    case 'RESPONSE':
      return 'Response';
    case 'PUBLISH':
    case 'MESSAGE_REQUEST':
      return 'Publish';
    case 'SUBSCRIBE':
    case 'MESSAGE_RESPONSE':
      return 'Subscribe';
  }
};
