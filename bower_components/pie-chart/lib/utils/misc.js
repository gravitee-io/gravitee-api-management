addDataForGauge: function(data, options) {
  if (!options || options.mode !== "gauge" || data.length !== 1) {
    return data;
  }
  
  data = data.concat();
  
  data.push({value: options.total - data[0].value, color: "white", __isComplement: true});

  return data;
},

getDefaultMargins: function() {
  return {top: 10, right: 10, bottom: 10, left: 10};
},

clean: function(element) {
  d3.select(element).select('svg').remove();
},

bootstrap: function(element, dimensions) {
  d3.select(element).classed('chart', true);
  
  var width = dimensions.width;
  var height = dimensions.height;

  var svg = d3.select(element).append('svg')
  .attr('width', width)
  .attr('height', height)
  .append('g')
  .attr('transform', 'translate(' +
      (dimensions.width*.5) + ',' +
      (dimensions.height*.5) + ')'
  );
  
  return svg;
},

getRadius: function(dimensions) {
  var d = dimensions;
  return Math.min(
    (d.width - d.left - d.right),
    (d.height - d.top - d.bottom)
  )*.5;
},

looksLikeSameSeries: function(newData, oldData) {
  if (newData.length !== oldData.length) {
    return false;
  }
  
  for (var i = 0; i < newData.length; i++) {
    if (oldData[i].label !== newData[i].label) {
      return false;
    }
    
    if (oldData[i].color !== newData[i].color) {
      return false;
    }
  }
  
  return true;
}