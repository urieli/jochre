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
package com.joliciel.jochre.graphics;

import static org.junit.Assert.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.BitSet;

import javax.imageio.ImageIO;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import mockit.Expectations;
import mockit.Mocked;

public class ShapeFillerImplTest {
	private static final Logger LOG = LoggerFactory.getLogger(ShapeFillerImplTest.class);

	@Test
	public void testGetFillFactor(@Mocked final JochreImage jochreImage) throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		Config config = ConfigFactory.load();
		JochreSession jochreSession = new JochreSession(config);

		new Expectations() {
			{
				jochreImage.normalize(255);
				result = 255;
				minTimes = 0;
				jochreImage.normalize(0);
				result = 0;
				minTimes = 0;
			}
		};

		for (int i = 0; i <= 2; i++) {
			String imageName = "";
			if (i == 0) {
				imageName = "AlephWithHoles.png";
			} else if (i == 1) {
				imageName = "TesWithHoles.png";
			} else {
				imageName = "AlephNoHoles.png";
			}
			LOG.debug(imageName);
			InputStream imageFileStream = getClass().getResourceAsStream("/com/joliciel/jochre/test/resources/" + imageName);
			assertNotNull(imageFileStream);
			BufferedImage image = ImageIO.read(imageFileStream);

			Shape shape = new Shape(jochreImage, jochreSession);
			shape.setImage(image);
			shape.setTop(0);
			shape.setLeft(0);
			shape.setRight(image.getWidth() - 1);
			shape.setBottom(image.getHeight() - 1);

			ShapeFiller shapeFiller = new ShapeFiller();
			shapeFiller.getFillFactor(shape, 100);
		}
	}

	@Test
	public void testFillShape(@Mocked final JochreImage jochreImage) throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		Config config = ConfigFactory.load();
		JochreSession jochreSession = new JochreSession(config);

		new Expectations() {
			{
				jochreImage.normalize(255);
				result = 255;
				minTimes = 0;
				jochreImage.normalize(0);
				result = 0;
				minTimes = 0;
			}
		};

		for (int i = 0; i <= 1; i++) {
			String imageName = "";
			int fillFactor = 0;
			if (i == 0) {
				imageName = "AlephWithHoles.png";
				fillFactor = 5;
			} else if (i == 1) {
				imageName = "TesWithHoles.png";
				fillFactor = 5;
			} else {
				imageName = "AlephNoHoles.png";
				fillFactor = 1;
			}
			LOG.debug(imageName);
			InputStream imageFileStream = getClass().getResourceAsStream("/com/joliciel/jochre/test/resources/" + imageName);
			assertNotNull(imageFileStream);
			BufferedImage image = ImageIO.read(imageFileStream);
			Shape shape = new Shape(jochreImage, jochreSession);
			shape.setImage(image);
			shape.setTop(0);
			shape.setLeft(0);
			shape.setRight(image.getWidth() - 1);
			shape.setBottom(image.getHeight() - 1);

			ShapeFiller shapeFiller = new ShapeFiller();
			BitSet bitset = shapeFiller.fillShape(shape, 100, fillFactor);
			for (int y = 0; y < shape.getHeight(); y++) {
				StringBuilder line = new StringBuilder();
				for (int x = 0; x < shape.getWidth(); x++) {
					if (bitset.get(y * shape.getWidth() + x))
						line.append("x");
					else
						line.append("o");
				}
				LOG.debug(line.toString());
			}
		}
	}

	@Test
	public void testFillBitSet() throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		Config config = ConfigFactory.load();
		JochreSession jochreSession = new JochreSession(config);
		final int threshold = 100;
		final int width = 8;
		final int height = 8;

		int[] pixels = { 0, 1, 1, 0, 0, 1, 1, 1, // row 0
				0, 1, 0, 1, 0, 1, 0, 1, // row 1
				0, 0, 1, 1, 0, 0, 1, 1, // row 2
				0, 0, 1, 1, 0, 1, 1, 0, // row 3
				0, 0, 0, 1, 0, 1, 1, 0, // row 4
				0, 0, 0, 1, 1, 1, 0, 0, // row 5
				0, 0, 1, 0, 1, 0, 0, 0, // row 6
				1, 1, 0, 1, 1, 0, 0, 0, // row 7
		};

		ShapeMock shape = new ShapeMock(pixels, width, height, jochreSession);

		ShapeFiller shapeFiller = new ShapeFiller();
		BitSet filledBitSet = shapeFiller.fillBitSet(shape, shape.getBlackAndWhiteBitSet(threshold), 5);
		for (int y = 0; y < height; y++) {
			StringBuilder line = new StringBuilder();
			for (int x = 0; x < width; x++) {
				if (filledBitSet.get(y * width + x))
					line.append("x");
				else
					line.append("o");
			}
			LOG.debug(line.toString());
		}

	}
}
