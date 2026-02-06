const { execSync } = require('child_process');
module.exports = {
  name: 'gravitee-dashboard',
  factory: () => {
    return {
      hooks: {
        validateProject(project, report) {
          console.error('🚀  Launching script to build gravitee-dashboard library for console-webui...');
          try {
            execSync('node scripts/build-gravitee-dashboard.js', { stdio: 'inherit' });
          } catch (err) {
            console.error('❌  Error during build-gravitee-dashboard.sh', err);
          }
        },
      },
    };
  },
};
