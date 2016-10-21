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
package com.joliciel.jochre.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.ImageStatus;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Paragraph;
import com.joliciel.jochre.graphics.RowOfShapes;
import com.joliciel.jochre.graphics.Segmenter;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.graphics.Shape.SectionBrightnessMeasurementMethod;
import com.joliciel.jochre.graphics.SourceImage;
import com.joliciel.jochre.graphics.features.EmptyCentreFeature;
import com.joliciel.jochre.graphics.features.ShapeFeature;
import com.joliciel.jochre.graphics.features.TouchesBaseLineFeature;
import com.joliciel.jochre.graphics.features.TouchesMeanLineFeature;
import com.joliciel.jochre.graphics.features.VerticalElongationFeature;
import com.joliciel.jochre.graphics.features.VerticalSizeFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class SegmentationTest {
	private static final Logger LOG = LoggerFactory.getLogger(SegmentationTest.class);

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testSegmentation() throws Exception {
		// TODO: Note currently this requires high thresholds to work
		// Need to decide if this is valid in general, or only for these samples
		System.setProperty("config.file", "src/test/resources/testHighThresholds.conf");
		ConfigFactory.invalidateCaches();
		Config config = ConfigFactory.load();
		JochreSession jochreSession = new JochreSession(config);

		boolean writePixelsToLog = true;

		for (int imageNumber = 1; imageNumber <= 4; imageNumber++) {
			if (imageNumber != 1)
				continue;
			String imageName = "";
			String suffix = "";
			String text = "";
			String fileName = "";
			String userFileName;
			int rowCount = 2;
			int shapeCountRow1;
			int shapeCountRow2;
			int groupCountRow1;
			int groupCountRow2;
			int groupCountRow3 = 0;
			int shapeCountRow1Group1;
			int shapeCountRow2Group1;
			if (imageNumber == 1) {
				imageName = "MotlPeysiDemKhazns2RowsShort2";
				suffix = "jpg";
				text = "איך געה מיט אייך קיינער אין דער וועלט";
				fileName = "MotlPeysiDemKhazns2RowsShort2.pdf";
				userFileName = "Motl Peysi Dem Khazns";
				shapeCountRow1 = 13;
				shapeCountRow2 = 17;
				groupCountRow1 = 4;
				groupCountRow2 = 4;
				shapeCountRow1Group1 = 3;
				shapeCountRow2Group1 = 6;
			} else if (imageNumber == 2) {
				imageName = "MegileLiderZeresh";
				suffix = "png";
				text = "זרש, די מכשפה, װאָס שעלט ווי אַ מגפה";
				fileName = "MegileLiderManger.pdf";
				userFileName = "Megile Lider";
				shapeCountRow1 = 12;
				shapeCountRow2 = 17;
				groupCountRow1 = 3;
				groupCountRow2 = 5;
				shapeCountRow1Group1 = 4;
				shapeCountRow2Group1 = 4;
			} else if (imageNumber == 3) {
				imageName = "MendeleMoykherSforimVol1_41_0Excerpt";
				suffix = "png";
				text = "ער הייסט יאַנקיל, בעריל,";
				fileName = "MendeleMoykherSforimVol1_41_0.png";
				userFileName = "MendeleMoykherSforimVol1_41_0";
				shapeCountRow1 = 20;
				shapeCountRow2 = 0;
				groupCountRow1 = 4;
				groupCountRow2 = 0;
				shapeCountRow1Group1 = 2;
				shapeCountRow2Group1 = 0;
			} else {
				imageName = "JoinedLetterTest";
				suffix = "png";
				text = "Joined Letter Test";
				fileName = "JoinedLetterTest.png";
				userFileName = "JoinedLetterTest";
				rowCount = 2;
				shapeCountRow1 = 23;
				shapeCountRow2 = 23;
				groupCountRow1 = 4;
				groupCountRow2 = 4;
				groupCountRow3 = 5;
				shapeCountRow1Group1 = 6;
				shapeCountRow2Group1 = 5;
			}

			LOG.debug("######### imageName: " + imageName);

			// String fileName = "data/Zelmenyaners3Words.gif";
			InputStream imageFileStream = getClass().getResourceAsStream("/com/joliciel/jochre/test/resources/" + imageName + "." + suffix);
			assertNotNull(imageFileStream);
			BufferedImage image = ImageIO.read(imageFileStream);

			JochreDocument doc = new JochreDocument(jochreSession);
			doc.setFileName(fileName);
			doc.setName(userFileName);

			JochrePage page = doc.newPage();

			SourceImage sourceImage = page.newJochreImage(image, imageName);
			sourceImage.setWhiteGapFillFactor(5);
			sourceImage.setImageStatus(ImageStatus.AUTO_NEW);

			if (writePixelsToLog) {
				LOG.debug("i012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789");
				for (int y = 0; y < sourceImage.getHeight(); y++) {
					String line = "" + y;
					for (int x = 0; x < sourceImage.getWidth(); x++) {
						if (sourceImage.isPixelBlack(x, y, sourceImage.getBlackThreshold()))
							line += "x";
						else
							line += "o";
					}
					LOG.debug(line);
				}
			}

			Segmenter segmenter = new Segmenter(sourceImage, jochreSession);
			segmenter.segment();

			if (segmenter.isDrawSegmentation()) {
				BufferedImage segmentedImage = segmenter.getSegmentedImage();
				File tempDir = new File(System.getProperty("java.io.tmpdir"));
				ImageIO.write(segmentedImage, "PNG", new File(tempDir, imageName + "_seg.png"));
			}

			JochreImage jochreImage = sourceImage;
			int i = 0;
			boolean firstShape = true;
			int midPixelFirstShape = 0;
			int midPixelFirstShapeRaw = 0;

			for (Paragraph paragraph : jochreImage.getParagraphs()) {
				for (RowOfShapes row : paragraph.getRows()) {
					int j = 0;
					LOG.debug("============= Row " + i + " ================");
					for (Shape shape : row.getShapes()) {
						LOG.debug("Shape (" + i + "," + j + "). Left = " + shape.getLeft() + ". Top = " + shape.getTop() + ". Right = " + shape.getRight()
								+ ". Bottom = " + shape.getBottom());

						if (firstShape) {
							midPixelFirstShape = shape.getPixel(3, 3);
							midPixelFirstShapeRaw = shape.getRawPixel(3, 3);
							firstShape = false;
						}
						if (writePixelsToLog) {
							for (int y = 0; y < shape.getHeight(); y++) {
								String line = "";
								if (y == shape.getMeanLine())
									line += "M";
								else if (y == shape.getBaseLine())
									line += "B";
								else
									line += y;
								for (int x = 0; x < shape.getWidth(); x++) {
									if (shape.isPixelBlack(x, y, sourceImage.getBlackThreshold()))
										line += "x";
									else
										line += "o";
								}
								LOG.debug(line);
							}
						}
						j++;
					} // next shape
					i++;
				} // next row
			} // next paragraph

			i = 0;
			for (Paragraph paragraph : jochreImage.getParagraphs()) {
				for (RowOfShapes row : paragraph.getRows()) {
					for (GroupOfShapes group : row.getGroups()) {
						for (Shape shape : group.getShapes()) {
							if (i < text.length()) {
								String letter = text.substring(i, i + 1);
								String nextLetter = "";
								if (i + 1 < text.length())
									nextLetter = text.substring(i + 1, i + 2);
								if (nextLetter.equals("ָֹ") || nextLetter.equals("ַ")) {
									letter += nextLetter;
									i++;
								}
								LOG.debug("Letter: " + letter);
								shape.setLetter(letter);
							}
							i++;
						}
						i++; // to skip the space
						LOG.debug("Space");
					} // next group
				} // next row
			} // next paragraph

			List<ShapeFeature<?>> features = new ArrayList<ShapeFeature<?>>();
			features.add(new VerticalElongationFeature());
			features.add(new VerticalSizeFeature());
			features.add(new TouchesBaseLineFeature());
			features.add(new TouchesMeanLineFeature());
			features.add(new EmptyCentreFeature());

			i = 0;

			DecimalFormat df = new DecimalFormat("0.00");
			firstShape = true;
			int totalRowCount = 0;
			for (Paragraph paragraph : jochreImage.getParagraphs()) {
				for (RowOfShapes row : paragraph.getRows()) {
					totalRowCount++;
					LOG.debug("============= Row " + i + " ================");

					int j = 0;
					for (GroupOfShapes group : row.getGroups()) {
						for (Shape shape : group.getShapes()) {
							LOG.debug("============= Shape (" + i + "," + j + ") ================");
							LOG.debug("Left = " + shape.getLeft() + ". Top = " + shape.getTop() + ". Right = " + shape.getRight() + ". Bottom = "
									+ shape.getBottom());
							LOG.debug("Letter " + shape.getLetter());
							if (firstShape) {
								LOG.debug("mid pixel: " + midPixelFirstShape);
								assertEquals(midPixelFirstShape, shape.getPixel(3, 3));
								LOG.debug("mid pixel raw: " + midPixelFirstShapeRaw);
								assertEquals(midPixelFirstShapeRaw, shape.getRawPixel(3, 3));
								firstShape = false;
							}
							if (writePixelsToLog) {
								for (int y = 0; y < shape.getHeight(); y++) {
									String line = "";
									if (y == shape.getMeanLine())
										line += "M";
									else if (y == shape.getBaseLine())
										line += "B";
									else
										line += y;
									for (int x = 0; x < shape.getWidth(); x++) {
										if (shape.isPixelBlack(x, y, sourceImage.getBlackThreshold()))
											line += "x";
										else
											line += "o";
									}
									LOG.debug(line);
								}
							}
							double[][] totals = shape.getBrightnessBySection(5, 5, 1, SectionBrightnessMeasurementMethod.RAW);
							LOG.debug("Brightness counts");
							for (int y = 0; y < totals[0].length; y++) {
								String line = "";
								for (int x = 0; x < totals.length; x++) {
									line += df.format(totals[x][y]) + "\t";
								}
								LOG.debug(line);
							}

							for (ShapeFeature<?> feature : features) {
								RuntimeEnvironment env = new RuntimeEnvironment();
								FeatureResult<?> outcome = feature.check(shape, env);
								LOG.debug(outcome.toString());
							}
						}
						if (i == 0) {
							if (j == 0)
								assertEquals(shapeCountRow1Group1, group.getShapes().size());
						} else if (i == 1) {
							if (j == 0)
								assertEquals(shapeCountRow2Group1, group.getShapes().size());
						}

						j++;
					}
					if (i == 0)
						assertEquals(groupCountRow1, row.getGroups().size());
					else if (i == 1)
						assertEquals(groupCountRow2, row.getGroups().size());
					else if (i == 2)
						assertEquals(groupCountRow3, row.getGroups().size());

					if (i == 0)
						assertEquals(shapeCountRow1, row.getShapes().size());
					else if (i == 1)
						assertEquals(shapeCountRow2, row.getShapes().size());

					i++;
				} // next row
			} // next paragraph
			assertEquals(rowCount, totalRowCount);

		} // next test image

		LOG.debug("************** Finished ***********");
	}

}
