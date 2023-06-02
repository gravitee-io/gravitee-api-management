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
import { isFunction } from 'lodash';

import { PlanV2, PlanV4, UpdatePlanV2, UpdatePlanV4 } from '.';

import { BasePlan } from './basePlan';
import { UpdateBasePlan } from './updateBasePlan';

export function fakeBasePlan(modifier?: Partial<BasePlan> | ((base: BasePlan) => BasePlan)): BasePlan {
  const base: BasePlan = {
    id: 'aee23b1e-34b1-4551-a23b-1e34b165516a',
    name: 'Default plan',
    description: 'Default plan',
    characteristics: [],
    status: 'PUBLISHED',
    validation: 'AUTO',
    tags: ['tag1'],
    commentMessage: 'comment message',
    commentRequired: true,
    generalConditions: 'general conditions',
    security: {
      type: 'API_KEY',
      configuration: {},
    },
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakePlanV2(modifier?: Partial<PlanV2> | ((base: PlanV2) => PlanV2)): PlanV2 {
  const base: PlanV2 = {
    ...fakeBasePlan(modifier),
    definitionVersion: 'V2',
    flows: [
      {
        name: '',
        pathOperator: {
          path: '/',
          operator: 'STARTS_WITH',
        },
        condition: '',
        consumers: [],
        methods: [],
        pre: [
          {
            name: 'Mock',
            description: 'Saying hello to the world',
            enabled: true,
            policy: 'mock',
            configuration: { content: 'Hello world', status: '200' },
          },
        ],
        post: [],
        enabled: true,
      },
    ],
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakePlanV4(modifier?: Partial<PlanV4> | ((base: PlanV4) => PlanV4)): PlanV4 {
  const base: PlanV4 = {
    ...fakeBasePlan({ ...modifier }),
    definitionVersion: 'V4',
    flows: [
      {
        name: '',
        selectors: [
          {
            type: 'HTTP',
            pathOperator: 'EQUALS',
            path: '/my-path',
          },
        ],
        request: [
          {
            name: 'Mock',
            description: 'Saying hello to the world',
            enabled: true,
            policy: 'mock',
            configuration: { content: 'Hello world', status: '200' },
          },
        ],
        response: [],
        subscribe: [],
        publish: [],
        enabled: true,
      },
    ],
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeUpdateBasePlan(modifier?: Partial<UpdateBasePlan> | ((base: UpdateBasePlan) => UpdateBasePlan)): UpdateBasePlan {
  const base: UpdateBasePlan = {
    characteristics: ['one thing', 'two things'],
    commentMessage: 'A comment message',
    name: 'A plan name',
    description: 'A nice description',
    crossId: 'plan-cross-id',
    security: {
      configuration: {
        nice: 'configuration',
      },
    },
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeUpdatePlanV2(modifier?: Partial<UpdatePlanV2> | ((base: UpdatePlanV2) => UpdatePlanV2)): UpdatePlanV2 {
  const base: UpdatePlanV2 = {
    ...fakeUpdateBasePlan(modifier),
    definitionVersion: 'V2',
    flows: [
      {
        name: '',
        pathOperator: {
          path: '/',
          operator: 'STARTS_WITH',
        },
        condition: '',
        consumers: [],
        methods: [],
        pre: [
          {
            name: 'Mock',
            description: 'Saying hello to the world',
            enabled: true,
            policy: 'mock',
            configuration: { content: 'Hello world', status: '200' },
          },
        ],
        post: [],
        enabled: true,
      },
    ],
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeUpdatePlanV4(modifier?: Partial<UpdatePlanV4> | ((base: UpdatePlanV4) => UpdatePlanV4)): UpdatePlanV4 {
  const base: UpdatePlanV4 = {
    ...fakeUpdateBasePlan(modifier),
    definitionVersion: 'V4',
    flows: [
      {
        name: '',
        selectors: [
          {
            type: 'HTTP',
            pathOperator: 'EQUALS',
            path: '/my-path',
          },
        ],
        request: [
          {
            name: 'Mock',
            description: 'Saying hello to the world',
            enabled: true,
            policy: 'mock',
            configuration: { content: 'Hello world', status: '200' },
          },
        ],
        response: [],
        subscribe: [],
        publish: [],
        enabled: true,
      },
    ],
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
