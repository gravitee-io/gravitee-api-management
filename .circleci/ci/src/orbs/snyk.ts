import { orb, parameters } from '@circleci/circleci-config-sdk';
import { config } from '../config';

export const snyk = new orb.OrbImport('snyk', 'snyk', 'snyk', config.orbs.snyk, undefined, {
  jobs: {},
  executors: {},
  commands: {
    install: new parameters.CustomParametersList(),
  },
});
