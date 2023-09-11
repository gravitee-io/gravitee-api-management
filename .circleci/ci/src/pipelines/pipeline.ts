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
import { generateBuildRpmAndDockerImagesConfig } from './pipeline-build-rpm-and-docker-images';
import { generatePullRequestsConfig } from './pipeline-pull-requests';

export function buildCIPipeline(environment: CircleCIEnvironment): Config | null {
  switch (environment.action) {
    case 'pull_requests':
      return generatePullRequestsConfig(environment);
    case 'build_rpm_&_docker_images':
      return generateBuildRpmAndDockerImagesConfig(environment);
    case 'release_helm':
      return generateReleaseHelmConfig(environment);
    case 'full_release':
      return null; // TODO: add buildFullRelease(...)
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
  }
  return null;
}
