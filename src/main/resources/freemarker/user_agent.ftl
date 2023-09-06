<@compress single_line=true>
{
  "user_agent" : {
    <#if userAgentRegexFile??>"regex_file": "${userAgentRegexFile}",</#if>
    "field": "user-agent"
  }
},
{
  "set": {
      "field": "user_agent.name",
      "value": "Unknown",
      "override": false
   }
},
{
  "set": {
      "field": "user_agent.os_name",
      "value": "{{user_agent.os.name}}",
      "override": false
   }
},
{
  "set": {
      "field": "user_agent.os_name",
      "value": "Unknown",
      "override": false
   }
}
</@compress>
