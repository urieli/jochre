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

JOCHRE_TITLE="Jochre en Occitan"
JOCHRE_CREDITS="""Tèxtes ocerizats e lexic per la còla <a href="http://myriam.bras.free.fr/bateloc/index.html" target="_blank">BaTelÒc</a> de <a href="http://w3.erss.univ-tlse2.fr/" target="_blank">CLLE-ERSS</a> dins l'encastre del projècte <a href="http://restaure.unistra.fr" target="_blank">RESTAURE</a><br/>
Tèxtes numerizats pel <a href="http://locirdoc.fr/" target="_blank">CIRDÒC</a><br/>
Tèxtes indexats per <a href="http://www.joli-ciel.com/" target="_blank">Joliciel Informatique</a>"""

JOCHRE_LEFT_TO_RIGHT=True
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

# UI strings, partially corrected
JOCHRE_UI_STRINGS = {
"logout" : u"se desconnectar",
"searchButton" : u"cercar",
"titleField" : u"Títol",
"authorField" : u"Autor",
"strictField" : u"Forma exacta",
"clear" : u"Clear",
"fromYearField" : u"Annada despuòi",
"toYearField" : u"dusca",
"sortBy" : u"Sort by",
"sortByScore" : u"score",
"sortByYearAscending" : u"year ↑",
"sortByYearDescending" : u"year ↓",
"unableToProcessQuery" : u"Unable to process query",
"noResults" : u"No results",
"foundResults" : u"%d resultats. Resultats %d a %d",
"foundMoreResults" : u"Mai que %d resultats. Resultats %d a %d",
"title" : u"Títol",
"author" : u"Autor",
"section" : u"Partidas",
"pages" : u"Paginas",
"to" : u"a",
"publisher" : u"Editor",
"date" : u"Data",
"first" : u"Primièra",
"prev" : u"Precedenta",
"next" : u"Seguenta",
"last" : u"Darrièra",
"volume" : u"volum",
"fixWordTitle": u"Fix a word",
"fixWordInstructions" : u"""PVos prepausam de corregir lo tèxte reconegut pel logicial de reconeissença de caractèrs (OCR).
Vos demandam de vos sarrar al pus prèp de çò que vesètz sus l'imatge.""",
"fixWordWord" : u"Word",
"fixWordFont" : u"Font",
"fixWordFontExample" : u"Font example",
"fixWordLanguage" : u"Language",
"prefsTitle" : u"Preferences",
"prefsDocsPerPage" : u"Results per page",
"prefsSnippetsPerDoc" : u"Snippets per result",
}

SHOW_SECTION = False

def PAGE_URL_TRANSFORM(pageNumber):
  return pageNumber

KEYBOARD_MAPPINGS_ENABLED = False

KEYBOARD_MAPPINGS = {}

DOCS_PER_PAGE = 10
SNIPPETS_PER_DOC = 20