print('In apis collection, \'views\' becomes \'categories\'');
db.apis.updateMany( {}, { $rename: { 'views': 'categories' } } );

print('Rename \'views\' collection in \'categories\'');
db.views.renameCollection('categories');

print('Change view links to category links');
db.pages.updateMany(
  { type: 'LINK', 'configuration.resourceType': 'view'},
  { $set: { 'configuration.resourceType': 'category' } }
);

print('Change parameters keys');
db.parameters.find({ _id: {$in: ['portal.apis.viewMode.enabled','portal.apis.apiheader.showviews.enabled','api.quality.metrics.views.weight']}}).forEach( param => {
  let newId = '';
  let oldId = param._id;
  if (param._id === 'portal.apis.viewMode.enabled') {
      newId = 'portal.apis.categoryMode.enabled';
  } else if (param._id === 'portal.apis.apiheader.showviews.enabled') {
      newId = 'portal.apis.apiheader.showcategories.enabled';
  } else if (param._id === 'api.quality.metrics.views.weight') {
      newId = 'api.quality.metrics.categories.weight';
  }
  if (newId) {
      param._id = newId;
      db.parameters.save(param);
      db.parameters.deleteOne({_id: oldId});
  }
});

print ('Update Audit logs to replace VIEW with CATEGORY');
db.audits.find({event: /VIEW/}).forEach(audit => {
  audit.event = audit.event.replace('VIEW', 'CATEGORY');
  audit.properties.CATEGORY = audit.properties.VIEW;
  delete audit.properties.VIEW;
  db.audits.save(audit);
});

print ('Update events payloads');
db.events.find({payload: /views/}).forEach( e => {
    e.payload=e.payload.replace('"views"','"categories"');
    db.events.save(e);
});
