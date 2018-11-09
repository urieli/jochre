#coding=utf-8
import json
import requests
import logging
import re

from django.shortcuts import render
from django.shortcuts import redirect
from django.conf import settings
from django.template.defaulttags import register
from django.http import HttpResponse

from jochre.models import KeyboardMapping, Preferences

def search(request):
	logger = logging.getLogger(__name__)

	if not request.user.is_authenticated:
		return redirect('accounts/login/')
	
	username = request.user.username
	ip = getClientIP(request)
	prefs = getPreferences(request)
	
	searchUrl = settings.JOCHRE_SEARCH_URL
	advancedSearch = False
	haveResults = False

	query = ''
	if 'query' in request.GET:
		query = request.GET['query']
	
	author = ''
	hasAuthors = False
	if 'author' in request.GET:
		author = "|".join(request.GET.getlist('author'))
		author = author.replace("||", "|")
		hasAuthors = len(author.replace("|", ""))>0
		
	title = ''
	if 'title' in request.GET:
		title = request.GET['title']
		
	strict = False
	if ('strict') in request.GET:
		strict = True

	authorInclude = True
	if ('authorInclude') in request.GET:
		authorInclude = request.GET['authorInclude']=='true'

	fromYear = ''
	if 'fromYear' in request.GET:
		fromYear = request.GET['fromYear']

	toYear = ''
	if 'toYear' in request.GET:
		toYear = request.GET['toYear']

	sortBy = ''
	if 'sortBy' in request.GET:
		sortBy = request.GET['sortBy']
	
	if hasAuthors or len(title)>0 or len(fromYear)>0 or len(toYear)>0 or strict or sortBy == 'yearAsc' or sortBy == 'yearDesc':
		advancedSearch = True

	displayAdvancedSearch = False
	if advancedSearch:
		displayAdvancedSearch = True
	
	page = 0
	if 'page' in request.GET:
		page = int(request.GET['page'])-1
	
	model = {"query" : query,
			 "authors" : filter(None, author.split("|")),
			 "authorQuery": author,
			 "authorInclude" : authorInclude,
			 "title" : title,
			 "strict" : strict,
			 "fromYear" : fromYear,
			 "toYear" : toYear,
			 "sortBy" : sortBy,
			 "displayAdvancedSearch" : displayAdvancedSearch,
			 "page" : page+1,
			 "JOCHRE_TITLE" : settings.JOCHRE_TITLE,
			 "JOCHRE_CREDITS" : settings.JOCHRE_CREDITS,
			 "JOCHRE_SEARCH_EXT_URL" : settings.JOCHRE_SEARCH_EXT_URL,
			 "RTL" : (not settings.JOCHRE_LEFT_TO_RIGHT),
			 "readOnline" : settings.JOCHRE_READ_ONLINE,
			 "Strings" : settings.JOCHRE_UI_STRINGS,
			 "JOCHRE_CROWD_SOURCE" : settings.JOCHRE_CROWD_SOURCE,
			 "JOCHRE_FONT_LIST" : settings.JOCHRE_FONT_LIST,
			 "JOCHRE_FONT_NAMES" : settings.JOCHRE_FONT_NAMES,
			 "JOCHRE_LANGUAGE_LIST" : settings.JOCHRE_LANGUAGE_LIST,
			 "JOCHRE_LANGUAGE_NAMES" : settings.JOCHRE_LANGUAGE_NAMES,
			 "ip": ip,
			 }
			 
	if len(query)>0:
		MAX_DOCS=1000
		userdata = {"command": "search",
					"query": query,
					"user": username,
					"ip": ip,
					"page": page,
					"resultsPerPage" : prefs['docsPerPage']
					}
		if len(author)>0:
			userdata['authors'] = author
		if len(title)>0:
			userdata['title'] = title
		if strict:
			userdata['expand'] = 'false'
		if authorInclude:
			userdata['authorInclude'] = 'true'
		else:
			userdata['authorInclude'] = 'false'
		if len(fromYear)>0:
			userdata['fromYear'] = fromYear
		if len(toYear)>0:
			userdata['toYear'] = toYear
		if len(sortBy)>0:
			if sortBy=='yearAsc':
				userdata['sortBy'] = 'Year'
				userdata['sortAscending'] = 'true'
			elif sortBy=='yearDesc':
				userdata['sortBy'] = 'Year'
				userdata['sortAscending'] = 'false'

		logger.debug("sending request: {0}, {1}".format(searchUrl, userdata))

		resp = requests.get(searchUrl, userdata)
		
		results = resp.json()
		
		if 'parseException' in results:
			# Fix double-escaped unicode strings in exception!
			message = results["message"]
			match = re.search("\\\\u([0-9a-fA-F]{4})", message) 
			while match:
				message = message[:match.start()] + chr(int(match.group(1), 16)) + message[match.end():]
				match = re.search("\\\\u([0-9a-fA-F]{4})", message) 
			logger.debug("Parse Exception: {0}".format(message))
			model["parseException"] = message
		else:
			docIds = ''
			totalHits = results['totalHits']
			maxResults = results['maxResults']
			docs = results['results']

			for result in docs:
				doc = result['doc']
				if 'volume' in result:
					if 'titleEnglish' in result:
						doc['titleEnglishAndVolume'] = doc['titleEnglish'] + ", " + settings.JOCHRE_UI_STRINGS['volume'] + " " + doc['volume']
					else:
						doc['titleEnglishAndVolume'] = settings.JOCHRE_UI_STRINGS['volume'] + " " + doc['volume']
					if 'volumeRTL' in settings.JOCHRE_UI_STRINGS:
						if 'title' in doc:
							doc['titleAndVolume'] = doc['title'] + ", " + settings.JOCHRE_UI_STRINGS['volumeRTL']  + " " +  doc['volume']
						else:
							doc['titleAndVolume'] = settings.JOCHRE_UI_STRINGS['volumeRTL']  + " " +  doc['volume']
					else:
						if 'title' in doc:
							doc['titleAndVolume'] = doc['title'] + ", " + settings.JOCHRE_UI_STRINGS['volume']  + " " +  doc['volume']
						else:
							doc['titleAndVolume'] = settings.JOCHRE_UI_STRINGS['volume']  + " " +  doc['volume']
				else:
					if 'titleEnglish' in doc:
						doc['titleEnglishAndVolume'] = doc['titleEnglish']
					else:
						doc['titleEnglishAndVolume'] = ''
					if 'title' in doc:
						doc['titleAndVolume'] = doc['title']
					else:
						doc['titleAndVolume'] = ''
				
				doc['pages'] = settings.JOCHRE_UI_STRINGS['pages'].format(doc['startPage'], doc['endPage'])
				if 'pagesRTL' in settings.JOCHRE_UI_STRINGS:
					doc['pagesRTL'] = settings.JOCHRE_UI_STRINGS['pagesRTL'].format(doc['startPage'], doc['endPage'])

				if len(docIds)>0:
					docIds += ','
				docIds += str(doc['docId'])
			
			if len(docs)>0:
				haveResults = True
				userdata = {"command": "snippets",
							"snippetCount": prefs['snippetsPerDoc'],
							"query": query,
							"docIds": docIds,
							"user": username,
							"ip": ip,
							}
				if strict:
					userdata['expand'] = 'false'
				resp = requests.get(searchUrl, userdata)
				model["results"] = docs
				start = page * prefs['docsPerPage'] + 1
				end = start + prefs['docsPerPage'] - 1
				if end > totalHits: end = totalHits
				if end > maxResults: end = maxResults
				model["start"] = start
				model["end"] = end
				model["resultCount"] = totalHits

				pageLinks = []
				lastPage = (totalHits - 1) // prefs['docsPerPage']
				if (totalHits > maxResults):
					lastPage = (maxResults - 1) // prefs['docsPerPage']
				startPage = page - 3
				if startPage < 0: startPage = 0
				endPage = startPage + 6
				if endPage > lastPage: endPage = lastPage
				pageLinks.append({"name":settings.JOCHRE_UI_STRINGS["first"], "val":1, "active":True})
				pageLinks.append({"name":settings.JOCHRE_UI_STRINGS["prev"], "val":page, "active": (page > 0)})
				if startPage>0: pageLinks.append({"name":"..", "val":0, "active": False})
				for i in range(startPage, endPage+1):
					pageLinks.append({"name":"%d" % (i+1), "val": i+1, "active": i != page })
				if endPage<lastPage: pageLinks.append({"name":"..", "val":0, "active": False})
				pageLinks.append({"name":settings.JOCHRE_UI_STRINGS["next"], "val":page+2, "active": (page < lastPage)})
				pageLinks.append({"name":settings.JOCHRE_UI_STRINGS["last"], "val":lastPage+1, "active":True})
				model["pageLinks"] = pageLinks

				if (totalHits <= maxResults):
					foundResults = settings.JOCHRE_UI_STRINGS['foundResults'].format(totalHits, start, end)
				else:
					foundResults = settings.JOCHRE_UI_STRINGS['foundMoreResults'].format(maxResults, start, end)
				model["foundResults"] = foundResults
				
				if 'foundResultsRTL' in settings.JOCHRE_UI_STRINGS:
					if (totalHits <= maxResults):
						foundResultsRTL = settings.JOCHRE_UI_STRINGS['foundResultsRTL'].format(totalHits, start, end)
					else:
						foundResultsRTL = settings.JOCHRE_UI_STRINGS['foundMoreResultsRTL'].format(maxResults, start, end)
					model["foundResultsRTL"] = foundResultsRTL
				
				snippetMap = resp.json()
	
				for result in docs:
					doc = result['doc']
					bookId = doc['name']
					docId = doc['docId']
					snippetObj = snippetMap[str(docId)]
					
					if 'snippetError' in snippetObj:
						snippetError = snippetObj['snippetError']
						doc['snippetError'] = snippetError
						
					else:
						snippets = snippetObj['snippets']
						snippetsToSend = []
						for snippet in snippets:
							snippetJson = json.dumps(snippet)
							snippetText = snippet.pop("text", "")
							
							userdata = {"command": "imageSnippet",
										"snippet": snippetJson,
										"user": username}

							logger.debug("sending request: {0}, {1}".format(settings.JOCHRE_SEARCH_EXT_URL, userdata))

							req = requests.Request(method='GET', url=settings.JOCHRE_SEARCH_EXT_URL, params=userdata)
							preparedReq = req.prepare()
							snippetImageUrl = preparedReq.url
							pageNumber = snippet['pageIndex']
							
							snippetDict = {"snippetText" : snippetText,
										   "pageNumber": pageNumber,
										   "imageUrl": snippetImageUrl }
							
							if settings.JOCHRE_READ_ONLINE:
								snippetReadUrl = settings.JOCHRE_UI_STRINGS['pageURL'].format(bookId, settings.PAGE_URL_TRANSFORM(pageNumber))
								snippetDict["readOnlineUrl"] = snippetReadUrl

							snippetsToSend.append(snippetDict)
							result['snippets'] = snippetsToSend
					
	model["haveResults"] = haveResults

	userdata = {"command": "bookCount",
				"user": username,
				"ip": ip,
				}

	logger.debug("sending request: %s, %s" % (searchUrl, userdata))

	resp = requests.get(searchUrl, userdata).json()
	bookCount = resp["bookCount"]
	model["bookCount"] = bookCount

	return render(request, 'search.html', model)

@register.filter
def get_item(dictionary, key):
	return dictionary[key]

@register.filter
def addstr(arg1, arg2):
	"""concatenate arg1 & arg2"""
	return str(arg1) + str(arg2)

def getClientIP(request):
	x_forwarded_for = request.META.get('HTTP_X_FORWARDED_FOR')
	if x_forwarded_for:
		ip = x_forwarded_for.split(',')[0]
	else:
		ip = request.META.get('REMOTE_ADDR')
	return ip

def getPreferences(request):
	logger = logging.getLogger(__name__)

	if not request.user.is_authenticated:
		raise ValueError('User not logged in.')
	
	username = request.user.username
	preferences = Preferences.objects.filter(user=request.user)

	if len(preferences)==1:
		docsPerPage = preferences[0].docsPerPage
		snippetsPerDoc = preferences[0].snippetsPerDoc
	else:
		docsPerPage = settings.DOCS_PER_PAGE
		snippetsPerDoc = settings.SNIPPETS_PER_DOC

	prefs = {
		"docsPerPage" : docsPerPage,
		"snippetsPerDoc" : snippetsPerDoc
	}
	return prefs

def preferences(request):
	model = getPreferences(request)
	return HttpResponse(json.dumps(model), content_type='application/json')

def updatePreferences(request):
	logger = logging.getLogger(__name__)

	if not request.user.is_authenticated:
		raise ValueError('User not logged in.')

	if request.method == 'POST':
		if 'action' in request.POST and request.POST['action']=='default':
			Preferences.objects.filter(user=request.user).delete()
			logging.debug('deleted preferences')
		else:
			newVals = {}
			if 'docsPerPage' in request.POST:
				newVals['docsPerPage'] = int(request.POST['docsPerPage'])
			if 'snippetsPerDoc' in request.POST:
				newVals['snippetsPerDoc'] = int(request.POST['snippetsPerDoc'])

			obj, created = Preferences.objects.update_or_create(
				user=request.user,
				defaults=newVals,
			)

	model = {"result": "success"}
	return HttpResponse(json.dumps(model), content_type='application/json')

def keyboard(request):
	logger = logging.getLogger(__name__)

	if not request.user.is_authenticated:
		raise ValueError('User not logged in.')
	
	username = request.user.username
	mappings = KeyboardMapping.objects.filter(user=request.user)

	if len(mappings)==1:
		mapping = json.loads(mappings[0].mapping)
		enabled = mappings[0].enabled
	else:
		mapping = settings.KEYBOARD_MAPPINGS
		enabled = settings.KEYBOARD_MAPPINGS_ENABLED

	model = {"mapping" : mapping, "enabled" : enabled}

	return HttpResponse(json.dumps(model), content_type='application/json')

def updateKeyboard(request):
	logger = logging.getLogger(__name__)

	if not request.user.is_authenticated:
		raise ValueError('User not logged in.')

	if request.method == 'POST':
		if 'action' in request.POST and request.POST['action']=='default':
			KeyboardMapping.objects.filter(user=request.user).delete()
			logging.debug('deleted mapping')
		else:
			newVals = {}
			if 'from' in request.POST and 'to' in request.POST:
				fromKeys = request.POST.getlist('from')
				logger.info("fromKeys: %s" % fromKeys)
				toKeys = request.POST.getlist('to')
				logger.info("toKeys: %s" % toKeys)
				newMapping = dict(zip(fromKeys, toKeys))
				newMapping = {k: v for k, v in newMapping.items() if v != "" and k != ""}
				newMappingJson = json.dumps(newMapping)

				logging.debug('newMapping: %s' % newMappingJson)
				newVals['mapping'] = newMappingJson
			if 'enabled' in request.POST:
				newVals['enabled'] = True
			else:
				newVals['enabled'] = False

			obj, created = KeyboardMapping.objects.update_or_create(
				user=request.user,
				defaults=newVals,
			)

	model = {"result": "success"}
	return HttpResponse(json.dumps(model), content_type='application/json')

def contents(request):
	logger = logging.getLogger(__name__)

	if not request.user.is_authenticated:
		return redirect('accounts/login/')

	try:
		searchUrl = settings.JOCHRE_SEARCH_URL
		username = request.user.username
		ip = getClientIP(request)

		contents = ''
		if 'doc' in request.GET:
			docName = request.GET['doc']

			userdata = {"command": "document",
						"docName": docName,
						"user": username,
						"ip": ip,
						}

			logger.debug("sending request: %s, %s" % (searchUrl, userdata))

			resp = requests.get(searchUrl, userdata)
			docs = resp.json()
			doc = docs[0]

			userdata = {"command": "contents",
						"docName": docName,
						"user": username,
						"ip": ip,
						}

			logger.debug("sending request: %s, %s" % (searchUrl, userdata))

			resp = requests.get(searchUrl, userdata)
			contents = resp.text

		model = {"contents": contents,
						"doc" : doc,
						"RTL" : (not settings.JOCHRE_LEFT_TO_RIGHT)
						}
	except:
		model = {"contents": "An error occurred while fetching content, please try to refresh this page.",
			"RTL" : False}

	return render(request, 'contents.html', model)