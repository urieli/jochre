#!/usr/bin/env python
# -*- coding: utf-8 -*-

# Sample settings in Yiddish
import os

# Static files (CSS, JavaScript, Images)
# https://docs.djangoproject.com/en/1.8/howto/static-files/
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
STATIC_URL = '/static/'
STATICFILES_DIRS = (
    os.path.join(BASE_DIR, 'static'),
)
# on server with apache2 httpd integration, use something like this instead, with jochreClient being the apache2 application name
# STATIC_URL = '/jochreClient/static/'
# STATIC_ROOT = os.path.join(BASE_DIR, 'static/')

# SECURITY WARNING: keep the secret key used in production secret!
SECRET_KEY = 'yrm_bco(44o_+6j8_!r&top0uw4i+^3wb*=)6awlw&s@50e+&0'

JOCHRE_SEARCH_URL='http://localhost:8080/jochre/search'
JOCHRE_SEARCH_EXT_URL='http://localhost:8080/jochre/search'

EMAIL_BACKEND = 'django.core.mail.backends.console.EmailBackend'

JOCHRE_TITLE="יאָוקער אױף ייִדיש"
JOCHRE_CREDITS="""Texts scanned by the <a href="http://www.yiddishbookcenter.org/" target="_blank">Yiddish Book Center</a><br/>
Texts OCR'd and indexed by Assaf Urieli, <a href="http://www.joli-ciel.com/" target="_blank">Joliciel Informatique</a><br/>
Lexicon by the Yitskhok Niborski and the <a href="http://yiddishweb.com/" target="_blank">Medem Bibliothèque</a>"""

JOCHRE_LEFT_TO_RIGHT=False
JOCHRE_READ_ONLINE=True

JOCHRE_UI_STRINGS = {
"logout" : u"Logout",
"searchButton" : u"זוך",
"titleField" : u"טיטל",
"authorField" : u"מחבר",
"strictField" : u"שטרענג",
"unableToProcessQuery" : u"Unable to process query",
"noResults" : u"No results",
"foundResults" : u"Found %d results. Results %d to %d",
"foundMoreResults" : u"Found more than %d results. Results %d to %d",
"foundResultsRTL" : u"%d רעזולטאַטן. רעזולטאַטן %d ביז %d",
"foundMoreResultsRTL" : u"מער װי %d רעזולטאַטן. רעזולטאַטן %d ביז %d",
"title" : u"Title",
"titleRTL" : u"טיטל",
"author" : u"Author",
"authorRTL" : u"מחבר",
"section" : u"Section",
"pages" : u"Pages",
"to" : u"to",
"sectionRTL" : u"אָפּטײל",
"pagesRTL" : u"זײַטן",
"toRTL" : u"ביז",
"publisher" : u"Publisher",
"date" : u"Date",
"first" : u"First",
"prev" : u"Prev",
"next" : u"Next",
"last" : u"Last",
"volume" : u"volume",
"volumeRTL" : u"באַנד",
}
