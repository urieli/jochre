///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Assaf Urieli
//
//This file is part of Jochre.
//
//Jochre is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Jochre is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Jochre.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.jochre.search.highlight;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.search.JochreIndexDocument;
import com.joliciel.jochre.search.JochreIndexField;
import com.joliciel.jochre.search.JochreSearchConfig;

class FixedSizeSnippetFinder implements SnippetFinder {
  private static final Logger LOG = LoggerFactory.getLogger(FixedSizeSnippetFinder.class);
  private final int snippetSize;
  private final JochreSearchConfig config;

  public FixedSizeSnippetFinder(JochreSearchConfig config) {
    this.config = config;
    this.snippetSize = config.getConfig().getInt("snippet-finder.snippet-size");
  }

  @Override
  public List<Snippet> findSnippets(IndexSearcher indexSearcher, int docId, Set<String> fields, Set<HighlightTerm> highlightTerms, int maxSnippets)
      throws IOException {
    Document doc = indexSearcher.doc(docId);
    JochreIndexDocument jochreDoc = new JochreIndexDocument(indexSearcher, docId, config);
    // find best snippet for each term
    PriorityQueue<Snippet> heap = new PriorityQueue<>();

    int i = -1;
    for (HighlightTerm term : highlightTerms) {
      i++;
      String content = jochreDoc.getContents();
      if (term.getStartOffset() >= content.length()) {
        String title = doc.get(JochreIndexField.titleEnglish.name());
        String startPage = doc.get(JochreIndexField.startPage.name());
        String endPage = doc.get(JochreIndexField.endPage.name());
        LOG.debug("Content: " + content);
        throw new RuntimeException(term.toString() + " cannot fit into contents for doc " + title + ", pages " + startPage + " to " + endPage
            + ", length: " + content.length());
      }
      List<HighlightTerm> snippetTerms = new ArrayList<>();
      snippetTerms.add(term);
      int j = -1;
      boolean foundPage = false;
      for (HighlightTerm otherTerm : highlightTerms) {
        j++;
        if (j <= i)
          continue;
        if (otherTerm.getPayload().getPageIndex() != term.getPayload().getPageIndex()) {
          if (foundPage)
            break;
          else
            continue;
        }
        foundPage = true;

        if (otherTerm.getStartOffset() < term.getStartOffset() + snippetSize) {
          snippetTerms.add(otherTerm);
        } else {
          break;
        }
      }
      HighlightTerm lastTerm = snippetTerms.get(snippetTerms.size() - 1);
      int middle = (term.getStartOffset() + lastTerm.getEndOffset()) / 2;
      int start = middle - (snippetSize / 2);
      int end = middle + (snippetSize / 2);
      if (start > term.getStartOffset())
        start = term.getStartOffset();
      if (end < lastTerm.getEndOffset())
        end = lastTerm.getEndOffset();

      if (start < 0)
        start = 0;
      if (end > content.length())
        end = content.length();

      for (int k = start; k >= 0; k--) {
        if (Character.isWhitespace(content.charAt(k))) {
          start = k + 1;
          break;
        }
      }
      for (int k = end; k < content.length(); k++) {
        if (Character.isWhitespace(content.charAt(k))) {
          end = k;
          break;
        }
      }

      if (start < 0)
        start = 0;

      Snippet snippet = new Snippet(docId, term.getField(), start, end, term.getPayload().getPageIndex());
      snippet.setHighlightTerms(snippetTerms);
      heap.add(snippet);
    }

    // if we have no snippets, add one per field type
    if (heap.isEmpty()) {
      String content = jochreDoc.getContents();
      int end = snippetSize * maxSnippets;
      if (end > content.length())
        end = content.length();
      for (int k = end; k < content.length(); k++) {
        if (Character.isWhitespace(content.charAt(k))) {
          end = k;
          break;
        }
      }
      Snippet snippet = new Snippet(docId, fields.iterator().next(), 0, end, jochreDoc.getStartPage());
      if (LOG.isTraceEnabled())
        LOG.trace("Snippet candidate: " + snippet);
      heap.add(snippet);
    }

    List<Snippet> snippets = new ArrayList<>(maxSnippets);
    while (snippets.size() < maxSnippets && !heap.isEmpty()) {
      Snippet snippet = heap.poll();
      boolean hasOverlap = false;
      for (Snippet otherSnippet : snippets) {
        if (otherSnippet.hasOverlap(snippet))
          hasOverlap = true;
      }
      if (!hasOverlap)
        snippets.add(snippet);
    }

    for (Snippet snippet : snippets) {
      LOG.debug("Added snippet: " + snippet.toJson());
    }
    return snippets;
  }
}
