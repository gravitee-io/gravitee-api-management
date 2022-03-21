print('In apis collection, scopes of json-validation policy have to be changed from REQUEST/RESPONSE to REQUEST_CONTENT/RESPONSE_CONTENT');
db.apis.find({ definition: /"json-validation" : {"scope":"REQUEST"|"json-validation" : {"scope":"RESPONSE"/}).forEach(api => {
    api.definition = api.definition
        .replace(/"json-validation" : {"scope":"REQUEST"/, "\"json-validation\" : {\"scope\":\"REQUEST_CONTENT\"")
        .replace(/"json-validation" : {"scope":"RESPONSE"/, "\"json-validation\" : {\"scope\":\"RESPONSE_CONTENT\"");
    db.apis.save(api);
});
