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
import { Config } from '@circleci/circleci-config-sdk';

export function initDynamicConfig(): Config {
  const dynamicConfig = new Config();
  /*
   * according to https://circleci.com/docs/dynamic-config/#how-dynamic-config-works, the parameters from config.yml, should be redefined also in the dynamic config.yml
   */
  dynamicConfig.defineParameter('gio_action', 'string', 'pull_requests', '', [
    'full_release',
    'release',
    'package_bundle',
    'nexus_staging',
    'pull_requests',
    'build_rpm',
    'build_docker_images',
    'release_notes_apim',
    'bridge_compatibility_tests',
    'publish_docker_images',
    'release_helm',
    'repositories_tests',
    'run_e2e_tests',
  ]);
  dynamicConfig.defineParameter('dry_run', 'boolean', true, 'Run in dry run mode?');
  dynamicConfig.defineParameter('docker_tag_as_latest', 'boolean', false, 'Is this version the latest version available?');
  dynamicConfig.defineParameter('graviteeio_version', 'string', '', 'Version of APIM to be used in docker images');
  dynamicConfig.defineParameter('apim_version_path', 'string', '/home/circleci/project/pom.xml', 'Path to pom.xml with APIM version');

  return dynamicConfig;
}
