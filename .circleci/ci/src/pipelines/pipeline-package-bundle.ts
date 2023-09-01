import { Config } from '@circleci/circleci-config-sdk';
import { CircleCIEnvironment } from '../pipelines/circleci-environment';
import { PackageBundleWorkflow } from '../workflows';

export function generatePackageBundleConfig(environment: CircleCIEnvironment): Config {
  if (environment.graviteeioVersion === undefined) {
    throw new Error('Graviteeio version is undefined - Please export CI_GRAVITEEIO_VERSION environment variable');
  }

  const dynamicConfig = new Config();
  const workflow = PackageBundleWorkflow.create(dynamicConfig, environment.graviteeioVersion, environment.isDryRun);
  dynamicConfig.addWorkflow(workflow);
  return dynamicConfig;
}
