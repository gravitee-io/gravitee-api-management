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

export const aquasec = new orb.OrbImport('aquasec', 'gravitee-io', 'aquasec', config.orbs.aquasec);

aquasec.jobs.fs_scan = new orb.OrbRef('fs_scan', new parameters.CustomParametersList(), aquasec);
aquasec.jobs.register_artifact = new orb.OrbRef(
  'register_artifact',
  new parameters.CustomParametersList([new parameters.CustomParameter('built_docker_image_file', 'string')]),
  aquasec,
);
aquasec.jobs.docker_image_scan = new orb.OrbRef(
  'docker_image_scan',
  new parameters.CustomParametersList([
    new parameters.CustomParameter('built_docker_image_file', 'string'),
    new parameters.CustomParameter('scanner_url', 'string'),
    new parameters.CustomParameter('skip_remote_docker_step', 'boolean'),
  ]),
  aquasec,
);
