package com.joliciel.jochre.yiddish;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class JochreYiddishTest {

  @Test
  public void testImageFileToAlto4() throws Exception {
    Config config = ConfigFactory.load();
    Map<String, String> args = new HashMap<>();
    args.put("isCleanSegment", "true");
    args.put("lexicon", "resources/jochre-yiddish-lexicon-1.0.1.zip");
    args.put("letterModel", "resources/yiddish_letter_model.zip");

    JochreYiddish jochreYiddish = new JochreYiddish(config, args);

    File inputFile = getFileFromResource("yiddish_sample.jpg");
    try (StringWriter writer = new StringWriter();) {
      jochreYiddish.imageFileToAlto4(inputFile, writer);
      String alto = writer.toString();

      DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = builderFactory.newDocumentBuilder();
      InputSource is = new InputSource(new StringReader(alto));
      Document xmlDocument = builder.parse(is);
      XPath xPath = XPathFactory.newInstance().newXPath();
      String expression = "//String/@CONTENT";
      NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i<nodeList.getLength(); i++) {
        Node node = nodeList.item(i);
        sb.append(node.getTextContent());
      }
      String result = sb.toString();
      assertEquals(result, "מאַמע-לשון");
    }
  }

  @Test
  public void testImageInputStreamToAlto4() throws Exception {
    Config config = ConfigFactory.load();
    Map<String, String> args = new HashMap<>();
    args.put("isCleanSegment", "true");
    args.put("lexicon", "resources/jochre-yiddish-lexicon-1.0.1.zip");
    args.put("letterModel", "resources/yiddish_letter_model.zip");

    JochreYiddish jochreYiddish = new JochreYiddish(config, args);


    try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("yiddish_sample.jpg"); StringWriter writer = new StringWriter();) {
      jochreYiddish.imageInputStreamToAlto4(inputStream, "yiddish_sample.jpg", writer);
      String alto = writer.toString();

      DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = builderFactory.newDocumentBuilder();
      InputSource is = new InputSource(new StringReader(alto));
      Document xmlDocument = builder.parse(is);
      XPath xPath = XPathFactory.newInstance().newXPath();
      String expression = "//String/@CONTENT";
      NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i<nodeList.getLength(); i++) {
        Node node = nodeList.item(i);
        sb.append(node.getTextContent());
      }
      String result = sb.toString();
      assertEquals(result, "מאַמע-לשון");
    }
  }

  private File getFileFromResource(String resource) {
    URL url = this.getClass().getClassLoader().getResource(resource);
    File file = null;
    try {
      file = new File(url.toURI());
    } catch (URISyntaxException e) {
      file = new File(url.getPath());
    } finally {
      return file;
    }
  }
}
