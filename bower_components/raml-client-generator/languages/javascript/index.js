var generator = require('../../lib/generator');

/**
 * Export a client generator instance.
 *
 * @type {Function}
 */
module.exports = generator({
  templates: {
    '.gitignore':   require('./templates/.gitignore.hbs'),
    'index.js':     require('./templates/index.js.hbs'),
    'README.md':    require('./templates/README.md.hbs'),
    'INSTALL.md':   require('./templates/INSTALL.md.hbs'),
    'package.json': require('./templates/package.json.hbs')
  },
  format: {
    variable: require('camel-case')
  },
  partials: {
    auth:      require('./partials/auth.js.hbs'),
    utils:     require('./partials/utils.js.hbs'),
    client:    require('./partials/client.js.hbs'),
    resources: require('./partials/resources.js.hbs')
  },
  helpers: {
    stringify:         require('javascript-stringify'),
    dependencies:      require('./helpers/dependencies'),
    requestSnippet:    require('./helpers/request-snippet'),
    parametersSnippet: require('./helpers/parameters-snippet')
  }
});
