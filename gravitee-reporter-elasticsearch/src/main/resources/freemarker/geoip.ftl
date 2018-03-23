<@compress single_line=true>
{
  "geoip" : {
    "field" : "remote-address"
  }
},
{
  "set": {
      "field": "geoip.city_name",
      "value": "Unknown",
      "override": false
   }
},
{
  "set": {
      "field": "geoip.continent_name",
      "value": "Unknown",
      "override": false
   }

},
{
  "set": {
    "field": "geoip.region_name",
    "value": "Unknown",
    "override": false
  }
}

</@compress>