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
package com.joliciel.jochre.search.alto;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.joliciel.talismane.utils.LogUtils;


class AltoReaderImpl extends DefaultHandler implements AltoReader {
	private static final Log LOG = LogFactory.getLog(AltoReaderImpl.class);
	private String tempVal = null;
    private AltoDocument doc = null;
    private AltoServiceInternal altoService;
    private AltoPage currentPage;
    private AltoTextBlock currentTextBlock;
    private AltoTextLine currentTextLine;
    private AltoString currentString;
    private int currentOffset;
    private boolean addedHyphen = false;
    
    public AltoReaderImpl() {
    }
    
    public AltoReaderImpl(AltoDocument doc) {
    	this.doc = doc;
    }
    
    @Override
	public AltoDocument parseFile(File altoFile) {
    	try {
	    	InputStream inputStream = new FileInputStream(altoFile);
	    	AltoDocument doc = this.parseFile(inputStream, altoFile.getParentFile().getName());
	    	return doc;
        } catch (IOException e) {
            LogUtils.logError(LOG, e);
            throw new RuntimeException(e);
        }
    }
    	
    public AltoDocument parseFile(InputStream inputStream, String documentName) {
    	if (this.doc==null)
    		this.doc = this.altoService.newDocument(documentName);
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try {
            SAXParser sp = spf.newSAXParser();
            sp.parse(inputStream, this);
            return this.doc;
        } catch(SAXException e) {
            LogUtils.logError(LOG, e);
            throw new RuntimeException(e);
        } catch(ParserConfigurationException e) {
            LogUtils.logError(LOG, e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            LogUtils.logError(LOG, e);
            throw new RuntimeException(e);
        }
    }
    
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    	// clear out tempVal whenever a new element is started
    	tempVal = "";
        if (qName.equals("Page")) {
        	int width = Integer.parseInt(attributes.getValue("WIDTH"));
        	int height = Integer.parseInt(attributes.getValue("HEIGHT"));
        	int pageIndex = Integer.parseInt(attributes.getValue("PHYSICAL_IMG_NR"));
        	currentPage = altoService.newPage(this.doc, pageIndex, width, height);
        	currentOffset = 0;
        } if (qName.equals("PrintSpace")) {
        	if (attributes.getValue("PC")!=null) {
        		double confidence = Double.parseDouble(attributes.getValue("PC"));
        		currentPage.setConfidence(confidence);
        	}
        } else if (qName.equals("TextBlock")) {
        	int left = Integer.parseInt(attributes.getValue("HPOS"));
        	int top = Integer.parseInt(attributes.getValue("VPOS"));
        	int width = Integer.parseInt(attributes.getValue("WIDTH"));
        	int height = Integer.parseInt(attributes.getValue("HEIGHT"));
        	currentTextBlock = altoService.newTextBlock(currentPage, left, top, width, height);
         	addedHyphen = false;
        } else if (qName.equals("TextLine")) {
           	int left = Integer.parseInt(attributes.getValue("HPOS"));
        	int top = Integer.parseInt(attributes.getValue("VPOS"));
        	int width = Integer.parseInt(attributes.getValue("WIDTH"));
        	int height = Integer.parseInt(attributes.getValue("HEIGHT"));
        	currentTextLine = altoService.nextTextLine(currentTextBlock, left, top, width, height);
        } else if (qName.equals("SP")) {
        	// a space
        	currentOffset += 1;
        	addedHyphen = false;
        } else if (qName.equals("HYP")) {
        	// a hyphen
           	int left = Integer.parseInt(attributes.getValue("HPOS"));
        	int top = Integer.parseInt(attributes.getValue("VPOS"));
        	int width = Integer.parseInt(attributes.getValue("WIDTH"));
        	int height = Integer.parseInt(attributes.getValue("HEIGHT"));
        	String content = attributes.getValue("CONTENT").replace("&quot;", "\"");
        	currentString = altoService.newString(currentTextLine, content, left, top, width, height);
        	
        	currentString.setSpanStart(currentOffset);
        	currentOffset += content.length();
        	currentString.setSpanEnd(currentOffset);
        	
        	if (attributes.getValue("WC")!=null) {
        		double confidence = Double.parseDouble(attributes.getValue("WC"));
        		currentString.setConfidence(confidence);
        	}
        	addedHyphen = true;
        } else if (qName.equals("String")) {
           	int left = Integer.parseInt(attributes.getValue("HPOS"));
        	int top = Integer.parseInt(attributes.getValue("VPOS"));
        	int width = Integer.parseInt(attributes.getValue("WIDTH"));
        	int height = Integer.parseInt(attributes.getValue("HEIGHT"));
        	String content = attributes.getValue("CONTENT").replace("&quot;", "\"");
        	currentString = altoService.newString(currentTextLine, content, left, top, width, height);
        	
        	currentString.setSpanStart(currentOffset);
        	currentOffset += content.length();
        	currentString.setSpanEnd(currentOffset);
        	
        	if (attributes.getValue("WC")!=null) {
        		double confidence = Double.parseDouble(attributes.getValue("WC"));
        		currentString.setConfidence(confidence);
        	}
        	
        	String subsType = attributes.getValue("SUBS_TYPE");
        	if (subsType!=null) {
        		if (subsType.equals("HypPart1"))
        			currentString.setHyphenStart(true);
        		else if (subsType.equals("HypPart2"))
        			currentString.setHyphenEnd(true);
        	}
        	
        	String hyphenatedContent = attributes.getValue("SUBS_CONTENT");
        	if (hyphenatedContent!=null)
        		currentString.setHyphenatedContent(hyphenatedContent);
        	addedHyphen = false;
        }
    }
    
    public void characters(char[] ch, int start, int length) throws SAXException {
    	// add the characters to tempVal
        tempVal += new String(ch,start,length);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
    	if (qName.equals("ALTERNATIVE")) {
    		currentString.getAlternatives().add(tempVal);
    	} else if (qName.equals("TextLine")) {
        	// an end of line is either a space or true end-of-line, add an offset unless there's a dash at the end of the previous line
        	if (!addedHyphen)
        		currentOffset += 1;
        	addedHyphen = false;
    	}
    }

	public AltoServiceInternal getAltoService() {
		return altoService;
	}

	public void setAltoService(AltoServiceInternal altoService) {
		this.altoService = altoService;
	}

    
}
