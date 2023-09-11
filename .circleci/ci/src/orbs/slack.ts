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
import { orb, parameters } from '@circleci/circleci-config-sdk';
import { config } from '../config';

export const slack = new orb.OrbImport('slack', 'circleci', 'slack', config.orbs.slack, undefined, {
  jobs: {},
  executors: {},
  commands: {
    notify: new parameters.CustomParametersList([
      new parameters.CustomParameter('channel', 'string'),
      new parameters.CustomParameter('custom', 'string'),
      new parameters.CustomParameter('branch_pattern', 'string'),
      new parameters.CustomParameter('event', 'string'),
      new parameters.CustomParameter('template', 'string'),
    ]),
  },
});
