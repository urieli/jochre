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
import com.joliciel.jochre.search.JochreSearchException;

/**
 * Returns snippets which span an entire row, making it possible to match text
 * and graphic snippets.
 * 
 * @author Assaf Urieli
 *
 */
class FullRowSnippetFinder implements SnippetFinder {
  private static final Logger LOG = LoggerFactory.getLogger(FullRowSnippetFinder.class);
  private final int rowExtension;
  private final String configId;

  public FullRowSnippetFinder(String configId) {
    this.configId = configId;
    JochreSearchConfig config = JochreSearchConfig.getInstance(configId);
    this.rowExtension = config.getConfig().getInt("snippet-finder.row-extension");
  }

  @Override
  public List<Snippet> findSnippets(IndexSearcher indexSearcher, int docId, Set<String> fields,
      Set<HighlightTerm> highlightTerms, int maxSnippets) throws IOException {
    Document doc = indexSearcher.doc(docId);
    JochreIndexDocument jochreDoc = new JochreIndexDocument(indexSearcher, docId, configId);
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
        throw new RuntimeException(term.toString() + " cannot fit into contents for doc " + title + ", pages "
            + startPage + " to " + endPage + ", length: " + content.length());
      }

      int pageIndex = term.getPayload().getPageIndex();
      int rowIndex = term.getPayload().getRowIndex();

      double rowTop = jochreDoc.getRowRectangle(pageIndex, rowIndex).getY();
      int pageRowCount = jochreDoc.getRowCount(pageIndex);

      int startRowIndex = rowIndex - rowExtension;
      if (startRowIndex < 0)
        startRowIndex = 0;
      // avoid starting on another column
      while (startRowIndex < rowIndex && jochreDoc.getRowRectangle(pageIndex, startRowIndex).getY() > rowTop)
        startRowIndex++;
      int start = jochreDoc.getStartIndex(pageIndex, startRowIndex);

      int endRowIndex = rowIndex + rowExtension;
      if (endRowIndex >= pageRowCount)
        endRowIndex = pageRowCount - 1;
      // avoid ending on another column
      while (endRowIndex > rowIndex && jochreDoc.getRowRectangle(pageIndex, endRowIndex).getY() < rowTop)
        endRowIndex--;
      int end = jochreDoc.getEndIndex(pageIndex, endRowIndex);

      Snippet snippet = new Snippet(docId, term.getField(), start, end, pageIndex);
      snippet.setStartRowIndex(startRowIndex);
      snippet.setEndRowIndex(endRowIndex);

      List<HighlightTerm> snippetTerms = new ArrayList<>();
      snippetTerms.add(term);
      int j = -1;
      boolean snippetAlreadyAdded = false;
      for (HighlightTerm otherTerm : highlightTerms) {
        j++;
        if (j == i)
          continue;

        if (otherTerm.getStartOffset() >= snippet.getStartOffset()
            && otherTerm.getEndOffset() <= snippet.getEndOffset()) {
          if (j < i) {
            snippetAlreadyAdded = true;
            break;
          }

          snippetTerms.add(otherTerm);
          if (otherTerm.getPayload().getPageIndex() != term.getPayload().getPageIndex()) {
            LOG.debug("document: " + jochreDoc.getName());
            LOG.debug("pageIndex: " + pageIndex);
            LOG.debug("startRowIndex: " + startRowIndex);
            LOG.debug("endRowIndex: " + endRowIndex);
            LOG.debug("start: " + start);
            LOG.debug("end: " + end);

            LOG.debug("snippet: " + snippet);
            LOG.debug("term payload: " + term.getPayload());
            LOG.debug("otherTerm payload: " + otherTerm.getPayload());

            throw new JochreSearchException("otherTerm on wrong page. term: " + term + ", otherTerm: " + otherTerm
                + ", document: " + jochreDoc.getName() + ", pageIndex: " + pageIndex + ", startRowIndex: "
                + startRowIndex + ", endRowIndex: " + endRowIndex + ", start: " + start + ", end: " + end);
          }

          if (otherTerm.getPayload().getRowIndex() > term.getPayload().getRowIndex()
              && otherTerm.getPayload().getRowIndex() + 1 < pageRowCount) {
            rowIndex = otherTerm.getPayload().getRowIndex();
            rowTop = jochreDoc.getRowRectangle(pageIndex, rowIndex).getY();

            endRowIndex = rowIndex + rowExtension;
            if (endRowIndex >= pageRowCount)
              endRowIndex = pageRowCount - 1;
            // avoid ending on another column
            while (endRowIndex > rowIndex && jochreDoc.getRowRectangle(pageIndex, endRowIndex).getY() < rowTop)
              endRowIndex--;
            end = jochreDoc.getEndIndex(pageIndex, endRowIndex);

            snippet.setEndRowIndex(endRowIndex);
            snippet.setEndOffset(end);
          }
        } // term within current snippet boundaries
      } // next term

      snippet.setHighlightTerms(snippetTerms);

      if (!snippetAlreadyAdded)
        heap.add(snippet);
    }

    // if we have no snippets, add one per field type
    if (heap.isEmpty()) {
      String content = jochreDoc.getContents();

      int start = jochreDoc.getStartIndex(jochreDoc.getStartPage(), 0);
      int pageRowCount = jochreDoc.getRowCount(jochreDoc.getStartPage());
      int endRowIndex = start + (rowExtension * 2);
      while (endRowIndex >= pageRowCount)
        endRowIndex--;
      int end = jochreDoc.getEndIndex(jochreDoc.getStartPage(), endRowIndex);

      if (end > content.length())
        end = content.length();

      Snippet snippet = new Snippet(docId, fields.iterator().next(), 0, end, jochreDoc.getStartPage());
      snippet.setStartRowIndex(0);
      snippet.setEndRowIndex(endRowIndex);

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
