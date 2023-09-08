import { orb, parameters } from '@circleci/circleci-config-sdk';
import { config } from '../config';

export const artifactory = new orb.OrbImport('artifactory-orb', 'jfrog', 'artifactory-orb', config.orbs.artifactory, undefined, {
  jobs: {},
  executors: {},
  commands: {
    install: new parameters.CustomParametersList(),
    configure: new parameters.CustomParametersList(),
    upload: new parameters.CustomParametersList([
      new parameters.CustomParameter('source', 'string'),
      new parameters.CustomParameter('target', 'string'),
    ]),
  },
});
