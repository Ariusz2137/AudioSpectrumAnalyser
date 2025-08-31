package audioSpectrumAnalyser;

import static audioSpectrumAnalyser.SgFilters.*;
import static java.lang.String.format;

public class SgFilter {

  /* number of points in the window */
  public int windowSize;
  public int derivative;
  /* order of the polynomial used in the fit */
  public int polynomial;
  public float[][] weights;

  public SgFilter(int windowSize, int derivative, int polynomial) {
    this.windowSize = windowSize;
    this.derivative = derivative;
    this.polynomial = polynomial;

    if (windowSize % 2 == 0 || windowSize < 5) {
      throw new IllegalArgumentException(format(
        "options.WindowSize [%d] must be odd and equal to or greater than 5", windowSize
      ));
    }
    if (derivative < 0) {
      throw new IllegalArgumentException(format(
        "options.Derivative [%d] must be equal or greater than 0", derivative
      ));
    }
    if (polynomial < 0) {
      throw new IllegalArgumentException(format(
        "options.Polynomial [%d] must be equal or greater than 0", polynomial
      ));
    }
    this.weights = computeWeights(windowSize, polynomial, derivative);
  }

  public SgFilter(int windowSize) {
    this(windowSize, 0, 3);
  }

  public float getHs(float[] h, int center, int half, int derivative) {
    float hs = 0;
    int count = 0;
    for (int i = center - half; i < center + half; i++) {
      if (i >= 0 && i < h.length - 1) {
        hs += h[i + 1] - h[i];
        count++;
      }
    }
    return (float) Math.pow(hs / count, derivative);
  }

  public void process(float[] data, float[] h, float[] out) {
    if (windowSize > data.length) {
      throw new IllegalArgumentException(format(
        "data length [%d] must be larger than options.WindowSize[%d]",
        data.length, windowSize
      ));
    }

    if (data.length != out.length) {
      throw new IllegalArgumentException(format(
        "data/output size mismatch [%d, %d]", data.length, out.length
      ));
    }

    int halfWindow = (int) Math.floor(windowSize / 2.0);
    int numPoints = data.length;
    float hs;

    //For the borders
    for (int i = 0; i < halfWindow; i++) {
      float[] wg1 = weights[halfWindow - i - 1];
      float[] wg2 = weights[halfWindow + i + 1];
      float d1 = 0;
      float d2 = 0;
      for (int l = 0; l < windowSize; l++) {
        d1 += wg1[l] * data[l];
        d2 += wg2[l] * data[numPoints - windowSize + l];
      }
      hs = getHs(h, halfWindow - i - 1, halfWindow, derivative);
      out[halfWindow - i - 1] = d1 / hs;
      hs = getHs(h, numPoints - halfWindow + i, halfWindow, derivative);
      out[numPoints-halfWindow+i] = d2 / hs;
    }

    //For the internal points
    var wg = weights[halfWindow];
    for (int i = windowSize; i <= numPoints; i++) {
      float d = 0;
      for (int l = 0; l < windowSize; l++) {
        d += wg[l] * data[l + i - windowSize];
      }
      hs = getHs(h, i - halfWindow - 1, halfWindow, derivative);
      out[i - halfWindow - 1] = d / hs;
    }
  }

}
