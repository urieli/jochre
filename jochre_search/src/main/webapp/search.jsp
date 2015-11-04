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
%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<%@ include file="Styles.css" %>
<script src="https://code.jquery.com/jquery-1.11.3.min.js"></script>
</head>
<body>
<form method="post" accept-charset="UTF-8">
<table>
<tr><td><img src="images/jochreLogo.png"/></td><td><input type="text" name="query" style="width:200px;" value="<%= queryString %>" />&nbsp;<input type="submit" value="Search" /></td></tr>
</table>
<%
if (queryString.length()>0) {
	URL myPage = new URL(request.getRequestURL().toString());
	String searchURL = "search?command=search&query=" + URLEncoder.encode(queryString, "UTF-8");
	URL url = new URL(myPage, searchURL);
	String json = SearchWebClientUtils.getJson(url);
	SearchResults results = new SearchResults(json);
	Set<Integer> docIds = new HashSet<Integer>();
	if (results.getScoreDocs().size()==0) {
		%><p>No results</p><%
	} else {
		for (SearchDocument result : results.getScoreDocs()) {
			docIds.add(result.getDocId());
		}

		String snippetUrl = "search?command=snippets&query=" + URLEncoder.encode(queryString, "UTF-8")
			+ "&docIds=";
		boolean first = true;
		for (int docId : docIds) {
			if (!first) snippetUrl += ",";
			snippetUrl+=docId;
			first=false;
		}
		url = new URL(myPage, snippetUrl);
		json = SearchWebClientUtils.getJson(url);
		SnippetResults snippetResults = new SnippetResults(json);
		%>
		<table width="720px">
		<%
		int i=0;
		for (SearchDocument result : results.getScoreDocs()) {
			String bookId = result.getUrl().substring(result.getUrl().lastIndexOf('/')+1);
			int startPageUrl = result.getStartPage() / 2 * 2;
			String readOnlineURL = "https://archive.org/stream/" + bookId + "#page/n" + startPageUrl + "/mode/2up";
			%>
			<tr><td colspan="2" height="5px" bgcolor="black"></td></tr>
			<tr><td align="left">
			<font size="+1"><b>Title:</b> <a href="<%= result.getUrl() %>" target="_blank"><%= result.getTitle() %></a></font><br/>
			<b>Author:</b> <%= result.getAuthor() %><br/>
			<b>Section:</b> Pages <a href="<%= readOnlineURL %>" target="_blank"><%= result.getStartPage() %> to <%= result.getEndPage() %></a></td><td align="right">score <%= result.getScore() %></td></tr>
			<tr><td colspan="2">
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
				<p>
				<a href="<%= readOnlineURL %>" target="_blank">Page <%= pageNumber %></a> : <span id="snippet<%=i %>_<%=j %>" class="snippet" dir="rtl"><%= snippetText %></span>
				</p>
				<div id="image<%=i %>_<%=j %>" style="display: none;"></div>
				<script>
				$loaded<%=i %>_<%=j %>=false;
				$("#snippet<%=i %>_<%=j %>").on("click",function(){
					//alert("<%= imageUrlAddress %>");
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
			<tr><td colspan="2" height="10px"></td></tr>
			<%
			i++;
		}
		%>
		</table>
		<%
	}
}
%>
</form>
</body>
</html>