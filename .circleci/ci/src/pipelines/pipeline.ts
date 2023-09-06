import { CircleCIEnvironment } from './circleci-environment';
import { Config } from '@circleci/circleci-config-sdk';
import { generatePackageBundleConfig } from './pipeline-package-bundle';
import { generatePublishDockerImagesConfig } from './pipeline-publish-docker-images';
import { generateNexusStagingConfig } from './pipeline-nexus-staging';
import { generateDbRepositoriesTestContainerConfig } from './pipeline-db-repositories-test-container';
import { generateReleaseNotesApimConfig } from './pipeline-release-notes-apim';

export function buildCIPipeline(environment: CircleCIEnvironment): Config | null {
  switch (environment.action) {
    case 'pull_requests':
      return null; // TODO: add buildPullRequest(...)
    case 'build_rpm_&_docker_images':
      return null; // TODO: add buildRprmAndDockerImages(...)
    case 'release_helm':
      return null; // TODO: add buildReleaseHelm(...)
    case 'full_release':
      return null; // TODO: add buildFullRelease(...)
    case 'release':
      return null; // TODO: add buildRelease(...)
    case 'package_bundle':
      return generatePackageBundleConfig(environment);
    case 'nexus_staging':
      return generateNexusStagingConfig(environment);
    case 'db_repositories_test_container':
      return generateDbRepositoriesTestContainerConfig(environment);
    case 'release_notes_apim':
      return generateReleaseNotesApimConfig(environment);
    case 'bridge_compatibility_tests':
      return null; // TODO: add buildBridgeCompatibilityTest(...)
    case 'publish_docker_images':
      return generatePublishDockerImagesConfig(environment);
  }
  return null;
}
