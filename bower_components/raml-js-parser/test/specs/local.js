'use strict';

if (typeof window === 'undefined') {
    var raml = require('../../lib/raml.js');
    var chai = require('chai');
    var q = require('q');
    var chaiAsPromised = require('chai-as-promised');

    chai.should();
    chai.use(chaiAsPromised);
} else {
    var raml = RAML.Parser;
    chai.should();
}

describe('Parser', function() {
    describe('Include', function() {
        it('should succeed on including local files', function(done) {
            raml.loadFile('test/raml-files/local.yml').should.eventually.deep.equal({
                title: 'MyApi',
                documentation: [
                    { title: 'Getting Started', content: '# Getting Started\n\nThis is a getting started guide.' }
                ]
            }).and.notify(done);
        });

        it('should succeed with windows file systems', function (done) {
            var expectedPath = 'C:\\Users\\test\\example.raml';

            var document = [
                '#%RAML 0.8',
                'title: Example API'
            ].join('\n');

            var reader = new raml.FileReader(function (path) {
              path.should.equal(expectedPath);

              return q(document);
            });

            raml.loadFile(expectedPath, { reader: reader }).should.eventually.deep.equal({
                title: 'Example API'
            }).and.notify(done);
        });
    });
});
