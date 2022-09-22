import { cache, Checkout, Job, ReusedCommand, Run, workspace } from '../../sdk/index.mjs';

import { keeperOrb } from '../orbs/keeper.mjs';

export function createSetupJob(executor) {
  return new Job('setup', executor, [
    new Checkout(),
    new ReusedCommand(keeperOrb.commands['env-export'], {
      'secret-url': 'keeper://7CgijuGiFDSLynRJt1Dm9w/custom_field/xml',
      'var-name': 'MAVEN_SETTINGS',
    }),
    new Run({
      command: 'echo $MAVEN_SETTINGS > .gravitee.settings.xml',
    }),
    new cache.Restore({
      name: 'Restore Maven cache for compute-tag job',
      keys: ['gravitee-api-management-compute-tag-{{ .Branch }}-{{ checksum "pom.xml" }}'],
    }),
    new Run({
      command: 'echo $MAVEN_SETTINGS > .gravitee.settings.xml',
    }),
    new Run({
      name: 'Compute APIM version',
      command: [
        `export APIM_VERSION=$(mvn -s .gravitee.settings.xml -q -Dexec.executable=echo -Dexec.args='\${project.version}' --non-recursive exec:exec)`,
        `echo "export APIM_VERSION=$APIM_VERSION" >> $BASH_ENV`,
        'echo "Gravitee APIM version: ${APIM_VERSION}"',
        `echo $APIM_VERSION > .apim-version.txt`,
      ].join('\n'),
    }),
    new Run({
      name: 'Compute Tag for Docker images',
      command: [
        'export TAG=${CIRCLE_BRANCH}-latest',
        'echo "export TAG=$TAG" >> $BASH_ENV',
        'echo "Docker images will be tagged with: ${TAG}"',
        'echo $TAG > .docker-tag.txt',
      ].join('\n'),
    }),
    new cache.Save({
      name: 'Save Maven cache for compute-tag job',
      paths: ['~/.m2'],
      key: 'gravitee-api-management-compute-tag-{{ .Branch }}-{{ checksum "pom.xml" }}',
      when: 'always',
    }),
    new workspace.Persist({
      root: '.',
      paths: ['.gravitee-settings.xml', '.docker-tag.txt', '.apim-version.txt'],
    }),
  ]);
}
