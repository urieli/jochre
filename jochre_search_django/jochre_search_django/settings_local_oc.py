#!/usr/bin/env python
# -*- coding: utf-8 -*-

# Sample settings in Occitan

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

INSTALLED_APPS = [
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.messages',
    'django.contrib.staticfiles',
    'django.contrib.sites',
    'allauth',
    'allauth.account',
    'allauth.socialaccount',
    'jochre',
]

# ... include the providers you want to enable:
#INSTALLED_APPS.append('allauth.socialaccount.providers.facebook')
#INSTALLED_APPS.append('allauth.socialaccount.providers.google')

ALLOWED_HOSTS = ['localhost', '127.0.0.1']

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

JOCHRE_TITLE= {
"oc": u"Jochre en Occitan",
"en": u"Jochre in Occitan"
}
JOCHRE_CREDITS= {
"en": """Texts OCR'd and lexicon by the <a href="http://myriam.bras.free.fr/bateloc/index.html" target="_blank">BaTelÒc</a> project at <a href="http://w3.erss.univ-tlse2.fr/" target="_blank">CLLE-ERSS</a> dins l'encastre del projècte <a href="http://restaure.unistra.fr" target="_blank">RESTAURE</a><br/>
Texts digitized by <a href="http://locirdoc.fr/" target="_blank">CIRDÒC</a><br/>
Texts indexed by <a href="http://www.joli-ciel.com/" target="_blank">Joliciel Informatique</a>""",
"oc": """Tèxtes ocerizats e lexic per la còla <a href="http://myriam.bras.free.fr/bateloc/index.html" target="_blank">BaTelÒc</a> de <a href="http://w3.erss.univ-tlse2.fr/" target="_blank">CLLE-ERSS</a> dins l'encastre del projècte <a href="http://restaure.unistra.fr" target="_blank">RESTAURE</a><br/>
Tèxtes numerizats pel <a href="http://locirdoc.fr/" target="_blank">CIRDÒC</a><br/>
Tèxtes indexats per <a href="http://www.joli-ciel.com/" target="_blank">Joliciel Informatique</a>""",
}

DEFAULT_LANG = "oc"

# Is each field right-to-left or left-to-right
FIELDS_LTR = {
  'contents': True,
  'title': True,
  'titleTranscribed': True,
  'volume': True,
  'author': True,
  'authorTranscribed': True,
  'publisher': True,
  'date': True,
  'referenceNumber': True,
}

JOCHRE_READ_ONLINE=False

JOCHRE_CROWD_SOURCE=True

JOCHRE_FONT_LIST = ("serif", "serifItalics", "sansSerif", "sansSerifItalics")
JOCHRE_FONT_NAMES = {
"serif" : u"Serif",
"serifItalics" : u"Serif - italique",
"sansSerif" : u"Sans Serif",
"sansSerifItalics" : u"Sans Serif - italique"
}

JOCHRE_LANGUAGE_LIST = ("oc", "fr", "other")
JOCHRE_LANGUAGE_NAMES = {
"oc" : "occitan",
"fr" : "français",
"other" : "autre"
}

# UI strings
JOCHRE_UI_STRINGS = {
}

SHOW_SECTION = False

def PAGE_URL_TRANSFORM(pageNumber):
  return pageNumber

KEYBOARD_MAPPINGS_ENABLED = False

KEYBOARD_MAPPINGS = {}

DOCS_PER_PAGE = 10
SNIPPETS_PER_DOC = 20