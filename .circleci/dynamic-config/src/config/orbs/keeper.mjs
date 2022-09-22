import { CustomParameter, CustomParametersList, OrbImport } from '../../sdk/index.mjs';

export const keeperOrb = new OrbImport('keeper', 'gravitee-io', 'keeper', '0.6.2', undefined, {
  jobs: {},
  executors: {},
  commands: {
    'env-export': new CustomParametersList([
      new CustomParameter('secret-url', 'string'),
      new CustomParameter('var-name', 'string'),
    ]),
  },
});
