#!/usr/bin/env python
# -*- coding: utf-8 -*-

# Sample settings in Occitan

import os

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# Static files (CSS, JavaScript, Images)
# https://docs.djangoproject.com/en/1.8/howto/static-files/
# on server with apache2 httpd integration, use something like this instead, with jochreClientOc being the apache2 application name
# STATIC_URL = '/jochreClientOc/static/'
STATIC_URL = '/static/'
STATIC_ROOT = os.path.join(BASE_DIR, 'static/')



# SECURITY WARNING: keep the secret key used in production secret!
SECRET_KEY = 'yrm_bco(44o_+6j8_!r&top0uw4i+^3wb*=)6awlw&s@50e+&0'

SITE_ID = 1

JOCHRE_SEARCH_URL='http://localhost:8080/jochreSearchOc/search'
JOCHRE_SEARCH_EXT_URL='http://localhost:8080/jochreSearchOc/search'

LOGIN_REDIRECT_URL = '/'

EMAIL_BACKEND = 'django.core.mail.backends.console.EmailBackend'

JOCHRE_TITLE="Jochre en Occitan"
JOCHRE_CREDITS="""Textes océrisés et lexique par <a href="http://w3.erss.univ-tlse2.fr/" target="_blank">CLLE-ERSS</a><br/>
Textes numérisés par <a href="http://locirdoc.fr/" target="_blank">Cirdoc</a><br/>
Textes indexés by Assaf Urieli, <a href="http://www.joli-ciel.com/" target="_blank">Joliciel Informatique</a>"""

JOCHRE_LEFT_TO_RIGHT=True
JOCHRE_READ_ONLINE=False

# UI strings is complete guesswork by Assaf
JOCHRE_UI_STRINGS = {
"logout" : u"Logout",
"searchButton" : u"cercar",
"titleField" : u"Títol",
"authorField" : u"Fasedor",
"strictField" : u"estrech",
"unableToProcessQuery" : u"Unable to process query",
"noResults" : u"No results",
"foundResults" : u"%d resultatas. Resultatas %d a %d",
"foundMoreResults" : u"Mai que %d resultatas. Resultatas %d a %d",
"title" : u"Títol",
"author" : u"Fasedor",
"section" : u"Encors",
"pages" : u"Paginas",
"to" : u"a",
"publisher" : u"Editor",
"date" : u"Data",
"first" : u"Primièr",
"prev" : u"Reiral",
"next" : u"Seguidor",
"last" : u"Darrièr",
"volume" : u"volum",
}
