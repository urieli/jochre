package com.joliciel.jochre.segmentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.Paragraph;
import com.joliciel.jochre.graphics.RowOfShapes;
import com.joliciel.jochre.graphics.Segmenter;
import com.joliciel.jochre.graphics.SourceImage;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import mockit.Expectations;
import mockit.Mocked;

/**
 * In these rather resource-heavy tests, we analyse real images, and ensure that
 * the textual paragraphs have been found in the correct order, with the correct
 * area (more or less), and with the correct number of rows and number of words
 * on certain sample rows.
 * 
 * @author Assaf Urieli
 *
 */
public class SegmentationTest {
	private static final Logger LOG = LoggerFactory.getLogger(SegmentationTest.class);

	/**
	 * A very simple, basic page.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMotlPessiDemKhazns(@Mocked JochrePage jochrePage) throws Exception {
		Map<String, Object> configMap = new HashMap<>();
		configMap.put("jochre.locale", "yi");
		Config config = ConfigFactory.parseMap(configMap).withFallback(ConfigFactory.load());
		JochreSession jochreSession = new JochreSession(config);

		String imageName = "SholemAleykhem_MotelPeysiDemKhazns_12_0.png";
		LOG.debug(imageName);
		InputStream imageFileStream = getClass().getResourceAsStream("/com/joliciel/jochre/segmentation/" + imageName);
		assertNotNull(imageFileStream);
		BufferedImage image = ImageIO.read(imageFileStream);
		SourceImage sourceImage = new SourceImage(jochrePage, "", image, jochreSession);

		Segmenter segmenter = new Segmenter(sourceImage, jochreSession);
		segmenter.segment();

		List<Rectangle> textPars = new ArrayList<>();
		Rectangle textPar1 = new Rectangle(544, 824, 2432, 344);
		Rectangle textPar2 = new Rectangle(552, 1176, 2512, 2112);
		Rectangle textPar3 = new Rectangle(584, 3320, 2448, 344);
		Rectangle textPar4 = new Rectangle(568, 3688, 2464, 592);

		textPars.add(textPar1);
		textPars.add(textPar2);
		textPars.add(textPar3);
		textPars.add(textPar4);

		int i = 0;
		int j = 0;
		List<Paragraph> textParagraphs = new ArrayList<>();
		for (Paragraph par : sourceImage.getParagraphs()) {
			Rectangle real = new Rectangle(par.getLeft(), par.getTop(), par.getRight() - par.getLeft(), par.getBottom() - par.getTop());
			Rectangle expected = textPars.get(i);
			Rectangle intersection = expected.intersection(real);
			double realArea = real.width * real.height;
			double expectedArea = expected.width * expected.height;
			double intersectionArea = intersection.width * intersection.height;
			double realRatio = intersectionArea / realArea;
			double expectedRatio = intersectionArea / expectedArea;

			LOG.debug("Paragraph " + j);
			LOG.debug("realRatio: " + realRatio);
			LOG.debug("expectedRatio: " + expectedRatio);
			if (realRatio > 0.95 && expectedRatio > 0.8) {
				LOG.debug("Found");
				textParagraphs.add(par);
				i++;
			}
			j++;
		}

		assertEquals(textPars.size(), textParagraphs.size());

		int[] rowCounts = new int[] { 3, 18, 3, 5 };
		int[] wordCountsFirstRow = new int[] { 6, 8, 8, 8 };

		for (i = 0; i < textParagraphs.size(); i++) {
			assertEquals(rowCounts[i], textParagraphs.get(i).getRows().size());
			RowOfShapes row = textParagraphs.get(i).getRows().get(0);
			assertEquals(wordCountsFirstRow[i], row.getGroups().size());
		}

	}

	/**
	 * Pietrushka is a bit unusual in that it contains a column separator in the
	 * middle. Given that it's in Yiddish, the columns need to be aligned from right
	 * to left.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPietrushka(@Mocked JochrePage jochrePage) throws Exception {
		Map<String, Object> configMap = new HashMap<>();
		configMap.put("jochre.locale", "yi");
		Config config = ConfigFactory.parseMap(configMap).withFallback(ConfigFactory.load());
		JochreSession jochreSession = new JochreSession(config);

		String imageName = "Pietrushka_FolksEntsiklopedyeVol1_17_0.png";
		LOG.debug(imageName);
		InputStream imageFileStream = getClass().getResourceAsStream("/com/joliciel/jochre/segmentation/" + imageName);
		assertNotNull(imageFileStream);
		BufferedImage image = ImageIO.read(imageFileStream);
		SourceImage sourceImage = new SourceImage(jochrePage, "", image, jochreSession);

		Segmenter segmenter = new Segmenter(sourceImage, jochreSession);
		segmenter.segment();

		List<Rectangle> textPars = new ArrayList<>();
		Rectangle textPar1 = new Rectangle(1832, 688, 1336, 1864);
		Rectangle textPar2 = new Rectangle(1848, 2608, 1312, 2200);
		Rectangle textPar3 = new Rectangle(1848, 4856, 1296, 94);
		Rectangle textPar4 = new Rectangle(448, 696, 1320, 2080);
		Rectangle textPar5 = new Rectangle(448, 2816, 1328, 2128);

		textPars.add(textPar1);
		textPars.add(textPar2);
		textPars.add(textPar3);
		textPars.add(textPar4);
		textPars.add(textPar5);

		int i = 0;
		int j = 0;
		List<Paragraph> textParagraphs = new ArrayList<>();
		for (Paragraph par : sourceImage.getParagraphs()) {
			Rectangle real = new Rectangle(par.getLeft(), par.getTop(), par.getRight() - par.getLeft(), par.getBottom() - par.getTop());
			Rectangle expected = textPars.get(i);
			Rectangle intersection = expected.intersection(real);
			double realArea = real.width * real.height;
			double expectedArea = expected.width * expected.height;
			double intersectionArea = intersection.width * intersection.height;
			double realRatio = intersectionArea / realArea;
			double expectedRatio = intersectionArea / expectedArea;

			LOG.debug("Paragraph " + j);
			LOG.debug("realRatio: " + realRatio);
			LOG.debug("expectedRatio: " + expectedRatio);
			if (realRatio > 0.95 && expectedRatio > 0.8) {
				LOG.debug("Found");
				textParagraphs.add(par);
				i++;
			}
			j++;
		}

		assertEquals(textPars.size(), textParagraphs.size());

		int[] rowCounts = new int[] { 18, 21, 1, 20, 21 };
		int[] wordCountsFirstRow = new int[] { 6, 7, 7, 8, 7 };

		for (i = 0; i < textParagraphs.size(); i++) {
			assertEquals(rowCounts[i], textParagraphs.get(i).getRows().size());
			RowOfShapes row = textParagraphs.get(i).getRows().get(0);
			assertEquals(wordCountsFirstRow[i], row.getGroups().size());
		}

	}

	/**
	 * This page is challenging because of the large blotch of dirt in the lower
	 * right. Also, there are several short, indented, one-line paragraphs.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPerets_KhsidisheMayselekh(@Mocked JochrePage jochrePage) throws Exception {
		Map<String, Object> configMap = new HashMap<>();
		configMap.put("jochre.locale", "yi");
		Config config = ConfigFactory.parseMap(configMap).withFallback(ConfigFactory.load());
		JochreSession jochreSession = new JochreSession(config);

		String imageName = "Peretz_KhsidisheMayselekh_5_0.png";
		LOG.debug(imageName);
		InputStream imageFileStream = getClass().getResourceAsStream("/com/joliciel/jochre/segmentation/" + imageName);
		assertNotNull(imageFileStream);
		BufferedImage image = ImageIO.read(imageFileStream);
		SourceImage sourceImage = new SourceImage(jochrePage, "", image, jochreSession);

		Segmenter segmenter = new Segmenter(sourceImage, jochreSession);
		segmenter.segment();

		List<Rectangle> textPars = new ArrayList<>();
		Rectangle textPar1 = new Rectangle(1670, 920, 624, 182);
		Rectangle textPar2 = new Rectangle(740, 1524, 2368, 96);
		Rectangle textPar3 = new Rectangle(680, 1652, 2620, 192);
		Rectangle textPar4 = new Rectangle(652, 1872, 2648, 800);
		Rectangle textPar5 = new Rectangle(1564, 2696, 1524, 96);
		Rectangle textPar6 = new Rectangle(660, 2820, 2632, 328);
		Rectangle textPar7 = new Rectangle(660, 3176, 2628, 212);
		Rectangle textPar8 = new Rectangle(664, 3404, 2636, 428);
		Rectangle textPar9 = new Rectangle(1992, 3868, 1088, 100);
		Rectangle textPar10 = new Rectangle(664, 4000, 2624, 436);
		Rectangle textPar11 = new Rectangle(664, 4468, 2628, 204);

		textPars.add(textPar1); // title paragraph
		textPars.add(textPar2);
		textPars.add(textPar3);
		textPars.add(textPar4);
		textPars.add(textPar5);
		textPars.add(textPar6);
		textPars.add(textPar7);
		textPars.add(textPar8);
		textPars.add(textPar9); // short paragraph
		textPars.add(textPar10); // paragraph with blotch
		textPars.add(textPar11);

		int i = 0;
		int j = 0;
		List<Paragraph> textParagraphs = new ArrayList<>();
		for (Paragraph par : sourceImage.getParagraphs()) {
			Rectangle real = new Rectangle(par.getLeft(), par.getTop(), par.getRight() - par.getLeft(), par.getBottom() - par.getTop());
			Rectangle expected = textPars.get(i);
			Rectangle intersection = expected.intersection(real);
			double realArea = real.width * real.height;
			double expectedArea = expected.width * expected.height;
			double intersectionArea = intersection.width * intersection.height;
			double realRatio = intersectionArea / realArea;
			double expectedRatio = intersectionArea / expectedArea;

			LOG.debug("Paragraph " + j);
			LOG.debug("realRatio: " + realRatio);
			LOG.debug("expectedRatio: " + expectedRatio);
			if (realRatio > 0.8 && expectedRatio > 0.8) {
				LOG.debug("Found");
				textParagraphs.add(par);
				i++;
			}
			j++;
		}

		assertEquals(textPars.size(), textParagraphs.size());

		int[] rowCounts = new int[] { 1, 1, 2, 7, 1, 3, 2, 4, 1, 4, 2 };
		int[] wordCountsFirstRow = new int[] { 1, 0, 12, 0, 0, 0, 0, 0, 0, 0, 0 };

		for (i = 0; i < textParagraphs.size(); i++) {
			assertEquals("row count " + i, rowCounts[i], textParagraphs.get(i).getRows().size());
			RowOfShapes row = textParagraphs.get(i).getRows().get(0);
			if (wordCountsFirstRow[i] > 0)
				assertEquals("word count " + i, wordCountsFirstRow[i], row.getGroups().size());
		}

	}

	/**
	 * Segmentation errors reported for Alsacien.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAlsacien1(@Mocked final JochrePage jochrePage, @Mocked final JochreDocument jochreDoc) throws Exception {
		Map<String, Object> configMap = new HashMap<>();
		configMap.put("jochre.locale", "de");
		Config config = ConfigFactory.parseMap(configMap).withFallback(ConfigFactory.load());
		JochreSession jochreSession = new JochreSession(config);

		new Expectations() {
			{
				jochrePage.getDocument();
				result = jochreDoc;
				minTimes = 0;
				jochreDoc.isLeftToRight();
				result = true;
				minTimes = 0;
			}
		};

		String imageName = "Alsacien1.jpg";
		LOG.debug(imageName);
		InputStream imageFileStream = getClass().getResourceAsStream("/com/joliciel/jochre/segmentation/" + imageName);
		assertNotNull(imageFileStream);
		BufferedImage image = ImageIO.read(imageFileStream);
		SourceImage sourceImage = new SourceImage(jochrePage, "", image, jochreSession);

		Segmenter segmenter = new Segmenter(sourceImage, jochreSession);
		segmenter.segment();

		List<Rectangle> textPars = new ArrayList<>();
		Rectangle textPar1 = new Rectangle(715, 517, 462, 115);
		// TODO: for now it's splitting this paragraph by row, since it's assuming
		// paragraphs cannot be
		// both outdented and indented on the same page
		// Rectangle textPar2 = new Rectangle(50, 666, 1798, 1039);
		Rectangle textPar3 = new Rectangle(55, 1837, 1777, 335);
		Rectangle textPar4 = new Rectangle(50, 2211, 1765, 154);
		Rectangle textPar5 = new Rectangle(44, 2404, 1782, 511);
		Rectangle textPar6 = new Rectangle(50, 2948, 1776, 154);
		Rectangle textPar7 = new Rectangle(50, 3135, 1770, 77);

		textPars.add(textPar1); // title paragraph
		// textPars.add(textPar2);
		textPars.add(textPar3);
		textPars.add(textPar4);
		textPars.add(textPar5);
		textPars.add(textPar6);
		textPars.add(textPar7);

		int i = 0;
		int j = 0;
		List<Paragraph> textParagraphs = new ArrayList<>();
		for (Paragraph par : sourceImage.getParagraphs()) {
			Rectangle real = new Rectangle(par.getLeft(), par.getTop(), par.getRight() - par.getLeft(), par.getBottom() - par.getTop());
			Rectangle expected = textPars.get(i);
			Rectangle intersection = expected.intersection(real);
			double realArea = real.width * real.height;
			double expectedArea = expected.width * expected.height;
			double intersectionArea = intersection.width * intersection.height;
			double realRatio = intersectionArea / realArea;
			double expectedRatio = intersectionArea / expectedArea;

			LOG.debug("Paragraph " + j + ": " + par.toString());
			LOG.debug("realRatio: " + realRatio);
			LOG.debug("expectedRatio: " + expectedRatio);
			if (realRatio > 0.8 && expectedRatio > 0.8) {
				LOG.debug("Found");
				textParagraphs.add(par);
				i++;
			}
			j++;
		}

		assertEquals(textPars.size(), textParagraphs.size());

		int[] rowCounts = new int[] { 1, 4, 2, 6, 2, 1 };
		int[] wordCountsFirstRow = new int[] { 2, 0, 0, 0, 0, 0, 0 };

		for (i = 0; i < textParagraphs.size(); i++) {
			assertEquals("row count " + i, rowCounts[i], textParagraphs.get(i).getRows().size());
			RowOfShapes row = textParagraphs.get(i).getRows().get(0);
			if (wordCountsFirstRow[i] > 0)
				assertEquals("word count " + i, wordCountsFirstRow[i], row.getGroups().size());
		}

	}

	/**
	 * Segmentation errors reported for Alsacien.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAlsacien2(@Mocked final JochrePage jochrePage, @Mocked final JochreDocument jochreDoc) throws Exception {
		Map<String, Object> configMap = new HashMap<>();
		configMap.put("jochre.locale", "de");
		Config config = ConfigFactory.parseMap(configMap).withFallback(ConfigFactory.load());
		JochreSession jochreSession = new JochreSession(config);

		new Expectations() {
			{
				jochrePage.getDocument();
				result = jochreDoc;
				minTimes = 0;
				jochreDoc.isLeftToRight();
				result = true;
				minTimes = 0;
			}
		};

		String imageName = "Alsacien2.jpeg";
		LOG.debug(imageName);
		InputStream imageFileStream = getClass().getResourceAsStream("/com/joliciel/jochre/segmentation/" + imageName);
		assertNotNull(imageFileStream);
		BufferedImage image = ImageIO.read(imageFileStream);
		SourceImage sourceImage = new SourceImage(jochrePage, "", image, jochreSession);

		Segmenter segmenter = new Segmenter(sourceImage, jochreSession);
		segmenter.segment();

		List<Rectangle> textPars = new ArrayList<>();
		Rectangle textPar1 = new Rectangle(63, 81, 1059, 108);
		Rectangle textPar2 = new Rectangle(66, 204, 1065, 294);
		Rectangle textPar3 = new Rectangle(63, 516, 1068, 348);
		Rectangle textPar4 = new Rectangle(63, 879, 1071, 537);
		Rectangle textPar5 = new Rectangle(63, 1428, 1068, 354);

		textPars.add(textPar1); // title paragraph
		textPars.add(textPar2);
		textPars.add(textPar3);
		textPars.add(textPar4);
		textPars.add(textPar5);

		int i = 0;
		int j = 0;
		List<Paragraph> textParagraphs = new ArrayList<>();
		for (Paragraph par : sourceImage.getParagraphs()) {
			Rectangle real = new Rectangle(par.getLeft(), par.getTop(), par.getRight() - par.getLeft(), par.getBottom() - par.getTop());
			Rectangle expected = textPars.get(i);
			Rectangle intersection = expected.intersection(real);
			double realArea = real.width * real.height;
			double expectedArea = expected.width * expected.height;
			double intersectionArea = intersection.width * intersection.height;
			double realRatio = intersectionArea / realArea;
			double expectedRatio = intersectionArea / expectedArea;

			LOG.debug("Paragraph " + j + ": " + par.toString());
			LOG.debug("realRatio: " + realRatio);
			LOG.debug("expectedRatio: " + expectedRatio);
			if (realRatio > 0.8 && expectedRatio > 0.8) {
				LOG.debug("Found");
				textParagraphs.add(par);
				i++;
				if (i >= textPars.size())
					break;
			}
			j++;
		}

		assertEquals(textPars.size(), textParagraphs.size());

		int[] rowCounts = new int[] { 2, 5, 6, 9, 6 };
		int[] wordCountsFirstRow = new int[] { 10, 8, 9, 8, 8 };

		for (i = 0; i < textParagraphs.size(); i++) {
			assertEquals("row count " + i, rowCounts[i], textParagraphs.get(i).getRows().size());
			RowOfShapes row = textParagraphs.get(i).getRows().get(0);
			if (wordCountsFirstRow[i] > 0)
				assertEquals("word count " + i, wordCountsFirstRow[i], row.getGroups().size());
		}

	}

	/**
	 * Segmentation errors reported for Alsacien play - challenging because of the
	 * unusual indentation.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAlsacienPlay3(@Mocked final JochrePage jochrePage, @Mocked final JochreDocument jochreDoc) throws Exception {
		Map<String, Object> configMap = new HashMap<>();
		configMap.put("jochre.locale", "de");
		Config config = ConfigFactory.parseMap(configMap).withFallback(ConfigFactory.load());
		JochreSession jochreSession = new JochreSession(config);

		new Expectations() {
			{
				jochrePage.getDocument();
				result = jochreDoc;
				minTimes = 0;
				jochreDoc.isLeftToRight();
				result = true;
				minTimes = 0;
			}
		};

		String imageName = "AlsacienPlay3.jpg";
		LOG.debug(imageName);
		InputStream imageFileStream = getClass().getResourceAsStream("/com/joliciel/jochre/segmentation/" + imageName);
		assertNotNull(imageFileStream);
		BufferedImage image = ImageIO.read(imageFileStream);
		SourceImage sourceImage = new SourceImage(jochrePage, "", image, jochreSession);

		Segmenter segmenter = new Segmenter(sourceImage, jochreSession);
		segmenter.segment();

		List<Rectangle> textPars = new ArrayList<>();
		Rectangle textPar1 = new Rectangle(712, 532, 556, 52);
		Rectangle textPar2 = new Rectangle(324, 600, 1324, 128);
		Rectangle textPar3 = new Rectangle(680, 730, 592, 50);
		Rectangle textPar4 = new Rectangle(404, 808, 684, 48);

		textPars.add(textPar1); // title paragraph
		textPars.add(textPar2);
		textPars.add(textPar3);
		textPars.add(textPar4);

		int i = 0;
		int j = 0;
		List<Paragraph> textParagraphs = new ArrayList<>();
		for (Paragraph par : sourceImage.getParagraphs()) {
			Rectangle real = new Rectangle(par.getLeft(), par.getTop(), par.getRight() - par.getLeft(), par.getBottom() - par.getTop());
			Rectangle expected = textPars.get(i);
			Rectangle intersection = expected.intersection(real);
			double realArea = real.width * real.height;
			double expectedArea = expected.width * expected.height;
			double intersectionArea = intersection.width * intersection.height;
			double realRatio = intersectionArea / realArea;
			double expectedRatio = intersectionArea / expectedArea;

			LOG.debug("Paragraph " + j + ": " + par.toString());
			LOG.debug("realRatio: " + realRatio);
			LOG.debug("expectedRatio: " + expectedRatio);
			if (realRatio > 0.8 && expectedRatio > 0.8) {
				LOG.debug("Found");
				textParagraphs.add(par);
				i++;
				if (i >= textPars.size())
					break;
			}
			j++;
		}

		assertEquals(textPars.size(), textParagraphs.size());

		int[] rowCounts = new int[] { 1, 2, 1, 1 };
		// TODO: words in "spaced" rows (uses spacing to emphasize instead of bold
		// or italics) get split
		// should try to detect multiple single letter words
		int[] wordCountsFirstRow = new int[] { 0, 10, 0, 5 };

		for (i = 0; i < textParagraphs.size(); i++) {
			assertEquals("row count " + i, rowCounts[i], textParagraphs.get(i).getRows().size());
			RowOfShapes row = textParagraphs.get(i).getRows().get(0);
			if (wordCountsFirstRow[i] > 0)
				assertEquals("word count " + i, wordCountsFirstRow[i], row.getGroups().size());
		}

	}
}
