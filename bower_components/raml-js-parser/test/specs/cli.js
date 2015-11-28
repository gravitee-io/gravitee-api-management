/* global describe, it */

'use strict';

var chai = require('chai');
var exec = require('child_process').exec;
var join = require('path').join;

var should = chai.should();

var PARSER_BIN = join(__dirname, '../../bin/raml-parser');

describe('CLI', function () {
  it('should error when missing file argument', function (done) {
    exec('node ' + PARSER_BIN, function (err, stdout, stderr) {
      err.should.match(/No filename specified/);
      stdout.should.equal('');
      stderr.should.match(/No filename specified/);

      return done();
    });
  });

  it('should error when file is missing', function (done) {
    var file = join(__dirname, '../raml-files/magic-file.yml');
    var match = new RegExp('cannot read ' + file);

    exec(['node', PARSER_BIN, file].join(' '), function (err, stdout, stderr) {
      err.should.match(match);
      stdout.should.equal('');
      stderr.should.match(match);

      return done();
    });
  });

  it('should parse RAML using CLI', function (done) {
    exec([
      'node',
      PARSER_BIN,
      join(__dirname, '../raml-files/local.yml')
    ].join(' '), function (err, stdout, stderr) {
      should.not.exist(err);
      stderr.should.equal('');

      var output = JSON.parse(stdout);

      output.title.should.equal('MyApi');
      output.documentation.should.be.an('array');

      return done(err);
    });
  });
});
