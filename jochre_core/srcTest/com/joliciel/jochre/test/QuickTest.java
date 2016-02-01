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
package com.joliciel.jochre.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

public class QuickTest {
    private static final Log LOG = LogFactory.getLog(QuickTest.class);

    @Test
	public void testSubstring() {
		String text = "blah[blah]blah";
		for (int i = 0; i<text.length(); i++) {
			String letter = text.substring(i,i+1);
			if (letter.equals("[")) {
				int endIndex = text.indexOf("]", i);
				if (endIndex>=0) {
					letter = text.substring(i+1, endIndex);
					i = endIndex;
				}
			}
			LOG.debug(letter);
		}
	}
	
    @Test
	public void testReplaceAll() {
		String text = "bl|aa|h";
		text = text.replaceAll("\\|(.)\\1\\|", "$1");
		LOG.debug(text);

		text = "b|lala|h";
		text = text.replaceAll("\\|(..)\\1\\|", "$1");
		LOG.debug(text);
	}
	
    @Test
	public void testNumberFormat() {
		String number = "888888";
		int j = Integer.parseInt(number, 16);
		LOG.debug(j);
	}
}
