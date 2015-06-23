package com.joliciel.jochre.search;

import java.io.Reader;
import java.util.List;

import com.joliciel.jochre.search.alto.AltoString;

interface TokenExtractor {

	List<AltoString> findTokens(String fieldName, Reader input);
}
