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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import mockit.Expectations;
import mockit.Mocked;

public class LineDefinitionImplTest {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(LineDefinitionImplTest.class);

	@Test
	public void testTrace(@Mocked final Shape shape) {
		LineDefinition lineDef = new LineDefinition(0, 0);
		List<Integer> steps = new ArrayList<>();
		steps.add(2);
		steps.add(3);
		lineDef.setSteps(steps);

		new Expectations() {
			{
				shape.getHeight();
				result = 8;
				minTimes = 0;
				shape.getWidth();
				result = 8;
				minTimes = 0;
			}
		};

		BitSet bitset = new BitSet(shape.getHeight() * shape.getWidth());
		lineDef.trace(bitset, shape, 5, 2, 8, 0);

		int[] bitsetPixels = { 0, 0, 0, 0, 0, 0, 0, 0, // row
				0, 0, 0, 0, 0, 0, 0, 0, // row
				0, 0, 0, 0, 1, 1, 0, 0, // row
				0, 1, 1, 1, 0, 0, 0, 0, // row
				1, 0, 0, 0, 0, 0, 0, 0, // row
				0, 0, 0, 0, 0, 0, 0, 0, // row
				0, 0, 0, 0, 0, 0, 0, 0, // row
				0, 0, 0, 0, 0, 0, 0, 0 // row
		};

		for (int x = 0; x < 8; x++)
			for (int y = 0; y < 8; y++) {
				assertEquals(bitsetPixels[y * 8 + x] == 1, bitset.get(y * 8 + x));
			}

		bitset = new BitSet(shape.getHeight() * shape.getWidth());
		lineDef.trace(bitset, shape, 1, 1, 4, 2);

		int[] bitsetPixels2 = { 0, 0, 0, 0, 0, 0, 0, 0, // row
				0, 1, 0, 0, 0, 0, 0, 0, // row
				0, 1, 0, 0, 0, 0, 0, 0, // row
				0, 0, 1, 0, 0, 0, 0, 0, // row
				0, 0, 1, 0, 0, 0, 0, 0, // row
				0, 0, 1, 0, 0, 0, 0, 0, // row
				0, 0, 0, 0, 0, 0, 0, 0, // row
				0, 0, 0, 0, 0, 0, 0, 0 // row
		};

		for (int x = 0; x < 8; x++)
			for (int y = 0; y < 8; y++) {
				assertEquals("failure at x=" + x + ",y=" + y, bitsetPixels2[y * 8 + x] == 1, bitset.get(y * 8 + x));
			}
	}

	@Test
	public void testFollow(@Mocked final Shape shape) {
		LineDefinition lineDef = new LineDefinition(0, 0);
		List<Integer> steps = new ArrayList<>();
		steps.add(2);
		steps.add(3);
		lineDef.setSteps(steps);

		new Expectations() {
			{
				shape.getHeight();
				result = 8;
				minTimes = 0;
				shape.getWidth();
				result = 8;
				minTimes = 0;
			}
		};

		int[] endPoint = lineDef.follow(shape, 5, 2, 4, 0);

		assertEquals(1, endPoint[0]);
		assertEquals(3, endPoint[1]);
	}

	@Test
	public void testFollowInShape() throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		Config config = ConfigFactory.load();
		JochreSession jochreSession = new JochreSession(config);

		LineDefinition lineDef = new LineDefinition(0, 0);
		List<Integer> steps = new ArrayList<>();
		steps.add(2);
		steps.add(3);
		lineDef.setSteps(steps);

		int[] pixels = { 0, 1, 1, 0, 0, 1, 1, 1, // row
				0, 1, 1, 1, 0, 1, 1, 1, // row
				0, 0, 1, 1, 0, 0, 1, 1, // row
				0, 0, 1, 1, 0, 1, 1, 0, // row
				0, 0, 0, 1, 1, 1, 1, 0, // row
				0, 0, 0, 1, 1, 1, 0, 0, // row
				0, 0, 1, 1, 1, 0, 0, 0, // row
				1, 1, 1, 1, 1, 0, 0, 0 // row
		};

		Shape shape = new ShapeMock(pixels, 8, 8, jochreSession);

		int[] endPoint = lineDef.followInShape(shape, 5, 5, 0, 100, 0);

		assertEquals(2, endPoint[0]);
		assertEquals(6, endPoint[1]);
		assertEquals(3, endPoint[2]); // the length
	}
}
