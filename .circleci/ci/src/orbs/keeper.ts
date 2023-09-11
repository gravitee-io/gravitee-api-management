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

export const keeper = new orb.OrbImport('keeper', 'gravitee-io', 'keeper', config.orbs.keeper, undefined, {
  jobs: {},
  executors: {},
  commands: {
    install: new parameters.CustomParametersList(),
    'env-export': new parameters.CustomParametersList([
      new parameters.CustomParameter('secret-url', 'string'),
      new parameters.CustomParameter('var-name', 'string'),
    ]),
    exec: new parameters.CustomParametersList([
      new parameters.CustomParameter('step-name', 'string'),
      new parameters.CustomParameter('command', 'string'),
    ]),
  },
});
