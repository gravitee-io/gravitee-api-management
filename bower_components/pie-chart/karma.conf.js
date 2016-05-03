module.exports = function(config) {
  config.set({
    basePath: '',

    files: [
      'bower_components/angular/angular.js',
      'bower_components/angular-mocks/angular-mocks.js',
      'bower_components/d3/d3.js',
      'dist/pie-chart.min.js',
      'dist/pie-chart.spec.js'
    ],

    frameworks: ["jasmine"],

    plugins: ["karma-jasmine", "karma-phantomjs-launcher", "karma-coverage"],

    preprocessors: {
      'dist/pie-chart.min.js': ['coverage']
    },

    exclude: [],
    
    coverageReporter: {
      type : 'text-summary'
    },

    reporters: ['progress', 'coverage'],

    port: 9876,

    runnerPort: 9100,

    autoWatch: true,
    
    browsers: ['PhantomJS']
  });
};
