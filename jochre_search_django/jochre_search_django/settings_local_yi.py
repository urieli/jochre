#!/usr/bin/env python
# -*- coding: utf-8 -*-

# Sample settings in Yiddish
import os
import logging

logging.basicConfig(level=logging.DEBUG,
                    format='%(asctime)s %(processName)-10s %(name)s %(levelname)-8s %(message)s',
                    datefmt='%y-%m-%d %H:%M',
                    filename='/var/log/jochreSearchDjango/django.log',
                    filemode='a')

# Quick-start development settings - unsuitable for production
# See https://docs.djangoproject.com/en/2.1/howto/deployment/checklist/

# SECURITY WARNING: keep the secret key used in production secret!
SECRET_KEY = '_ktmo6%l23m8q4p(dpzx8r(x#rauoqt2t3**lqx^j3z%@^r!f$'

# SECURITY WARNING: don't run with debug turned on in production!
DEBUG = True

ALLOWED_HOSTS = ['localhost', '127.0.0.1']

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# Static files (CSS, JavaScript, Images)
# https://docs.djangoproject.com/en/1.8/howto/static-files/
# on server with apache2 httpd integration, use something like this instead, with jochreClientOc being the apache2 application name
# STATIC_URL = '/jochreClient/static/'
STATIC_URL = '/static/'
STATIC_ROOT = os.path.join(BASE_DIR, 'static/')

# SECURITY WARNING: keep the secret key used in production secret!
SECRET_KEY = 'yrm_bco(44o_+6j8_!r&top0uw4i+^3wb*=)6awlw&s@50e+&0'

SITE_ID = 1

JOCHRE_SEARCH_URL='http://localhost:8080/jochre/search'
JOCHRE_SEARCH_EXT_URL='http://localhost:8080/jochre/search'

LOGIN_REDIRECT_URL = '/'

EMAIL_BACKEND = 'django.core.mail.backends.console.EmailBackend'

JOCHRE_TITLE="יאָוקער אױף ייִדיש"
JOCHRE_CREDITS="""Texts scanned by the <a href="http://www.yiddishbookcenter.org/" target="_blank">Yiddish Book Center</a><br/>
Texts OCR'd and indexed by Assaf Urieli, <a href="http://www.joli-ciel.com/" target="_blank">Joliciel Informatique</a><br/>
Lexicon by Yitskhok Niborski and the <a href="http://yiddishweb.com/" target="_blank">Medem Bibliothèque</a>"""

JOCHRE_LEFT_TO_RIGHT=False
JOCHRE_READ_ONLINE=True

JOCHRE_CROWD_SOURCE=True

JOCHRE_FONT_LIST = ("serif", "serifItalics", "sansSerif", "sansSerifItalics")
JOCHRE_FONT_NAMES = {
"serif" : u"Serif",
"serifItalics" : u"Serif - Italics",
"sansSerif" : u"Sans Serif",
"sansSerifItalics" : u"Sans Serif - Italics"
}

JOCHRE_LANGUAGE_LIST = ("yi", "other")
JOCHRE_LANGUAGE_NAMES = {
"yi" : "Yiddish",
"other" : "Other"
}

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
"fixWordTitle": u"Fix a word",
"fixWordInstructions" : u"""Please enter the word exactly as it appears above, including niqqud, apsotrophes, quotes, dashes, and other punctuation.
If the word as badly segmented (i.e. if only part of the word appears), do not correct it.""",
"fixWordWord" : u"Word",
"fixWordFont" : u"Font",
"fixWordFontExample" : u"Font example",
"fixWordLanguage" : u"Language",
}
