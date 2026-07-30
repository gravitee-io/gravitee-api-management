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

export const aikido = new orb.OrbImport('aikido', 'gravitee-io', 'aikido', config.orbs.aikido);

// The scanner runs locally and bind-mounts the Docker socket, so the image has to be
// present in the local daemon: it must be pulled before the command runs, and the job
// using it must run on a machine executor.
aikido.commands['scan_docker_image'] = new orb.OrbRef(
  'scan_docker_image',
  new parameters.CustomParametersList([
    // aikido_api_key is left to its default (AIKIDO_API_KEY), which is the variable
    // name the Keeper env-export step exports.
    new parameters.CustomParameter('built_docker_image_file', 'string'),
    new parameters.CustomEnumParameter('fail_on', ['', 'low', 'medium', 'high', 'critical'], 'critical'),
  ]),
  aikido,
);
