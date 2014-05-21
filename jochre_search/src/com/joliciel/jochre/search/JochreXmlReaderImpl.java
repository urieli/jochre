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
package com.joliciel.jochre.search;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

class JochreXmlReaderImpl extends DefaultHandler implements JochreXmlReader {
    private String tempVal = null;
    private SearchDocument doc = null;
    private SearchServiceInternal searchService;
    private String fileNameBase;
    private SearchPage currentPage;
    private SearchParagraph currentParagraph;
    private SearchRow currentRow;
    private SearchWord currentWord;
    
    public JochreXmlReaderImpl(SearchDocument doc) {
    	this.doc = doc;
    }
    
    @Override
	public void parseFile(File xmlFile) {
    	this.fileNameBase = xmlFile.getName().substring(0, xmlFile.getName().lastIndexOf('.'));
    	
        //get a factory
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try {
        	
            //get a new instance of parser
            SAXParser sp = spf.newSAXParser();
            
            //parse the file and also register this class for call backs
            sp.parse(xmlFile, this);
            
        }catch(SAXException se) {
            se.printStackTrace();
        }catch(ParserConfigurationException pce) {
            pce.printStackTrace();
        }catch (IOException ie) {
            ie.printStackTrace();
        }
    }
    
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    	// clear out tempVal whenever a new element is started
    	tempVal = "";
        if (qName.equals("image")) {
        	int width = Integer.parseInt(attributes.getValue("width"));
        	int height = Integer.parseInt(attributes.getValue("height"));
//        	String lang = attributes.getValue("lang");
        	currentPage = searchService.newPage(fileNameBase, width, height);
        	this.doc.getPages().add(currentPage);
        } else if (qName.equals("paragraph")) {
        	int left = Integer.parseInt(attributes.getValue("l"));
        	int top = Integer.parseInt(attributes.getValue("t"));
        	int right = Integer.parseInt(attributes.getValue("r"));
        	int bottom = Integer.parseInt(attributes.getValue("b"));
        	currentParagraph = searchService.newParagraph(left, top, right, bottom);
        	currentPage.getParagraphs().add(currentParagraph);
        } else if (qName.equals("row")) {
           	int left = Integer.parseInt(attributes.getValue("l"));
        	int top = Integer.parseInt(attributes.getValue("t"));
        	int right = Integer.parseInt(attributes.getValue("r"));
        	int bottom = Integer.parseInt(attributes.getValue("b"));
        	currentRow = searchService.newRow(left, top, right, bottom);
        	currentParagraph.getRows().add(currentRow);
        } else if (qName.equals("word")) {
           	int left = Integer.parseInt(attributes.getValue("l"));
        	int top = Integer.parseInt(attributes.getValue("t"));
        	int right = Integer.parseInt(attributes.getValue("r"));
        	int bottom = Integer.parseInt(attributes.getValue("b"));
        	String text = attributes.getValue("text").replace("&quot;", "\"");
        	boolean known = attributes.getValue("known").equals("true");
        	currentWord = searchService.newWord(text, left, top, right, bottom);
        	currentWord.setKnown(known);
        	currentRow.getWords().add(currentWord);
        } else if (qName.equals("char")) {
           	int left = Integer.parseInt(attributes.getValue("l"));
        	int top = Integer.parseInt(attributes.getValue("t"));
        	int right = Integer.parseInt(attributes.getValue("r"));
        	int bottom = Integer.parseInt(attributes.getValue("b"));
        	String text = attributes.getValue("letter").replace("&quot;", "\"");
        	int confidence = Integer.parseInt(attributes.getValue("charConfidence"));
        	SearchLetter letter = searchService.newLetter(currentWord, text, left, top, right, bottom);
        	letter.setConfidence(confidence);
        	currentWord.getLetters().add(letter);
        } 
    }
    
    public void characters(char[] ch, int start, int length) throws SAXException {
    	// add the characters to tempVal
        tempVal += new String(ch,start,length);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
    	// do nothing for now
    }

	public SearchServiceInternal getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchServiceInternal searchService) {
		this.searchService = searchService;
	}
    
    
}
