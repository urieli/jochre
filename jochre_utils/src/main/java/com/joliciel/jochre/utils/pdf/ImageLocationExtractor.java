package com.joliciel.jochre.utils.pdf;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.contentstream.operator.DrawObject;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.PDFStreamEngine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.contentstream.operator.state.Concatenate;
import org.apache.pdfbox.contentstream.operator.state.Restore;
import org.apache.pdfbox.contentstream.operator.state.Save;
import org.apache.pdfbox.contentstream.operator.state.SetGraphicsStateParameters;
import org.apache.pdfbox.contentstream.operator.state.SetMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an example on how to get the x/y coordinates of image location and size of image.
 */
public class ImageLocationExtractor extends PDFStreamEngine
{
  private static final Logger LOG = LoggerFactory.getLogger(ImageLocationExtractor.class);
  private final List<ImageLocationObserver> observers = new ArrayList<>();
  
  /**
   * @throws IOException If there is an error loading text stripper properties.
   */
  public ImageLocationExtractor() throws IOException
  {
    // preparing PDFStreamEngine
    addOperator(new Concatenate());
    addOperator(new DrawObject());
    addOperator(new SetGraphicsStateParameters());
    addOperator(new Save());
    addOperator(new Restore());
    addOperator(new SetMatrix());
  }

  /**
   * @param operator The operation to perform.
   * @param operands The list of arguments.
   *
   * @throws IOException If there is an error processing the operation.
   */
  @Override
  protected void processOperator(Operator operator, List<COSBase> operands) throws IOException
  {
    String operation = operator.getName();
    if ("Do".equals(operation)) {
      COSName objectName = (COSName) operands.get(0);
      // get the PDF object
      PDXObject xobject = getResources().getXObject(objectName);
      // check if the object is an image object
      if (xobject instanceof PDImageXObject) {
        PDImageXObject image = (PDImageXObject )xobject;
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        LOG.debug("Found image [" + objectName.getName() + "] with type " + image.getSuffix());

        Matrix ctmNew = getGraphicsState().getCurrentTransformationMatrix();
        float scaledWidth = ctmNew.getScalingFactorX();
        float scaledHeight = ctmNew.getScalingFactorY();
        float scaledTop = ctmNew.getTranslateX();
        float scaledLeft = ctmNew.getTranslateY();
        
        // position of image in the pdf in terms of user space units
        LOG.debug("position in PDF = " + scaledTop + ", " + scaledLeft + " in user space units");
        // raw size in pixels
        LOG.debug("raw image size = " + imageWidth + ", " + imageHeight + " in pixels");
        // displayed size in user space units
        LOG.debug("displayed size  = " + scaledWidth + ", " + scaledHeight + " in user space units");
        
        for (ImageLocationObserver observer : observers) {
          observer.onImageFound(image, objectName.getName(), scaledLeft, scaledTop, scaledWidth, scaledHeight);
        }
      } else if (xobject instanceof PDFormXObject) {
        PDFormXObject form = (PDFormXObject) xobject;
        showForm(form);
      }
    } else {
      super.processOperator(operator, operands);
    }
  }

  public void addObserver(ImageLocationObserver observer) {
    this.observers.add(observer);
  }
  
  public interface ImageLocationObserver {
    void onImageFound(PDImageXObject image, String name, float scaledLeft, float scaledTop, float scaledWidth, float scaledHeight);
  }

  /**
   * @throws IOException If there is an error parsing the document.
   */
  public static void main( String[] args ) throws IOException
  {
    String fileName = args[0];
    PDDocument document = PDDocument.load( new File(fileName) );

    try {
      ImageLocationExtractor extractor = new ImageLocationExtractor();
      int pageNum = 0;
      for(PDPage page : document.getPages()) {
        pageNum++;
        LOG.debug("Processing page: " + pageNum);
        LOG.debug("Page rotation: " + page.getRotation());
        extractor.processPage(page);
      }
    } finally {
      document.close();
    }
  }
}