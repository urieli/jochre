<%@page import="com.fasterxml.jackson.core.type.TypeReference"%>
<%@page import="com.fasterxml.jackson.databind.ObjectMapper"%>
<%@page import="java.io.StringWriter"%>
<%@page import="java.net.URL"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Set"%>
<%@page import="org.apache.commons.io.IOUtils"%>
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
<td>Strict? <input type="checkbox" name="strict" value="true" <% if (strict) { %>checked="checked" <% } %>/></td>
<td class="RTLAuthor" width="200px"><b>טיטל:</b> <input type="text" name="title" style="width:150px;" value="<%= titleQueryStringInput %>" /></td>
<td class="RTLAuthor" width="200px"><b>מחבר:</b> <input type="text" name="author" style="width:150px;" value="<%= authorQueryStringInput %>" /></td>
</tr>
</table>
</td>
<td>&nbsp;</td>
</tr>
</table>
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
	StringWriter writer = new StringWriter();
	IOUtils.copy(url.openConnection().getInputStream(), writer, "UTF-8");
	String json = writer.toString();
	
	ObjectMapper mapper = new ObjectMapper();
	List<Map<String, Object>> results = mapper.readValue(json,
            new TypeReference<ArrayList<Map<String, Object>>>() {
            });

	if (results.size()==0) {
		%><p>No results</p><%
	} else if (results.get(0).get("parseException")!=null) {
		%>
		<p>Unable to process query:</p>
		<p><%= results.get(0).get("message") %>
		<%
	} else {
		Set<Integer> docIds = new HashSet<Integer>();
		int max = results.size() / RESULTS_PER_PAGE;
		if (pagination>max) pagination = max;
		int start = pagination*RESULTS_PER_PAGE;
		while (start>results.size()-1) {
			start -= RESULTS_PER_PAGE;
			pagination-=1;
		}
		int end = start+RESULTS_PER_PAGE;
		if (end>results.size()) end=results.size();
		
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
		
		List<Map<String,Object>> paginatedResults = new ArrayList<Map<String,Object>>(RESULTS_PER_PAGE);
		for (int i=start; i<end; i++) {
			Map<String,Object> result = results.get(i);
			paginatedResults.add(result);
			docIds.add((Integer)result.get("docId"));
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
		writer = new StringWriter();
		IOUtils.copy(url.openConnection().getInputStream(), writer, "UTF-8");
		json = writer.toString();

		Map<String, Object> snippetResults = mapper.readValue(json,
	            new TypeReference<HashMap<String, Object>>() {
	            });
		
		String resultText = "" + results.size();
		if (results.size()==MAX_DOCS) resultText = "оver " + resultText;
		%>
		<p><b>Found <%= resultText %> results. Results <%= start+1 %> to <%= end %>:</b></p>
		
		<table style="width:740px;">
		<%
		int i=0;
		for (Map<String,Object> result : paginatedResults) {
			String bookId = (String) result.get("name");
			int startPageUrl = (Integer) result.get("startPage") / 2 * 2;
			String readOnlineURL = "https://archive.org/stream/" + bookId + "#page/n" + startPageUrl + "/mode/2up";
			String title = (String) result.get("title");
			if (result.get("volume")!=null)
				title += ", volume " + result.get("volume");
			
			String titleLang = (String) result.get("titleLang");
			if (titleLang!=null && result.get("volume")!=null)
				titleLang += ", באַנד " + result.get("volume");
			if (titleLang==null)
				titleLang = "";
			
			String author = (String) result.get("author");
			String authorLang = (String) result.get("authorLang");
			if (authorLang==null)
				authorLang = "";
			%>
			<tr><td height="5px" bgcolor="black"></td></tr>
			<tr><td align="left">
			<table style="width:100%;">
			<tr>
			<td class="Title" width="50%"><b>Title:</b> <a href="<%= result.get("url") %>" target="_blank"><%= title %></a></td>
			<td class="RTLTitle"><b>טיטל:</b> <a href="<%= result.get("url") %>" target="_blank"><%= titleLang %></a></td>
			</tr>
			<tr>
			<td class="Author" width="50%"><b>Author:</b> <%= author %></td>
			<td class="RTLAuthor"><b>מחבר:</b> <%= authorLang %></td>
			</tr>
			<tr>
			<td class="Author"  width="50%"><b>Section:</b> Pages <a href="<%= readOnlineURL %>" target="_blank"><%= result.get("startPage") %> to <%= result.get("endPage") %></a></td>
			<td class="RTLAuthor"><b>אָפּטײל:</b> זײַטן <a href="<%= readOnlineURL %>" target="_blank"><%= result.get("startPage") %> ביז <%= result.get("endPage") %></a></td>
			</tr>
			<%
			if (result.get("publisher")!=null) {
				%>
				<tr><td class="Author" colspan="2"><b>Publisher:</b> <%= result.get("publisher") %></td></tr>
				<%
			}
			%>
			<%
			if (result.get("date")!=null) {
				%>
				<tr><td class="Author" colspan="2"><b>Date:</b> <%= result.get("date") %></td></tr>
				<%
			}
			%>
			</table>
			</td></tr>
			<tr><td>
			<%
			@SuppressWarnings("unchecked")
			List<Map<String,Object>> snippets = (List<Map<String,Object>>) ((Map<String,Object>) snippetResults.get(result.get("docId").toString())).get("snippets");
			int j=0;
			for (Map<String,Object> snippet : snippets) {
				String imageUrlAddress = "search?command=imageSnippet&snippet=" + URLEncoder.encode(mapper.writeValueAsString(snippet), "UTF-8");
				int pageNumber = (Integer) snippet.get("pageIndex");
				int urlPageNumber = pageNumber / 2 * 2;
				readOnlineURL = "https://archive.org/stream/" + bookId + "#page/n" + urlPageNumber + "/mode/2up";
				%>
				<table style="width:100%; border:0px">
				<tr>
				<td><div id="snippet<%=i %>_<%=j %>" class="snippet"><%= snippet.get("text") %></div></td>
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