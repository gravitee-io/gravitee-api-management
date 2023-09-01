import { orb, parameters } from '@circleci/circleci-config-sdk';
import { config } from '../config';

export const keeper = new orb.OrbImport('keeper', 'gravitee-io', 'keeper', config.orbs.keeper, undefined, {
  jobs: {},
  executors: {},
  commands: {
    install: new parameters.CustomParametersList(),
    'env-export': new parameters.CustomParametersList([
      new parameters.CustomParameter('secret-url', 'string'),
      new parameters.CustomParameter('var-name', 'string'),
    ]),
    exec: new parameters.CustomParametersList([
      new parameters.CustomParameter('step-name', 'string'),
      new parameters.CustomParameter('command', 'string'),
    ]),
  },
});
