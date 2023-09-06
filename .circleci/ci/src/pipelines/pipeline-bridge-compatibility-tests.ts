import { Config } from '@circleci/circleci-config-sdk';
import { BridgeCompatibilityTestsWorkflow } from '../workflows';
import { CircleCIEnvironment } from './circleci-environment';

export function generateBridgeCompatibilityTestsConfig(environment: CircleCIEnvironment): Config {
  const dynamicConfig = new Config();
  const workflow = BridgeCompatibilityTestsWorkflow.create(dynamicConfig, environment);
  dynamicConfig.addWorkflow(workflow);
  return dynamicConfig;
}
