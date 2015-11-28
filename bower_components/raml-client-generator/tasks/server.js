var gulp   = require('gulp');
var server = require('../test/fixtures/server');

/**
 * Spin up a test server for client implementations to hit.
 */
gulp.task('server', function (done) {
  // Initialise the application.
  var app = server.listen(4444, done);

  function close () {
    app.close();

    gulp.removeListener('err', close);
    gulp.removeListener('stop', close);
  }

  gulp.on('err', close);
  gulp.on('stop', close);
});
