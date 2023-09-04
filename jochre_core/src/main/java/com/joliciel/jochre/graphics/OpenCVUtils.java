package com.joliciel.jochre.graphics;

import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;

import org.bytedeco.opencv.global.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc.*;
import org.bytedeco.opencv.opencv_core.*;

import java.awt.image.BufferedImage;

public interface OpenCVUtils {
  OpenCVFrameConverter<Mat> cvConverter = new OpenCVFrameConverter.ToMat();
  Java2DFrameConverter bufferedImageConverter = new Java2DFrameConverter();

  static Mat fromBufferedImage(BufferedImage bufferedImage) {
    return cvConverter.convertToMat(bufferedImageConverter.convert(bufferedImage));
  }

  static BufferedImage toBufferedImage(Mat mat) {
    return bufferedImageConverter.convert(cvConverter.convert(mat));
  }

  static Mat equalizeMat(Mat source) {
    Mat dest = new Mat();
    opencv_imgproc.equalizeHist(source, dest);
    return dest;
  }

  static BufferedImage equalizeImage(BufferedImage source) {
    Mat dest = equalizeMat(fromBufferedImage(source));
    return toBufferedImage(dest);
  }
}
