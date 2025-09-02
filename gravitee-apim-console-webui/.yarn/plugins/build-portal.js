const { execSync } = require('child_process');
module.exports = {
  name: 'yarn-doctor',
  factory: () => {
    return {
      hooks: {
        validateProject(project, report) {
          console.error('🚀  Launching script to build gravitee-markdown library for console-webui...');
          try {
            execSync('scripts/build-gravitee-markdown.sh', { stdio: 'inherit' });
          } catch (err) {
            console.error('❌  Error during build-gravitee-markdown.sh', err);
          }
        },
      },
    };
  },
};
