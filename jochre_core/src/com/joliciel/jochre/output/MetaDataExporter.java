package com.joliciel.jochre.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.doc.DocumentObserver;
import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.talismane.utils.LogUtils;

public class MetaDataExporter implements DocumentObserver {
	private static final Log LOG = LogFactory.getLog(MetaDataExporter.class);

	private File outDir;
	private String baseName;
	
	public MetaDataExporter(File outDir, String baseName) {
		super();
		this.outDir = outDir;
		this.baseName = baseName;
	}
	

	@Override
	public void onDocumentStart(JochreDocument jochreDocument) {
	}

	@Override
	public void onPageStart(JochrePage jochrePage) {
	}

	@Override
	public void onImageStart(JochreImage jochreImage) {
	}

	@Override
	public void onImageComplete(JochreImage jochreImage) {
	}

	@Override
	public void onPageComplete(JochrePage jochrePage) {
	}

	@Override
	public void onDocumentComplete(JochreDocument jochreDocument) {
		try {
			File metaDataFile = new File(outDir, baseName + "_metadata.txt");
			Writer metaWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(metaDataFile, false),"UTF8"));
			for (Entry<String, String> field : jochreDocument.getFields().entrySet()) {
				metaWriter.write(field.getKey() + "\t" + field.getValue() + "\n");
				metaWriter.flush();
			}
			metaWriter.close();
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	@Override
	public void onStart() {
	}

	@Override
	public void onComplete() {
	}
}
