<%@page import="java.util.ArrayList"%>
<%@page import="com.joliciel.jochre.search.highlight.Snippet"%>
<%@page import="java.util.List"%>
<%@page import="com.joliciel.jochre.search.webClient.SnippetResults"%>
<%@page import="com.joliciel.jochre.search.webClient.SearchWebClientUtils"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.HashSet"%>
<%@page import="com.joliciel.jochre.search.webClient.SearchDocument"%>
<%@page import="com.joliciel.talismane.utils.WeightedOutcome"%>
<%@page import="com.joliciel.jochre.search.webClient.SearchResults"%>
<%@page import="org.apache.commons.io.IOUtils"%>
<%@page import="java.io.StringWriter"%>
<%@page import="java.io.StringReader"%>
<%@page import="java.io.InputStream"%>
<%@page import="java.net.URLConnection"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.net.URL"%>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>

<%
request.setCharacterEncoding("UTF-8");
response.setCharacterEncoding("UTF-8");
String queryString = request.getParameter("query");
if (queryString==null) queryString = "";
String queryStringInput = queryString.replace("\"", "&quot;");

String authorQueryString = request.getParameter("author");
if (authorQueryString==null) authorQueryString = "";
String authorQueryStringInput = authorQueryString.replace("\"", "&quot;");
String titleQueryString = request.getParameter("title");
if (titleQueryString==null) titleQueryString = "";
String titleQueryStringInput = titleQueryString.replace("\"", "&quot;");

boolean strict = false;
if (request.getParameter("strict")!=null) {
	strict = request.getParameter("strict").equals("true");
}
boolean advancedSearch = authorQueryString.length()>0 || titleQueryString.length()>0 || strict;

int pagination = 0;
if (request.getParameter("page")!=null)
	pagination = Integer.parseInt(request.getParameter("page"));
%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<%@ include file="Styles.css" %>
<script src="jquery-1.11.3.min.js"></script>
</head>
<body>
<form id="frmQuery" method="get" accept-charset="UTF-8" action="search.jsp">
<input type="hidden" name="page" id="hdnPage" value="0" />
<table>
<tr>
<td><img src="images/jochreLogo.png" width="150px" /></td>
<td style="vertical-align: bottom;" align="right" width="400px"><input type="text" name="query" style="width:300px;" value="<%= queryStringInput %>" />&nbsp;<input type="submit" value="Search" /></td>
<td style="vertical-align: bottom;" width="20px"><span id="toggleAdvancedSearch" ><img src="images/plusInCircle.png" border="0" width="20px" /></span></td>
</tr>
<tr id="advancedSearch" style="display: <%= advancedSearch ? "visible" : "none" %>;">
<td colspan="2" align="right">
<table>
<tr>
<td>Strict? <input type="checkbox" name="strict" value="true" <% if (strict) { %>checked="true" <% } %>/></td>
<td class="RTLAuthor" width="200px"><b>טיטל:</b> <input type="text" name="title" style="width:150px;" value="<%= titleQueryStringInput %>" /></td>
<td class="RTLAuthor" width="200px"><b>מחבר:</b> <input type="text" name="author" style="width:150px;" value="<%= authorQueryStringInput %>" /></td>
</tr>
</table>
</td>
<td>&nbsp;</td>
</tr>
</table>
</div>
<script>
$("#toggleAdvancedSearch").on("click",function(){
	$("#advancedSearch").toggle();
   });
</script>
<%
if (queryString.length()>0) {
	int MAX_DOCS=1000;
	int RESULTS_PER_PAGE=10;
	URL myPage = new URL(request.getRequestURL().toString());
	String searchURL = "search?command=search&maxDocs=" + MAX_DOCS + "&query=" + URLEncoder.encode(queryString, "UTF-8");
	if (authorQueryString.length()>0)
		searchURL += "&author=" + URLEncoder.encode(authorQueryString, "UTF-8");
	if (titleQueryString.length()>0)
		searchURL += "&title=" + URLEncoder.encode(titleQueryString, "UTF-8");
	if (strict)
		searchURL += "&expand=false";
	
	URL url = new URL(myPage, searchURL);
	String json = SearchWebClientUtils.getJson(url);
	SearchResults results = new SearchResults(json);
	Set<Integer> docIds = new HashSet<Integer>();
	if (results.getScoreDocs().size()==0) {
		%><p>No results</p><%
	} else {
		int max = results.getScoreDocs().size() / RESULTS_PER_PAGE;
		if (pagination>max) pagination = max;
		int start = pagination*RESULTS_PER_PAGE;
		while (start>results.getScoreDocs().size()-1) {
			start -= RESULTS_PER_PAGE;
			pagination-=1;
		}
		int end = start+RESULTS_PER_PAGE;
		if (end>results.getScoreDocs().size()) end=results.getScoreDocs().size();
		
		int startRange = pagination - 3;
		int endRange = pagination + 3;
		while (startRange<0) {
			startRange += 1;
			endRange += 1;
		}
		while (endRange>max) {
			startRange -= 1;
			endRange -= 1;
		}
		if (startRange<0) startRange=0;
		
		List<SearchDocument> paginatedResults = new ArrayList<SearchDocument>(RESULTS_PER_PAGE);
		for (int i=start; i<end; i++) {
			SearchDocument result = results.getScoreDocs().get(i);
			paginatedResults.add(result);
			docIds.add(result.getDocId());
		}

		String snippetUrl = "search?command=snippets&snippetCount=8&snippetSize=160&query=" + URLEncoder.encode(queryString, "UTF-8")
			+ "&docIds=";
		boolean first = true;
		for (int docId : docIds) {
			if (!first) snippetUrl += ",";
			snippetUrl+=docId;
			first=false;
		}
		if (strict)
			snippetUrl += "&expand=false";
		url = new URL(myPage, snippetUrl);
		json = SearchWebClientUtils.getJson(url);
		SnippetResults snippetResults = new SnippetResults(json);
		
		String resultText = "" + results.getScoreDocs().size();
		if (results.getScoreDocs().size()==MAX_DOCS) resultText = "оver " + resultText;
		%>
		<p><b>Found <%= resultText %> results. Results <%= start+1 %> to <%= end %>:</b></p>
		
		<table width="740px">
		<%
		int i=0;
		for (SearchDocument result : paginatedResults) {
			String bookId = result.getUrl().substring(result.getUrl().lastIndexOf('/')+1);
			int startPageUrl = result.getStartPage() / 2 * 2;
			String readOnlineURL = "https://archive.org/stream/" + bookId + "#page/n" + startPageUrl + "/mode/2up";
			String title = result.getTitle();
			if (result.getVolume()!=null)
				title += ", volume " + result.getVolume();
			
			String titleLang = result.getTitleLang();
			if (titleLang!=null && result.getVolume()!=null)
				titleLang += ", באַנד " + result.getVolume();
			if (titleLang==null)
				titleLang = "";
			
			String author = result.getAuthor();
			String authorLang = result.getAuthorLang();
			if (authorLang==null)
				authorLang = "";
			%>
			<tr><td height="5px" bgcolor="black"></td></tr>
			<tr><td align="left">
			<table width="100%">
			<tr>
			<td class="Title" width="50%"><b>Title:</b> <a href="<%= result.getUrl() %>" target="_blank"><%= title %></a></td>
			<td class="RTLTitle"><b>טיטל:</b> <a href="<%= result.getUrl() %>" target="_blank"><%= titleLang %></a></td>
			</tr>
			<tr>
			<td class="Author" width="50%"><b>Author:</b> <%= author %></td>
			<td class="RTLAuthor"><b>מחבר:</b> <%= authorLang %></td>
			</tr>
			<tr>
			<td class="Author"  width="50%"><b>Section:</b> Pages <a href="<%= readOnlineURL %>" target="_blank"><%= result.getStartPage() %> to <%= result.getEndPage() %></a></td>
			<td class="RTLAuthor"><b>אָפּטײל:</b> זײַטן <a href="<%= readOnlineURL %>" target="_blank"><%= result.getStartPage() %> ביז <%= result.getEndPage() %></a></td>
			</tr>
			<%
			if (result.getPublisher()!=null) {
				%>
				<tr><td class="Author" colspan="2"><b>Publisher:</b> <%= result.getPublisher() %></td></tr>
				<%
			}
			%>
			<%
			if (result.getDate()!=null) {
				%>
				<tr><td class="Author" colspan="2"><b>Date:</b> <%= result.getDate() %></td></tr>
				<%
			}
			%>
			</table>
			</td></tr>
			<tr><td>
			<%
			List<Snippet> snippets = snippetResults.getSnippetMap().get(result.getDocId());
			int j=0;
			for (Snippet snippet : snippets) {
				String textUrl = "search?command=textSnippet&snippet=" + URLEncoder.encode(snippet.toJson(), "UTF-8");
				url = new URL(myPage, textUrl);
				String snippetText = SearchWebClientUtils.getJson(url);
				String imageUrlAddress = "search?command=imageSnippet&snippet=" + URLEncoder.encode(snippet.toJson(), "UTF-8");
				int pageNumber = snippet.getPageIndex();
				int urlPageNumber = pageNumber / 2 * 2;
				readOnlineURL = "https://archive.org/stream/" + bookId + "#page/n" + urlPageNumber + "/mode/2up";
				%>
				<table width="100%" border="0">
				<tr>
				<td><div id="snippet<%=i %>_<%=j %>" class="snippet"><%= snippetText %></div></td>
				<td align="center" valign="top" width="30px"><span id="img<%=i %>_<%=j %>" ><img src="images/image.png" border="0" /></span></td>
				<td align="center" valign="top" width="30px"><a href="<%= readOnlineURL %>" target="_blank"><img src="images/text.png" border="0" /></a></td>
				<td align="right" valign="top" width="30px"><a href="<%= readOnlineURL %>" target="_blank"><%= pageNumber %></a></td>
				</tr>
				</table>
				<div id="image<%=i %>_<%=j %>" style="display: none;"></div>
				<script>
				$loaded<%=i %>_<%=j %>=false;
				$("#img<%=i %>_<%=j %>").on("click",function(){
					if (!$loaded<%=i %>_<%=j %>) {
						$("#image<%=i %>_<%=j %>").html('<img src="<%= imageUrlAddress %>" width="720px" border="1" />');
						$loaded<%=i %>_<%=j %> = true;
					}
					$("#image<%=i %>_<%=j %>").toggle();
			    });
				$("#image<%=i %>_<%=j %>").on("click",function(){
					window.open('<%= readOnlineURL %>', '_blank');
				});
				</script>
				<%
				j++;
			}
			%></td></tr>
			<tr><td height="10px"></td></tr>
			<%
			i++;
		}
		%>
		</table>
		
		<%
		// pagination links
		if (max>0) {
			%>
			<div style="width: 740px; text-align: left; ">
			<table style="margin: auto;"><tr>
			<td>
			<%
			if (pagination>0) {
				%><a href="#" id="pagePrev">Prev</a><%
			} else {
				%><span style="font-weight: bold; color: gray;">Prev</span><%
			}
			%>
			</td>
			<%
			for (i=startRange; i<=endRange; i++) {
				%><td style="width:40px; text-align: center;"><%
				if (i==pagination) {
					%><b><%=i+1 %></b><%
				} else {
					%><a href="#" id="page<%= i%>"><%= i+1 %></a><%
				}
				%></td><%
			}
			%>
			<td>
			<%
			if (pagination<max) {
				%><a href="#" id="pageNext">Next</a><%
			} else {
				%><span style="font-weight: bold; color: gray;">Next</span><%
			}
			%>
			</td>
			</tr></table></div>
			<%
			for (i=startRange; i<=endRange; i++) {
				%>
				<script>
				$("#page<%=i %>").on("click",function(){
					$("#hdnPage").val("<%=i%>");
					$("#frmQuery").submit();
					return false;
			    });
				</script>
				<%
			}
			%>
			<script>
			$("#pagePrev").on("click",function(){
				$("#hdnPage").val("<%=pagination-1%>");
				$("#frmQuery").submit();
				return false;
		    });
			$("#pageNext").on("click",function(){
				$("#hdnPage").val("<%=pagination+1%>");
				$("#frmQuery").submit();
				return false;
		    });
			</script>
			<%
		} // pagination links
	}
}
%>
</form>
</body>
</html>