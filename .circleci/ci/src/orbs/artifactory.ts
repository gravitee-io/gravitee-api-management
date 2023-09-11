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

export const artifactory = new orb.OrbImport('artifactory-orb', 'jfrog', 'artifactory-orb', config.orbs.artifactory, undefined, {
  jobs: {},
  executors: {},
  commands: {
    install: new parameters.CustomParametersList(),
    configure: new parameters.CustomParametersList(),
    upload: new parameters.CustomParametersList([
      new parameters.CustomParameter('source', 'string'),
      new parameters.CustomParameter('target', 'string'),
    ]),
  },
});
