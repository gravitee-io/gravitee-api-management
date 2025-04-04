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
import { generatePackageBundleConfig } from './pipeline-package-bundle';
import { generateBridgeCompatibilityTestsConfig } from './pipeline-bridge-compatibility-tests';
import { generatePublishDockerImagesConfig } from './pipeline-publish-docker-images';
import { generateNexusStagingConfig } from './pipeline-nexus-staging';
import { generateRepositoriesTestsConfig } from './pipeline-repositories-tests';
import { generateReleaseNotesApimConfig } from './pipeline-release-notes-apim';
import { generateReleaseHelmConfig } from './pipeline-release-helm';
import { generateReleaseConfig } from './pipeline-release';
import { generateBuildRpmConfig } from './pipeline-build-rpm';
import { generateBuildDockerImagesConfig } from './pipeline-build-docker-images';
import { generatePullRequestsConfig } from './pipeline-pull-requests';
import { generateFullReleaseConfig } from './pipeline-full-release';
import { generateHelmTestsConfig } from './pipeline-helm-tests';
import { generateRunE2ETestsConfig } from './pipeline-run-e2e-tests';

export function buildCIPipeline(environment: CircleCIEnvironment): Config | null {
  switch (environment.action) {
    case 'pull_requests':
      return generatePullRequestsConfig(environment);
    case 'build_rpm':
      return generateBuildRpmConfig(environment);
    case 'build_docker_images':
      return generateBuildDockerImagesConfig(environment);
    case 'release_helm':
      return generateReleaseHelmConfig(environment);
    case 'full_release':
      return generateFullReleaseConfig(environment);
    case 'release':
      return generateReleaseConfig(environment);
    case 'package_bundle':
      return generatePackageBundleConfig(environment);
    case 'nexus_staging':
      return generateNexusStagingConfig(environment);
    case 'repositories_tests':
      return generateRepositoriesTestsConfig(environment);
    case 'release_notes_apim':
      return generateReleaseNotesApimConfig(environment);
    case 'bridge_compatibility_tests':
      return generateBridgeCompatibilityTestsConfig(environment);
    case 'publish_docker_images':
      return generatePublishDockerImagesConfig(environment);
    case 'helm_tests':
      return generateHelmTestsConfig(environment);
    case 'run_e2e_tests':
      return generateRunE2ETestsConfig(environment);
  }
  return null;
}
