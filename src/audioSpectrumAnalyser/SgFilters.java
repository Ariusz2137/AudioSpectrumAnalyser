package audioSpectrumAnalyser;

public class SgFilters {

  public static float gramPolynomial(int i, int m, int k, int s) {
    float result;
    if (k > 0) {
      float t0 = 4 * k - 2;
      float t1 = k * (2 * m - k + 1);
      float t2 = t0 / t1;

      float t3 = i * gramPolynomial(i, m, k - 1, s);
      float t4 = s * gramPolynomial(i, m, k - 1, s - 1);

      float t5 = (k - 1) * (2 * m + k);
      float t6 = k * (2 * m - k + 1);
      float t7 = t5 / t6;

      float t8 = gramPolynomial(i, m, k-2, s);

      result = t2 * (t3 + t4) - t7 * t8;
    } else {
      if (k == 0 && s == 0) {
        result = 1;
      } else {
        result = 0;
      }
    }
    return result;
  }

  public static int productOfRange(int a, int b) {
    int gf = 1;
    if (a >= b) {
      for (int j = a - b + 1; j <= a; j++) {
        gf *= j;
      }
    }
    return gf;
  }

  public static float polyWeight(int i, int t, int windowMiddle, int polynomial, int derivative) {
    float sum = 0;
    for (int k = 0; k <= polynomial; k++) {
      float t0 = 2 * k + 1;
      float t1 = productOfRange(2 * windowMiddle, k);
      float t2 = productOfRange(2 * windowMiddle + k + 1, k + 1);
      float t3 = gramPolynomial(i, windowMiddle, k, 0);
      float t4 = gramPolynomial(t, windowMiddle, k, derivative);
      sum += t0 * (t1 / t2) * t3 * t4;
    }
    return sum;
  }

  public static float[][] computeWeights(int windowSize, int polynomial, int derivative) {
    float[][] weights = new float[windowSize][];
    int windowMiddle = (int) Math.floor(windowSize / 2.0);

    for (int row = -windowMiddle; row <= windowMiddle; row++) {
      weights[row + windowMiddle] = new float[windowSize];
      for (int col = -windowMiddle; col <= windowMiddle; col++) {
        weights[row + windowMiddle][col + windowMiddle] = polyWeight(col, row, windowMiddle, polynomial, derivative);
      }
    }

    return weights;
  }

}
