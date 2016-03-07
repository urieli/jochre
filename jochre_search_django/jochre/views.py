#coding=utf-8
import json
import requests
import paginate
import logging

from django.shortcuts import render
from django.shortcuts import redirect
from django.conf import settings
from django.template.defaulttags import register

def search(request):
    if not request.user.is_authenticated():
        return redirect('accounts/login/')
    
    username = request.user.username
    
    searchUrl = settings.JOCHRE_SEARCH_URL
    advancedSearch = False
    haveResults = False

    query = ''
    if 'query' in request.GET:
        query = request.GET['query']
    
    author = ''
    if 'author' in request.GET:
        author = request.GET['author']
        
    title = ''
    if 'title' in request.GET:
        title = request.GET['title']
        
    strict = False
    if ('strict') in request.GET:
        strict = True
    
    if len(author)>0 or len(title)>0 or strict:
        advancedSearch = True

    displayAdvancedSearch = 'none'
    if advancedSearch:
        displayAdvancedSearch = 'visible'
    
    pageNumber = 0
    if 'page' in request.GET:
        pageNumber = int(request.GET['page'])
    
    model = {"query" : query,
             "author" : author,
             "title" : title,
             "strict" : strict,
             "displayAdvancedSearch" : displayAdvancedSearch,
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
             "defaultFontImage" : "images/" + settings.JOCHRE_FONT_LIST[0] + ".png"
             }
             
    if len(query)>0:
        MAX_DOCS=1000
        RESULTS_PER_PAGE=10
        userdata = {"command": "search",
                    "maxDocs": MAX_DOCS,
                    "query": query,
                    "user": username}
        if len(author)>0:
            userdata['author'] = author
        if len(title)>0:
            userdata['title'] = title
        if strict:
            userdata['expand'] = 'false'
        
        resp = requests.get(searchUrl, userdata)
        
        results = resp.json()
        
        if len(results)==1 and 'parseException' in results[0]:
            model["parseException"] = results[0]["message"]
        else:
            page = paginate.Page(results, page=pageNumber, items_per_page=RESULTS_PER_PAGE)
            
            docIds = ''
            
            for result in page.items:
                if 'volume' in result and 'title' in result:
                    result['titleAndVolume'] = result['title'] + ", " + settings.JOCHRE_UI_STRINGS['volume'] + " " + result['volume']
                    if 'titleLang' in result and 'volumeRTL' in settings.JOCHRE_UI_STRINGS:
                        result['titleLangAndVolume'] = result['titleLang'] + ", " + settings.JOCHRE_UI_STRINGS['volumeRTL']  + " " +  result['volume']
                    else:
                        result['titleLangAndVolume'] = ""
                else:
                    if 'title' in result:
                        result['titleAndVolume'] = result['title']
                        if 'titleLang' in result:
                            result['titleLangAndVolume'] = result['titleLang']
                        else:
                            result['titleLangAndVolume'] = ""
                if len(docIds)>0:
                    docIds += ','
                docIds += str(result['docId'])
            
            if len(page.items)>0:
                haveResults = True
                userdata = {"command": "snippets",
                            "snippetCount": 8,
                            "snippetSize": 160,
                            "query": query,
                            "docIds": docIds,
                            "user": username}
                if strict:
                    userdata['expand'] = 'false'
                resp = requests.get(searchUrl, userdata)
                model["results"] = page.items
                model["start"] = page.first_item
                model["end"] = page.last_item
                model["resultCount"] = len(results)
                model["pageLinks"] = page.link_map(url="http://localhost:8000?page=$page")
                
                foundResults = settings.JOCHRE_UI_STRINGS['foundResults'] % (len(results), page.first_item, page.last_item)
                if (len(results)>=1000):
                    foundResults = settings.JOCHRE_UI_STRINGS['foundMoreResults'] % (len(results), page.first_item, page.last_item)
                model["foundResults"] = foundResults
                
                if 'foundResultsRTL' in settings.JOCHRE_UI_STRINGS:
                    foundResultsRTL = settings.JOCHRE_UI_STRINGS['foundResultsRTL'] % (len(results), page.first_item, page.last_item)
                    if (len(results)>=1000):
                        foundResultsRTL = settings.JOCHRE_UI_STRINGS['foundMoreResultsRTL'] % (len(results), page.first_item, page.last_item)
                    model["foundResultsRTL"] = foundResultsRTL

                logging.debug(model["pageLinks"])
                
                snippetMap = resp.json()
    
                for result in page.items:
                    bookId = result['name']
                    docId = result['docId']
                    snippetObj = snippetMap[str(docId)]
                    snippets = snippetObj['snippets']
                    snippetsToSend = []
                    for snippet in snippets:
                        snippetJson = json.dumps(snippet)
                        snippetText = snippet.pop("text", "")
                        
                        userdata = {"command": "imageSnippet",
                                    "snippet": snippetJson,
                                    "user": username}
                        req = requests.Request(method='GET', url=settings.JOCHRE_SEARCH_EXT_URL, params=userdata)
                        preparedReq = req.prepare()
                        snippetImageUrl = preparedReq.url
                        pageNumber = snippet['pageIndex']
                        
                        snippetDict = {"snippetText" : snippetText,
                                       "pageNumber": pageNumber,
                                       "imageUrl": snippetImageUrl }
                        
                        if settings.JOCHRE_READ_ONLINE:
                            urlPageNumber = pageNumber / 2 * 2;
                            snippetReadUrl = u"https://archive.org/stream/" + bookId + u"#page/n" + str(urlPageNumber) + u"/mode/2up";
                            snippetDict["readOnlineUrl"] = snippetReadUrl

                        snippetsToSend.append(snippetDict)
                        result['snippets'] = snippetsToSend
                    
    model["haveResults"] = haveResults
    return render(request, 'search.html', model)

@register.filter
def get_item(dictionary, key):
    return dictionary[key]

@register.filter
def addstr(arg1, arg2):
    """concatenate arg1 & arg2"""
    return str(arg1) + str(arg2)