package com.joliciel.jochre.doc;

import java.io.File;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.EntityImpl;
import com.joliciel.jochre.graphics.GraphicsService;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Paragraph;
import com.joliciel.jochre.graphics.RowOfShapes;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.lang.Linguistics;
import com.joliciel.jochre.security.SecurityService;
import com.joliciel.jochre.security.User;
import com.joliciel.talismane.utils.PersistentList;
import com.joliciel.talismane.utils.PersistentListImpl;

class JochreDocumentImpl extends EntityImpl implements
		JochreDocumentInternal {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(JochreDocumentImpl.class);
	private DocumentServiceInternal documentServiceInternal;
	private GraphicsService graphicsService;
	private SecurityService securityService;
	
	private String fileName = "";
	private String name = "";
	private List<JochrePage> pages;
	private Locale locale;

	private int ownerId;
	private User owner;
	private String nameLocal = "";
	private String publisher = "";
	private String city = "";
	private int year;
	private String reference = "";

	private List<JochreImage> images;
	private PersistentList<Author> authors;
	
	private int totalPageCount;

	@Override
	public List<JochrePage> getPages() {
		if (this.pages==null) {
			if (this.isNew())
				this.pages = new ArrayList<JochrePage>();
			else
				this.pages = this.documentServiceInternal.findPages(this);
		}
		return pages;
	}

	@Override
	public JochrePage newPage() {
		JochrePageInternal page = this.documentServiceInternal.getEmptyJochrePageInternal();
		page.setDocument(this);
		this.getPages().add(page);
		page.setIndex(this.getPages().size());
		return page;
	}

	@Override
	public JochrePage getCurrentPage() {
		if (this.getPages().size()==0)
			return null;
		return this.getPages().get(this.getPages().size()-1);
	}

	@Override
	public void saveInternal() {
		this.documentServiceInternal.saveJochreDocument(this);
		if (this.pages!=null) {
			for (JochrePage page : this.pages) {
				page.save();
			}
		}
		if (this.authors!=null) {
			if (this.authors.isDirty()) {
				this.documentServiceInternal.replaceAuthors(this);
			}
		}
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}
	
	@Override
	public void getXml(OutputStream outputStream) {
      try {
    	  XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
    	  XMLStreamWriter writer = xmlOutputFactory.createXMLStreamWriter(outputStream);
    	  writer.writeStartDocument("UTF-8", "1.0");
    	  writer.writeStartElement("doc");
    	  writer.writeAttribute("name", this.getName());
    	  writer.writeAttribute("fileName", this.getFileName());
    	  writer.writeAttribute("locale", this.getLocale().getLanguage());   	  

          for (JochrePage page : this.getPages()) {
        	  writer.writeStartElement("page");
        	  writer.writeAttribute("index", "" + page.getIndex());
        	  for (JochreImage image : page.getImages()) {
        		  writer.writeStartElement("image");
        		  writer.writeAttribute("name", image.getName());
        		  writer.writeAttribute("index", "" + image.getIndex());
        		  for (Paragraph paragraph : image.getParagraphs()) {
        			  writer.writeStartElement("paragraph");
        			  writer.writeAttribute("index", "" + paragraph.getIndex());
        			  StringBuffer sb = new StringBuffer();
        			  for (RowOfShapes row : paragraph.getRows()) {
        				  for (GroupOfShapes group : row.getGroups()) {
        					  for (Shape shape : group.getShapes()) {
        						  if (shape.getLetter()!=null)
        							  sb.append(shape.getLetter());
        					  }
        					  sb.append(" ");
        				  }
        				  sb.append("\r\n");
        			  }
        			  writer.writeCData(sb.toString());
        			  writer.writeEndElement(); // paragraph
        		  }
        		  writer.writeEndElement(); // image
        	  }
        	  writer.writeEndElement(); // page
          }
          writer.writeEndElement(); // doc
          writer.writeEndDocument();
          writer.flush();
        }
        catch (XMLStreamException e) {
        	throw new RuntimeException(e);
        }

	}

	@Override
	public void segment() {
		for (JochrePage page : this.getPages())
			page.segment();
	}

	@Override
	public void segmentAndShow(String outputDirectory) {
		File outputPath = new File( outputDirectory );
		if( outputPath.exists() == false )
			outputPath.mkdirs();
		
		for (JochrePage page : this.getPages())
			page.segmentAndShow(outputDirectory);
	}
	
	public boolean isLeftToRight() {
		Linguistics linguistics = Linguistics.getInstance(locale);
		return linguistics.isLeftToRight();
	}

	public List<JochreImage> getImages() {
		if (this.images==null) {
			images = new ArrayList<JochreImage>();
			for (JochrePage page : this.getPages()) {
				for (JochreImage image : page.getImages()) {
					images.add(image);
				}
			}
		}
		return images;
	}

	@Override
	public int getOwnerId() {
		return ownerId;
	}

	@Override
	public void setOwnerId(int ownerId) {
		this.ownerId = ownerId;
		this.owner = null;
	}

	@Override
	public User getOwner() {
		if (this.owner==null && this.ownerId!=0)
			this.owner = this.getSecurityService().loadUser(this.ownerId);
		return owner;
	}

	@Override
	public void setOwner(User owner) {
		this.setOwnerId(owner.getId());
		this.owner = owner;
	}

	public SecurityService getSecurityService() {
		return securityService;
	}

	public void setSecurityService(SecurityService securityService) {
		this.securityService = securityService;
	}

	@Override
	public String getNameLocal() {
		return nameLocal;
	}

	@Override
	public void setNameLocal(String nameLocal) {
		this.nameLocal = nameLocal;
	}

	@Override
	public String getPublisher() {
		return publisher;
	}

	@Override
	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	@Override
	public String getCity() {
		return city;
	}

	@Override
	public void setCity(String city) {
		this.city = city;
	}

	@Override
	public int getYear() {
		return year;
	}

	@Override
	public void setYear(int year) {
		this.year = year;
	}

	public List<Author> getAuthors() {
		if (this.authors==null) {
			this.authors = new PersistentListImpl<Author>();
			if (!this.isNew()) {
				this.authors.addAllFromDB(this.getDocumentServiceInternal().findAuthors(this));
			}
		}
		return authors;
	}

	public DocumentServiceInternal getDocumentServiceInternal() {
		return documentServiceInternal;
	}

	public void setDocumentServiceInternal(
			DocumentServiceInternal documentServiceInternal) {
		this.documentServiceInternal = documentServiceInternal;
	}
	
	@Override
	public int hashCode() {
		if (this.isNew())
			return super.hashCode();
		else
			return ((Integer)this.getId()).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this.isNew()) {
			return super.equals(obj);
		} else {
			JochreDocument other = (JochreDocument) obj;
			return (this.getId()==other.getId());
		}
	}

	@Override
	public void deleteImage(JochreImage image) {
		if (!image.getPage().getDocument().equals(this)) {
			throw new RuntimeException("Cannot delete image from document - image is on another document");
		}
		JochrePage page = image.getPage();
		boolean lastImageOnPage = (page.getImages().size()==1);
		this.getGraphicsService().deleteJochreImage(image);
		if (lastImageOnPage)
			this.getDocumentServiceInternal().deleteJochrePage(page);
		this.getPages().remove(page);
	}

	public GraphicsService getGraphicsService() {
		return graphicsService;
	}

	public void setGraphicsService(GraphicsService graphicsService) {
		this.graphicsService = graphicsService;
	}

	@Override
	public String getReference() {
		return reference;
	}

	@Override
	public void setReference(String reference) {
		this.reference = reference;
	}

	public int getTotalPageCount() {
		return totalPageCount;
	}

	public void setTotalPageCount(int totalPageCount) {
		this.totalPageCount = totalPageCount;
	}
	
}
