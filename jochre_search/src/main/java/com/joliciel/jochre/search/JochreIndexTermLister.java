package com.joliciel.jochre.search;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.joliciel.jochre.utils.JochreException;
import com.joliciel.talismane.utils.LogUtils;

public class JochreIndexTermLister {
	private static final Log LOG = LogFactory.getLog(JochreIndexTermLister.class);
	
	private int docId;
	private IndexSearcher indexSearcher;
	private TreeMap<Integer,JochreTerm> offsetTermMap;

	public JochreIndexTermLister(int docId, IndexSearcher indexSearcher) {
		super();
		this.docId = docId;
		this.indexSearcher = indexSearcher;
	}
	
	public Map<String, Set<JochreTerm>> list() {
		try {
			Map<String,Set<JochreTerm>> fieldTermMap = new HashMap<String, Set<JochreTerm>>();
			
			IndexReader reader = indexSearcher.getIndexReader();
	
			IndexReaderContext readerContext = reader.getContext();
			List<LeafReaderContext> leaves = readerContext.leaves();
			int leaf = ReaderUtil.subIndex(docId, leaves);

			Set<String> fields = new HashSet<String>();
			
			fields.add(JochreIndexField.text.name());
			fields.add(JochreIndexField.author.name());
			fields.add(JochreIndexField.title.name());
			fields.add(JochreIndexField.publisher.name());
			fields.add(JochreIndexField.authorLang.name());
			fields.add(JochreIndexField.titleLang.name());
			
			for (String field : fields)
				fieldTermMap.put(field, new TreeSet<JochreTerm>());
			
			if (LOG.isTraceEnabled())
				LOG.trace("Searching leaf " + leaf);

			LeafReaderContext subContext = leaves.get(leaf);
			LeafReader atomicReader = subContext.reader();
			
			int fieldCounter = 0;
			for (String field : fields) {
				fieldCounter++;
				if (LOG.isTraceEnabled())
					LOG.trace("Field " + fieldCounter + ": " + field);
				
				Terms atomicReaderTerms = atomicReader.terms(field);
				if (atomicReaderTerms == null) {
					LOG.trace("Empty reader");
					continue; // nothing to do
				}
				
				TermsEnum termsEnum = atomicReaderTerms.iterator();
				
				@SuppressWarnings("unused")
				BytesRef bytesRef = null;
				while ((bytesRef = termsEnum.next())!=null) {
					this.findTerms(fieldTermMap, field, termsEnum, subContext, docId);
				} // next bytesRef
			} // next field

			return fieldTermMap;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new JochreException(e);
		}
	}
	
	public NavigableMap<Integer,JochreTerm> getTextTermByOffset() {
		if (offsetTermMap==null) {
			offsetTermMap = new TreeMap<Integer,JochreTerm>();
			Map<String,Set<JochreTerm>> fieldTermMap = this.list();
			Set<JochreTerm> textTerms = fieldTermMap.get(JochreIndexField.text.name());
			for (JochreTerm jochreTerm : textTerms) {
				offsetTermMap.put(jochreTerm.getStart(), jochreTerm);
			}
		}
		return offsetTermMap;
	}
	
	public void list(Writer writer) {
		try {
			JsonFactory jsonFactory = new JsonFactory();
			JsonGenerator jsonGen = jsonFactory.createGenerator(writer);
			
			jsonGen.writeStartArray();
			
			Map<String, Set<JochreTerm>> textFeatureMap = this.list();
			for (String field : textFeatureMap.keySet()) {
				jsonGen.writeStartObject();
				jsonGen.writeStringField("field", field);
				jsonGen.writeFieldName("terms");
				jsonGen.writeStartArray();
				for (JochreTerm textFeature : textFeatureMap.get(field)) {
					jsonGen.writeStartObject();
					jsonGen.writeStringField("name", textFeature.getName());
					jsonGen.writeNumberField("position", textFeature.getPosition());
					jsonGen.writeNumberField("start", textFeature.getStart());
					jsonGen.writeNumberField("end", textFeature.getEnd());
					jsonGen.writeEndObject();
				}
				jsonGen.writeEndArray();
				jsonGen.writeEndObject();
			}
			
			jsonGen.writeEndArray();
			jsonGen.flush();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new JochreException(e);
		}
	}
	
	private void findTerms(Map<String,Set<JochreTerm>> textFeatureMap, String field, TermsEnum termsEnum, LeafReaderContext subContext,
			int luceneId) throws IOException {
		Term term = new Term(field, BytesRef.deepCopyOf(termsEnum.term()));
		
		PostingsEnum docPosEnum = termsEnum.postings(null, PostingsEnum.OFFSETS | PostingsEnum.POSITIONS | PostingsEnum.PAYLOADS);
		int relativeId = docPosEnum.nextDoc();
		while (relativeId!=PostingsEnum.NO_MORE_DOCS) {
			int nextId = subContext.docBase + relativeId;
            if (luceneId == nextId) {
	        	//Retrieve the term frequency in the current document
	            int freq=docPosEnum.freq();
	            
	            if (LOG.isTraceEnabled())
	            	LOG.trace("Found " + freq + " matches for term " + term.toString() + ", luceneId " + nextId + ", docId " + docId + ", field " + field);
	            
	            for(int i=0; i<freq; i++){
	                int position=docPosEnum.nextPosition();
	                int start=docPosEnum.startOffset();
	                int end=docPosEnum.endOffset();
	
	                if (LOG.isTraceEnabled())
	                	LOG.trace("Found match " + position + " at luceneId " + nextId + ", field " + field + " start=" + start + ", end=" + end);
	                
	                BytesRef bytesRef = docPosEnum.getPayload();
	                JochrePayload payload = null;
	                if (bytesRef!=null)
	                	payload = new JochrePayload(bytesRef);
	                
	                JochreTerm jochreTerm = new JochreTerm(term.toString(), position, start, end, payload);
	                Set<JochreTerm> jochreTerms = textFeatureMap.get(field);
	                jochreTerms.add(jochreTerm);
				} // next occurrence
            } // correct document

            relativeId = docPosEnum.nextDoc();
		}
	}
	
	public static final class JochreTerm implements Comparable<JochreTerm> {
		String name;
		int position;
		int start;
		int end;
		JochrePayload payload;
		
		public JochreTerm(String name, int position, int start, int end,
				JochrePayload payload) {
			super();
			this.name = name;
			this.position = position;
			this.start = start;
			this.end = end;
			this.payload = payload;
		}

		public String getName() {
			return name;
		}

		public int getPosition() {
			return position;
		}

		public int getStart() {
			return start;
		}

		public int getEnd() {
			return end;
		}

		public JochrePayload getPayload() {
			return payload;
		}

		@Override
		public int compareTo(JochreTerm o) {
			if (o.getStart()!=this.getStart())
				return this.getStart() - o.getStart();
			if (o.getEnd()!=this.getEnd())
				return this.getEnd() - o.getEnd();
			if (o.getPosition()!=this.getPosition())
				return this.getPosition() - o.getPosition();
			if (!o.getName().equals(this.getName()))
				return this.getName().compareTo(o.getName());
			return 1;
		}
	}
}
