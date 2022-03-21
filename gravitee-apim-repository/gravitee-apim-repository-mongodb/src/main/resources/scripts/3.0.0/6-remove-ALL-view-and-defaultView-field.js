print('Remove default view "ALL" and "defaultView" field  from others views');
db.views.remove({'_id': 'all'});
db.views.update({}, {$unset: {'defaultView': ''}}, false, true);
