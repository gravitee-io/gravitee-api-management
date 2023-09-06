import { orb, parameters } from '@circleci/circleci-config-sdk';
import { config } from '../config';

export const github = new orb.OrbImport('gh', 'circleci', 'github-cli', config.orbs.github, undefined, {
  jobs: {},
  executors: {},
  commands: {
    setup: new parameters.CustomParametersList(),
  },
});
