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
"fromYearField" : u"אױסגאַבע פֿון יאָר",
"toYearField" : u"ביז יאָר",
"sortBy" : u"סאָרטירן לױטן",
"sortByScore" : u"חשבון",
"sortByYearAscending" : u"יאָר ↑",
"sortByYearDescending" : u"יאָר ↓",
"unableToProcessQuery" : u"Unable to process query",
"noResults" : u"No results",
"foundResults" : u"Found {0} results. Results {1} to {2}",
"foundMoreResults" : u"Found more than {0} results. Results {1} to {2}",
"foundResultsRTL" : u"{0} רעזולטאַטן. רעזולטאַטן {1} ביז {2}",
"foundMoreResultsRTL" : u"מער װי {0} רעזולטאַטן. רעזולטאַטן {1} ביז {2}",
"title" : u"Title",
"titleRTL" : u"טיטל",
"author" : u"Author",
"authorRTL" : u"מחבר",
"section" : u"Section",
"sectionRTL" : u"אָפּטײל",
"pages" : u"Pages {0} to {1}",
"pagesRTL" : u"זײַטן {0} ביז {1}",
"toRTL" : u"ביז",
"publisher" : u"Publisher",
"publisherRTL" : u"פֿאַרלאַג",
"date" : u"Year",
"dateRTL" : u"יאָר",
"pageURL" : u"https://archive.org/stream/{0}#page/n{1}/mode/1up",
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

def PAGE_URL_TRANSFORM(pageNumber):
	return pageNumber - 1

KEYBOARD_MAPPINGS_ENABLED = False

KEYBOARD_MAPPINGS = {
	u'a':	 u'אַ',
	u'A':	 u'א',
	u'b':	 u'ב',
	u'B':	 u'בּ',
	u'c':	 u'כ',
	u'C':	 u'ך',
	u'd':	 u'ד',
	u'D':	 u'ד',
	u'e':	 u'ע',
	u'E':	 u'ע',
	u'f':	 u'פֿ',
	u'F':	 u'ף',
	u'g':	 u'ג',
	u'G':	 u'ג',
	u'h':	 u'ה',
	u'H':	 u'ה',
	u'i':	 u'י',
	u'I':	 u'יִ',
	u'j':	 u'ױ',
	u'J':	 u'ױ',
	u'k':	 u'ק',
	u'K':	 u'כּ',
	u'l':	 u'ל',
	u'L':	 u'ל',
	u'm':	 u'מ',
	u'M':	 u'ם',
	u'n':	 u'נ',
	u'N':	 u'ן',
	u'o':	 u'אָ',
	u'O':	 u'וֹ',
	u'p':	 u'פּ',
	u'P':	 u'פ',
	u'q':	 u'ח',
	u'Q':	 u'כֿ',
	u'r':	 u'ר',
	u'R':	 u'ר',
	u's':	 u'ס',
	u'S':	 u'ת',
	u't':	 u'ט',
	u'T':	 u'תּ',
	u'u':	 u'ו',
	u'U':	 u'וּ',
	u'v':	 u'װ',
	u'V':	 u'בֿ',
	u'w':	 u'ש',
	u'W':	 u'שׂ',
	u'x':	 u'צ',
	u'X':	 u'ץ',
	u'y':	 u'ײ',
	u'Y':	 u'ײַ',
	u'z':	 u'ז',
	u'Z':	 u'ז',
	u'“':	 u'"',
	u'”':	 u'"',
	u'’':	 u"'",
	u'„':	 u'"',
}