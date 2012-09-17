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
package com.joliciel.jochre;

import java.beans.PropertyVetoException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.analyser.AnalyserServiceLocator;
import com.joliciel.jochre.boundaries.BoundaryServiceLocator;
import com.joliciel.jochre.boundaries.features.BoundaryFeatureServiceLocator;
import com.joliciel.jochre.doc.DocumentServiceLocator;
import com.joliciel.jochre.graphics.GraphicsServiceLocator;
import com.joliciel.jochre.graphics.features.GraphicsFeatureServiceLocator;
import com.joliciel.jochre.letterGuesser.LetterGuesserServiceLocator;
import com.joliciel.jochre.letterGuesser.features.LetterFeatureServiceLocator;
import com.joliciel.jochre.lexicon.LexiconServiceLocator;
import com.joliciel.jochre.pdf.PdfServiceLocator;
import com.joliciel.jochre.security.SecurityServiceLocator;
import com.joliciel.jochre.text.TextServiceLocator;
import com.joliciel.talismane.utils.features.FeatureService;
import com.joliciel.talismane.utils.features.FeatureServiceLocator;
import com.joliciel.talismane.utils.util.ObjectCache;
import com.joliciel.talismane.utils.util.SimpleObjectCache;
import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * Top-level locator for implementations of Jochre interfaces.
 * @author Assaf Urieli
 *
 */
public class JochreServiceLocator {
	private static final Log LOG = LogFactory.getLog(JochreServiceLocator.class);
	private GraphicsServiceLocator graphicsServiceLocator;
	private DocumentServiceLocator documentServiceLocator;
	private LetterGuesserServiceLocator letterGuesserServiceLocator;
	private AnalyserServiceLocator analyserServiceLocator;
	private BoundaryServiceLocator boundaryServiceLocator;
	private LexiconServiceLocator lexiconServiceLocator;
	private SecurityServiceLocator securityServiceLocator;
	private TextServiceLocator textServiceLocator;
	private PdfServiceLocator pdfServiceLocator;
	private BoundaryFeatureServiceLocator boundaryFeatureServiceLocator;
	private GraphicsFeatureServiceLocator graphicsFeatureServiceLocator;
	private LetterFeatureServiceLocator letterFeatureServiceLocator;
	
	private FeatureService featureService;
	
    private DataSource dataSource;
    private String dataSourcePropertiesResource;
    private Properties dataSourceProperties;
    private ObjectCache objectCache;
    private static JochreServiceLocator instance = null;

    private JochreServiceLocator() {
    	
    }
    
    public static JochreServiceLocator getInstance() {
    	if (instance==null) {
    		instance = new JochreServiceLocator();
    	}
    	return instance;
    }
    
    /**
     * The data source resource path as a classpath resource.
     * @return
     */
    public String getDataSourcePropertiesResource() {
        return dataSourcePropertiesResource;
    }

    public void setDataSourcePropertiesResource(String dataSourcePropertiesFile) {
        this.dataSourcePropertiesResource = dataSourcePropertiesFile;
    }
    
    public void setDataSourceProperties(InputStream is) {
        dataSourceProperties = new Properties();
        try {
	         dataSourceProperties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } 
    }
    
    private Properties getDataSourceProperties() {
        if (dataSourceProperties==null) {
            dataSourceProperties = new Properties();
            try {
            	LOG.debug("Loading database properties from: " + this.getDataSourcePropertiesResource());
                URL url =  ClassLoader.getSystemResource(this.getDataSourcePropertiesResource());
                String file = url.getFile();
                FileInputStream fis = new FileInputStream(file);
                dataSourceProperties.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }        
        }
        return dataSourceProperties;
    }
    
    private DataSource getDataSource() {
        if (dataSource==null) {
            ComboPooledDataSource ds = new ComboPooledDataSource();
            Properties props = this.getDataSourceProperties();
            try {
                ds.setDriverClass(props.getProperty("jdbc.driverClassName"));
            } catch (PropertyVetoException e) {
                 e.printStackTrace();
                 throw new RuntimeException(e);
            }
            ds.setJdbcUrl(props.getProperty("jdbc.url"));
            ds.setUser(props.getProperty("jdbc.username"));
            ds.setPassword(props.getProperty("jdbc.password"));
            dataSource = ds;
        }
        return dataSource;
    }

	public GraphicsServiceLocator getGraphicsServiceLocator() {
		if (this.graphicsServiceLocator==null) {
			this.graphicsServiceLocator = new GraphicsServiceLocator(this, this.getDataSource());
		}
		return graphicsServiceLocator;
	}
	
	public DocumentServiceLocator getDocumentServiceLocator() {
		if (this.documentServiceLocator==null) {
			this.documentServiceLocator = new DocumentServiceLocator(this, this.getDataSource());
		}
		return documentServiceLocator;
	}
	
	public LetterGuesserServiceLocator getLetterGuesserServiceLocator() {
		if (this.letterGuesserServiceLocator==null) {
			this.letterGuesserServiceLocator = new LetterGuesserServiceLocator(this);
		}
		return letterGuesserServiceLocator;
	}
	
	
	
	public AnalyserServiceLocator getAnalyserServiceLocator() {
		if (analyserServiceLocator==null) {
			analyserServiceLocator = new AnalyserServiceLocator(this);
		}
		return analyserServiceLocator;
	}

	public BoundaryServiceLocator getBoundaryServiceLocator() {
		if (boundaryServiceLocator==null) {
			boundaryServiceLocator = new BoundaryServiceLocator(this, this.getDataSource());
		}
		return boundaryServiceLocator;
	}

	public LexiconServiceLocator getLexiconServiceLocator() {
		if (this.lexiconServiceLocator==null) {
			this.lexiconServiceLocator = new LexiconServiceLocator(this);
		}
		return lexiconServiceLocator;
	}
	
	public SecurityServiceLocator getSecurityServiceLocator() {
		if (this.securityServiceLocator==null) {
			this.securityServiceLocator = new SecurityServiceLocator(this, this.getDataSource());
		}
		return securityServiceLocator;
	}

	public TextServiceLocator getTextServiceLocator() {
		if (this.textServiceLocator==null) {
			this.textServiceLocator = new TextServiceLocator(this, this.getDataSource());
		}
		return textServiceLocator;
	}

	public PdfServiceLocator getPdfServiceLocator() {
		if (this.pdfServiceLocator==null) {
			this.pdfServiceLocator = new PdfServiceLocator(this);
		}
		return pdfServiceLocator;
	}
	
	public BoundaryFeatureServiceLocator getBoundaryFeatureServiceLocator() {
		if (this.boundaryFeatureServiceLocator==null) {
			this.boundaryFeatureServiceLocator = new BoundaryFeatureServiceLocator(this);
		}
		return boundaryFeatureServiceLocator;
	}

	
	public GraphicsFeatureServiceLocator getGraphicsFeatureServiceLocator() {
		if (this.graphicsFeatureServiceLocator==null) {
			this.graphicsFeatureServiceLocator = new GraphicsFeatureServiceLocator(this);
		}
		return graphicsFeatureServiceLocator;
	}
	
	public LetterFeatureServiceLocator getLetterFeatureServiceLocator() {
		if (this.letterFeatureServiceLocator==null) {
			this.letterFeatureServiceLocator = new LetterFeatureServiceLocator(this);
		}
		return letterFeatureServiceLocator;
	}
	
	public ObjectCache getObjectCache() {
		if (this.objectCache==null)
			this.objectCache = new SimpleObjectCache();
		return objectCache;
	}

	public void setObjectCache(ObjectCache objectCache) {
		this.objectCache = objectCache;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public FeatureService getFeatureService() {
		if (featureService==null) {
			featureService = FeatureServiceLocator.getInstance().getFeatureService();
		}
		return featureService;
	}
	
}
