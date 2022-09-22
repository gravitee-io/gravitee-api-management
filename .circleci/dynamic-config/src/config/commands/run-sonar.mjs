import { CustomParameter, CustomParametersList, ReusableCommand, Run } from '../../sdk/index.mjs';

export function createRunSonarCommand() {
  return new ReusableCommand(`run-sonar`, [
    new Run({
      command: 'sonar-scanner -Dsonar.projectVersion=${APIM_VERSION}',
      working_directory: '<< parameters.working_directory >>',
    }),
  ]).defineParameter(
    'working_directory',
    'string',
    'gravitee-apim-rest-api',
    'Directory where the SonarCloud analysis will be run',
  );
}
