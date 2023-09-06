import { CircleCIEnvironment } from './circleci-environment';
import { Config } from '@circleci/circleci-config-sdk';
import { WorkflowNexusStaging } from '../workflows';
import { validateGraviteeioVersion } from '../utils';

export function generateNexusStagingConfig(environment: CircleCIEnvironment): Config {
  if (environment.isDryRun) {
    throw new Error('Dry Run - Nexus staging is deactivated if dry run is true');
  }
  validateGraviteeioVersion(environment.graviteeioVersion);

  const dynamicConfig = new Config();
  const workflow = WorkflowNexusStaging.create(dynamicConfig, environment);
  dynamicConfig.addWorkflow(workflow);
  return dynamicConfig;
}
