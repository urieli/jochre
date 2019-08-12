![Jochre Logo](https://raw.githubusercontent.com/wiki/urieli/jochre/images/jochreLogo300px.png)

The Jochre Search Django application builds a front-end search engine interface over Jochre Search.

## Source for graphics usage:

<img src="https://upload.wikimedia.org/wikipedia/commons/5/59/Keyboard_icon1.png" alt="Keyboard icon" width="80" />

```
https://commons.wikimedia.org/wiki/File:Keyboard_icon1.png
```

<img src="https://upload.wikimedia.org/wikipedia/commons/8/89/Portrait_Placeholder.png" alt="Profile placeholder" width="40" />

```
https://upload.wikimedia.org/wikipedia/commons/8/89/Portrait_Placeholder.png
```

<img src="https://upload.wikimedia.org/wikipedia/commons/6/64/Edit_icon_%28the_Noun_Project_30184%29.svg" alt="Edit icon" width="40" />

```
https://upload.wikimedia.org/wikipedia/commons/6/64/Edit_icon_%28the_Noun_Project_30184%29.svg
```

## Adding custom templates and styling

You can easily extend or override the core templates and styling by creating your own Django app in this directory. By default this is assumed to be `custom` in the `.gitignore` file, but in practice you can call it whatever you want.

The simplest way to manage these files is to create them as a separate repository that you check out into `custom`. The structure would be something like the following;

```
locale/
static/
└── custom/
    └── style.css
templates/
├── search/
│   └──header.html
└──privacy.html
```

You can see here I've created a subdirectory for my static assets to prevent collisions with the base assets - if you want to override assets entirely, you can put them in the same location in the `static` directory as in the `jochre` app.

Once you've created this app, you'll need to add it to `INSTALLED_APPS` in your `settings_local.py`. Add it to the top of the list above `jochre` so that it takes precedence.

Templates can either be overridden entirely, or extended using the built-in template blocks. For more information on extending templates, see https://tutorial.djangogirls.org/en/template_extending/.

If you add static assets, you'll need to rebuild them by running `python3 manage.py collectstatic` from within this directory. For more information on managing static assets, see https://docs.djangoproject.com/en/2.2/howto/static-files/.
