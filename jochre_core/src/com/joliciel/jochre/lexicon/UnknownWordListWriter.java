///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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
package com.joliciel.jochre.lexicon;

import java.io.IOException;
import java.io.Writer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.joliciel.jochre.doc.DocumentObserver;
import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Paragraph;
import com.joliciel.jochre.graphics.RowOfShapes;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.WeightedOutcome;

/**
 * Converts Jochre's analysis to human-readable text, either in plain or xhtml format.
 * Note that the current implementation has some Yiddish-specific rules (around Yiddish-style
 * double-quotes) which will need to be generalised.
 * @author Assaf Urieli
 *
 */
public class UnknownWordListWriter implements DocumentObserver {
	private static final Log LOG = LogFactory.getLog(UnknownWordListWriter.class);
	private Writer writer;
	
	public UnknownWordListWriter(Writer writer) {
		this.writer = writer;
	}

	@Override
	public void onDocumentStart(JochreDocument jochreDocument) {

	}

	@Override
	public void onPageStart(JochrePage jochrePage) {
		try {
			writer.write("#### Page " + jochrePage.getIndex() + "\n");
			writer.flush();
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}

	@Override
	public void onImageStart(JochreImage jochreImage) {
	}

	@Override
	public void onImageComplete(JochreImage image) {
		try {
			for (Paragraph paragraph : image.getParagraphs()) {
				if (!paragraph.isJunk()) {
					for (RowOfShapes row : paragraph.getRows()) {
						for (GroupOfShapes group : row.getGroups()) {
							if (group.getBestLetterSequence()!=null) {
								for (WeightedOutcome<String> wordFrequency : group.getBestLetterSequence().getWordFrequencies()) {
									if (wordFrequency.getWeight()==0) {
										writer.write(wordFrequency.getOutcome() + "\n");
										writer.flush();
									}
								}
							}
						}
					}
				}
			}
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}

	@Override
	public void onPageComplete(JochrePage jochrePage) {

	}

	@Override
	public void onDocumentComplete(JochreDocument jochreDocument) {
	
	}
}
