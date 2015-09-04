package com.joliciel.jochre.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.doc.DocumentObserver;
import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.talismane.utils.LogUtils;

public abstract class AbstractExporter implements DocumentObserver {
	private static final Log LOG = LogFactory.getLog(AbstractExporter.class);
	private File outputDir;
	protected Writer writer;
	private String suffix;
	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	private String dateString = format.format(new Date());
	
	public AbstractExporter(File outputDir, String suffix) {
		super();
		this.outputDir = outputDir;
		this.suffix = suffix;
	}

	public AbstractExporter(Writer writer) {
		super();
		this.writer = writer;
	}


	@Override
	public final void onDocumentStart(JochreDocument jochreDocument) {
		try {
			if (this.outputDir!=null) {
				File file = new File(outputDir, jochreDocument.getFileBase() + "_" + dateString + suffix);
				this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true),"UTF8"));
			}
			this.onDocumentStartInternal(jochreDocument);
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}

	protected abstract void onDocumentStartInternal(JochreDocument jochreDocument);

	@Override
	public final void onDocumentComplete(JochreDocument jochreDocument) {
		try {
			this.onDocumentCompleteInternal(jochreDocument);
			if (this.outputDir!=null) {
				this.writer.flush();
				this.writer.close();
			}
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}

	protected abstract void onDocumentCompleteInternal(JochreDocument jochreDocument);

}
