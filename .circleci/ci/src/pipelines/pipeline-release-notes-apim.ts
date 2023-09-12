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
import { CircleCIEnvironment } from './circleci-environment';
import { Config } from '@circleci/circleci-config-sdk';
import { ReleaseNotesApimWorkflow } from '../workflows';
import { validateGraviteeioVersion } from '../utils';
import { initDynamicConfig } from './config-factory';

export function generateReleaseNotesApimConfig(environment: CircleCIEnvironment): Config {
  validateGraviteeioVersion(environment.graviteeioVersion);

  const dynamicConfig = initDynamicConfig();
  const workflow = ReleaseNotesApimWorkflow.create(dynamicConfig, environment);
  dynamicConfig.addWorkflow(workflow);
  return dynamicConfig;
}
