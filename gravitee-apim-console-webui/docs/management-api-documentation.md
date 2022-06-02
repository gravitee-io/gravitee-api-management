# API Documentation

At this date, two types of document are supported:

* Swagger
* Markdown (MD)

You can access to your API data (name, version, description ...) on your API’s documentation by writing: `${api.name}` or `${api.metadata['foo-bar']}` in your documentation's content.

By default, documentation pages are in staging mode and will be visible to API owners and API members with specific documentation roles.
To make documentation visible for all users, you can switch on the *published* button.

You can set a page to be a *homepage* by clicking on the *house* button. API homepage will be visible in the Portal main page of the API.

You can also configure a page by clicking on the *settings* button. Page's configuration let you fetch page's content from an external resource such as Git repositories, or HTTP urls. In this case, you can configure an automatic fetch to refresh your pages periodically (see Auto Fetch here after).

(+) button let you create a new documentation page.

Another possibility is to add a whole directory. Click on the link at the top of the screen and import a directory.
If this directory contains a Gravitee descriptor (a file named `.gravitee.json`) we will add the content according to the descriptor.

Here is a descriptor sample:
```json
{
  "version": 1,
  "documentation": {
    "pages": [
      {
        "src": "/docs/readme.md",
        "dest": "/my/new/dir/",
        "name": "Homepage",
        "homepage": true
      },
      {
        "src": "/docs/doc2.md",
        "dest": "/my/new/dir/",
        "name": "Business"
      }
    ]
  }
}
```

## Auto Fetch

To fetch periodically your documentation from external source, you can enable the 'Auto Fetch' option and specify the fetch frequency.

This frequency definition uses a __cron expression__ that is a string consisting of six fields that describe the schedule representing second, minute, hour, day, month and weekday.

Here is some examples:

* Fetch every hour: `* * */1 * * *`
* At 00:00 on Saturday: `0 0 0 * * SAT`

__Note__: Platform administrator may have configured a max frequency that you cannot exceed. For example, if you configure a refresh every minute and the platform administrator configure a maximum period of 5 minutes, the refresh will be applied every 5 minutes.
