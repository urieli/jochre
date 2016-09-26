package com.joliciel.jochre.doc;

import java.io.File;
import java.io.OutputStream;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.Entity;
import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.graphics.GraphicsDao;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Paragraph;
import com.joliciel.jochre.graphics.RowOfShapes;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.lang.Linguistics;
import com.joliciel.jochre.security.SecurityDao;
import com.joliciel.jochre.security.User;
import com.joliciel.jochre.utils.JochreException;
import com.joliciel.talismane.utils.PersistentList;
import com.joliciel.talismane.utils.PersistentListImpl;

/**
 * One document, generally equivalent to a PDF file with multiple pages, each
 * containing one or more images.
 * 
 * @author Assaf Urieli
 *
 */
public class JochreDocument implements Entity {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(JochreDocument.class);
	private DocumentServiceInternal documentServiceInternal;

	private int id;

	private String fileName = "";
	private String name = "";
	private String fileBase = null;
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
	private Map<String, String> fields = new TreeMap<String, String>();

	private static Pattern diacriticPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

	private final JochreSession jochreSession;
	private final GraphicsDao graphicsDao;

	public JochreDocument(JochreSession jochreSession) {
		this.jochreSession = jochreSession;
		this.graphicsDao = GraphicsDao.getInstance(jochreSession);
	}

	/**
	 * The pages contained in this document.
	 */
	public List<JochrePage> getPages() {
		if (this.pages == null) {
			if (this.id == 0)
				this.pages = new ArrayList<JochrePage>();
			else
				this.pages = this.documentServiceInternal.findPages(this);
		}
		return pages;
	}

	public JochrePage newPage() {
		JochrePage page = new JochrePage(jochreSession);
		page.setDocument(this);
		this.getPages().add(page);
		page.setIndex(this.getPages().size());
		return page;
	}

	public JochrePage getCurrentPage() {
		if (this.getPages().size() == 0)
			return null;
		return this.getPages().get(this.getPages().size() - 1);
	}

	@Override
	public void save() {
		this.documentServiceInternal.saveJochreDocument(this);
		if (this.pages != null) {
			for (JochrePage page : this.pages) {
				page.save();
			}
		}
		if (this.authors != null) {
			if (this.authors.isDirty()) {
				this.documentServiceInternal.replaceAuthors(this);
			}
		}
	}

	/**
	 * The filename of this document.
	 */
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * A more human-friendly name for this document.
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * The document's locale.
	 */
	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	/**
	 * Returns an xml representation of this document as it currently stands, to
	 * be used for correcting the text associated with this document.
	 */
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
									if (shape.getLetter() != null)
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
		} catch (XMLStreamException e) {
			throw new JochreException(e);
		}

	}

	/**
	 * For a document containing pages which contain raw images, segments these
	 * images by converting them to JochreImages.
	 */
	public void segment() {
		for (JochrePage page : this.getPages())
			page.segment();
	}

	/**
	 * Segment the document and output the segmentation into PNG files so that
	 * they can be viewed by the user.
	 */
	public void segmentAndShow(String outputDirectory) {
		File outputPath = new File(outputDirectory);
		if (outputPath.exists() == false)
			outputPath.mkdirs();

		for (JochrePage page : this.getPages())
			page.segmentAndShow(outputDirectory);
	}

	/**
	 * Is this document's locale left-to-right or right-to-left (ignoring
	 * top-to-bottom for now!)
	 */
	public boolean isLeftToRight() {
		Linguistics linguistics = jochreSession.getLinguistics();
		return linguistics.isLeftToRight();
	}

	/**
	 * All images in this document, ordered by page index and image index.
	 */
	public List<JochreImage> getImages() {
		if (this.images == null) {
			images = new ArrayList<JochreImage>();
			for (JochrePage page : this.getPages()) {
				for (JochreImage image : page.getImages()) {
					images.add(image);
				}
			}
		}
		return images;
	}

	public int getOwnerId() {
		return ownerId;
	}

	void setOwnerId(int ownerId) {
		this.ownerId = ownerId;
		this.owner = null;
	}

	/**
	 * The User who uploaded this document in the first place.
	 */
	public User getOwner() {
		if (this.owner == null && this.ownerId != 0) {
			SecurityDao securityDao = SecurityDao.getInstance(jochreSession);
			this.owner = securityDao.loadUser(this.ownerId);
		}
		return owner;
	}

	public void setOwner(User owner) {
		this.setOwnerId(owner.getId());
		this.owner = owner;
	}

	public String getNameLocal() {
		return nameLocal;
	}

	public void setNameLocal(String nameLocal) {
		this.nameLocal = nameLocal;
	}

	public String getPublisher() {
		return publisher;
	}

	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	/**
	 * The authors for this document.
	 */
	public List<Author> getAuthors() {
		if (this.authors == null) {
			this.authors = new PersistentListImpl<Author>();
			if (this.id != 0) {
				this.authors.addAllFromDB(this.getDocumentServiceInternal().findAuthors(this));
			}
		}
		return authors;
	}

	public DocumentServiceInternal getDocumentServiceInternal() {
		return documentServiceInternal;
	}

	public void setDocumentServiceInternal(DocumentServiceInternal documentServiceInternal) {
		this.documentServiceInternal = documentServiceInternal;
	}

	@Override
	public int hashCode() {
		if (this.id == 0)
			return super.hashCode();
		else
			return this.id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this.id == 0) {
			return super.equals(obj);
		} else {
			if (obj == null)
				return false;
			JochreDocument other = (JochreDocument) obj;
			return (this.getId() == other.getId());
		}
	}

	/**
	 * Delete an image from this document. If it is the last image on the page,
	 * delete the page containing the image as well.
	 */
	public void deleteImage(JochreImage image) {
		if (!image.getPage().getDocument().equals(this)) {
			throw new RuntimeException("Cannot delete image from document - image is on another document");
		}
		JochrePage page = image.getPage();
		boolean lastImageOnPage = (page.getImages().size() == 1);
		this.graphicsDao.deleteJochreImage(image);
		if (lastImageOnPage)
			this.getDocumentServiceInternal().deleteJochrePage(page);
		this.getPages().remove(page);
	}

	public void deletePage(JochrePage page) {
		if (!page.getDocument().equals(this)) {
			throw new RuntimeException("Cannot delete page from document - page is on another document");
		}

		for (JochreImage image : page.getImages()) {
			this.graphicsDao.deleteJochreImage(image);
		}

		this.getDocumentServiceInternal().deleteJochrePage(page);
		this.getPages().remove(page);
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public int getTotalPageCount() {
		return totalPageCount;
	}

	public void setTotalPageCount(int totalPageCount) {
		this.totalPageCount = totalPageCount;
	}

	/**
	 * Any fields associated with this document.
	 */
	public Map<String, String> getFields() {
		return fields;
	}

	/**
	 * Get a file base useful for saving files based on this document.
	 */
	public String getFileBase() {
		if (fileBase == null) {
			fileBase = diacriticPattern.matcher(Normalizer.normalize(name, Form.NFD)).replaceAll("");
			fileBase = fileBase.replaceAll("[^A-Za-z0-9\\_-]+", "");
		}
		return fileBase;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

}
