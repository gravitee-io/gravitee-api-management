const { execSync } = require('child_process');
module.exports = {
  name: 'gravitee-portal',
  factory: () => {
    return {
      hooks: {
        validateProject(project, report) {
          console.error('🚀  Launching script to build gravitee-markdown library for console-webui...');
          try {
            execSync('node scripts/build-gravitee-markdown.js', { stdio: 'inherit' });
          } catch (err) {
            console.error('❌  Error during build-gravitee-markdown.sh', err);
          }
        },
      },
    };
  },
};
