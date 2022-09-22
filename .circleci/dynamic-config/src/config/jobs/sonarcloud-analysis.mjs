import { cache, Checkout, CustomParameter, ParameterizedJob, ReusedCommand, Run, workspace } from '../../sdk/index.mjs';
import { keeperOrb } from '../orbs/keeper.mjs';

export function createSonarCloudAnalysisJob(executor) {
  return new ParameterizedJob('sonar-cloud-analysis', executor)
    .addStep(
      new Run({
        name: 'Add SSH tool',
        command: 'apk add --no-cache openssh',
      }),
    )
    .addStep(new Checkout())
    .addStep(new workspace.Attach({ at: '.' }))
    .addStep(
      new cache.Restore({
        keys: [
          'gravitee-api-management-v8-sonarcloud-analysis-{{ .Branch }}-{{ checksum "pom.xml" }}',
          'gravitee-api-management-v8-sonarcloud-analysis-{{ .Branch }}-',
          'gravitee-api-management-v8-sonarcloud-analysis-',
        ],
      }),
    )
    .addStep(
      new ReusedCommand(keeperOrb.commands['env-export'], {
        'secret-url': 'keeper://9x9YgyU6DWzux4DPoHAzDQ/field/password',
        'var-name': 'SONAR_TOKEN',
      }),
    )
    .addStep(new ReusedCommand('get-apim-version'))
    .addStep(new ReusedCommand('run-sonar'))
    .addStep(new ReusedCommand('notify-on-failure'))
    .addStep(
      new cache.Save({
        paths: ['/opt/sonar-scanner/.sonar/cache'],
        key: 'gravitee-api-management-v8-sonarcloud-analysis-{{ .Branch }}-{{ checksum "pom.xml" }}',
        when: 'always',
      }),
    )
    .defineParameter('working_directory', 'string', '', 'The directory to run Sonar');
}
