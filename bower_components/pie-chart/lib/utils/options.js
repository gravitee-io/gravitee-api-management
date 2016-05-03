sanitizeOptions: function(options) {
  if (options.thickness === undefined) {
    options.thickness = 20;
  } else {
    options.thickness = parseInt(options.thickness);
  }
  
  
  if (options.mode === "gauge") {
    this.sanitizeGaugeOptions(options);
  }
  
  return options;
},

sanitizeGaugeOptions: function(options) {
  if (options.total === undefined) {
    options.total = 100;
  } else {
    options.total = parseInt(options.total);
  }
}