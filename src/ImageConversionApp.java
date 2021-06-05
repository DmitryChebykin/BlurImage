import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class ImageConversionApp {
    public static final int COLORS_COUNT_IN_RGB = 3;
    public static final int MAX_COLOR_VALUE_IN_RGB = 255;
    public static final int MIN_COLOR_VALUE_IN_RGB = 0;

    public static final int MEDIAN_BLUR_MATRIX_SIZE = 7;

    public static final double[][] BLUR_MATRIX = new double[][]{
            {1d / 9d, 1d / 9d, 1d / 9d},
            {1d / 9d, 1d / 9d, 1d / 9d},
            {1d / 9d, 1d / 9d, 1d / 9d}};

    public static final double[][] GAUSS_BLUR_MATRIX = new double[][]{
            {1d / 256d, 4d / 256d, 6d / 256d, 4d / 256d, 1d / 256d},
            {4d / 256d, 16d / 256d, 24d / 256d, 16d / 256d, 4d / 256d},
            {6d / 256d, 24d / 256d, 36d / 256d, 24d / 256d, 6d / 256d},
            {4d / 256d, 16d / 256d, 24d / 256d, 16d / 256d, 4d / 256d},
            {1d / 256d, 4d / 256d, 6d / 256d, 4d / 256d, 1d / 256d}};

    public static final double[][] SHARP_MATRIX = new double[][]{
            {0, -0.25, 0},
            {-0.25, 2, -0.25},
            {0, -0.25, 0}};

    public static final double[][] OUTLINE_MATRIX = new double[][]{
            {-0.5, -0.5, -0.5},
            {-0.5, 5, -0.5},
            {-0.5, -0.5, -0.5}};

    public static void main(String[] args) {
        File file = new File("image.jpg");
        BufferedImage source;

        try {
            source = ImageIO.read(file);
        } catch (IOException e) {
            System.out.println("Файл не читается.");
            return;
        }

        WritableRaster sourceRaster = source.getRaster();
        ColorModel colorModel = source.getColorModel();

        WritableRaster blurRaster = getConvertedRaster(sourceRaster, BLUR_MATRIX);
        writeRasterToImageFile(blurRaster, colorModel, "image_blur.jpg", "jpg");

        WritableRaster gaussBlurRaster = getConvertedRaster(sourceRaster, GAUSS_BLUR_MATRIX);
        writeRasterToImageFile(gaussBlurRaster, colorModel, "image_gauss_blur.jpg", "jpg");

        WritableRaster sharpRaster = getConvertedRaster(sourceRaster, SHARP_MATRIX);
        writeRasterToImageFile(sharpRaster, colorModel, "image_sharp.jpg", "jpg");

        WritableRaster outlineRaster = getConvertedRaster(sourceRaster, OUTLINE_MATRIX);
        writeRasterToImageFile(outlineRaster, colorModel, "image_outline.jpg", "jpg");

        WritableRaster medianBlurRaster = getConvertedRaster(gaussBlurRaster);
        writeRasterToImageFile(medianBlurRaster, colorModel, "image_median_blur.jpg", "jpg");

        WritableRaster watercolorRaster = getConvertedRaster(medianBlurRaster, OUTLINE_MATRIX);
        writeRasterToImageFile(watercolorRaster, colorModel, "image_watercolor.png", "png");
    }

    private static WritableRaster getConvertedRaster(WritableRaster sourceRaster, double[][] matrix) {
        int rasterHeight = sourceRaster.getHeight();
        int rasterWidth = sourceRaster.getWidth();
        int matrixSize = matrix.length;

        WritableRaster destinationRaster = sourceRaster.createCompatibleWritableRaster();

        if (rasterHeight < matrixSize || rasterWidth < matrixSize) {
            destinationRaster.setDataElements(0, 0, sourceRaster);
            return destinationRaster; // the image does not meet the minimum size for transformations, just copy raster
        }

        int leftIndexMargin = -matrixSize / 2;
        int rightIndexMargin = matrixSize / 2;

        int[] colors = new int[COLORS_COUNT_IN_RGB];

        for (int y = 0; y < rasterHeight; y++) {
            for (int x = 0; x < rasterWidth; x++) {
                double r = 0, g = 0, b = 0;

                for (int i = leftIndexMargin; i <= rightIndexMargin; i++) {
                    for (int j = leftIndexMargin; j <= rightIndexMargin; j++) {
                        int pixelByMatrixCoordinateX = x + j;
                        int pixelByMatrixCoordinateY = y + i;

                        // if the pixel by matrix is outside the image, get the data of pixels located mirrored from the current one
                        if (pixelByMatrixCoordinateX < 0 || pixelByMatrixCoordinateX > rasterWidth - 1) {
                            pixelByMatrixCoordinateX = x - j;
                        }

                        if (pixelByMatrixCoordinateY < 0 || pixelByMatrixCoordinateY > rasterHeight - 1) {
                            pixelByMatrixCoordinateY = y - i;
                        }

                        sourceRaster.getPixel(pixelByMatrixCoordinateX, pixelByMatrixCoordinateY, colors);
                        r = r + colors[0] * matrix[rightIndexMargin + i][rightIndexMargin + j];
                        g = g + colors[1] * matrix[rightIndexMargin + i][rightIndexMargin + j];
                        b = b + colors[2] * matrix[rightIndexMargin + i][rightIndexMargin + j];
                    }
                }

                colors[0] = (int) Math.ceil(getNormalizedColor(r));
                colors[1] = (int) Math.ceil(getNormalizedColor(g));
                colors[2] = (int) Math.ceil(getNormalizedColor(b));

                destinationRaster.setPixel(x, y, colors);
            }
        }

        return destinationRaster;
    }

    private static WritableRaster getConvertedRaster(WritableRaster sourceRaster) {
        int rasterHeight = sourceRaster.getHeight();
        int rasterWidth = sourceRaster.getWidth();

        WritableRaster destinationRaster = sourceRaster.createCompatibleWritableRaster();

        if (rasterHeight < MEDIAN_BLUR_MATRIX_SIZE || rasterWidth < MEDIAN_BLUR_MATRIX_SIZE) {
            destinationRaster.setDataElements(0, 0, sourceRaster);
            return destinationRaster; // the image does not meet the minimum size for transformations, just copy raster
        }

        int leftIndexMargin = -MEDIAN_BLUR_MATRIX_SIZE / 2;
        int rightIndexMargin = MEDIAN_BLUR_MATRIX_SIZE / 2;

        int[] colors = new int[COLORS_COUNT_IN_RGB];

        int[] r = new int[MEDIAN_BLUR_MATRIX_SIZE * MEDIAN_BLUR_MATRIX_SIZE];
        int[] g = new int[MEDIAN_BLUR_MATRIX_SIZE * MEDIAN_BLUR_MATRIX_SIZE];
        int[] b = new int[MEDIAN_BLUR_MATRIX_SIZE * MEDIAN_BLUR_MATRIX_SIZE];

        for (int y = 0; y < rasterHeight; y++) {
            for (int x = 0; x < rasterWidth; x++) {
                int count = 0;

                for (int i = leftIndexMargin; i <= rightIndexMargin; i++) {
                    for (int j = leftIndexMargin; j <= rightIndexMargin; j++) {

                        int pixelByMatrixCoordinateX = x + j;
                        int pixelByMatrixCoordinateY = y + i;

                        // if the matrix pixel is outside the image, get the data of pixels located mirrored from the current one
                        if (pixelByMatrixCoordinateX < 0 || pixelByMatrixCoordinateX > rasterWidth - 1) {
                            pixelByMatrixCoordinateX = x - j;
                        }

                        if (pixelByMatrixCoordinateY < 0 || pixelByMatrixCoordinateY > rasterHeight - 1) {
                            pixelByMatrixCoordinateY = y - i;
                        }

                        sourceRaster.getPixel(pixelByMatrixCoordinateX, pixelByMatrixCoordinateY, colors);
                        r[count] = colors[0];
                        g[count] = colors[1];
                        b[count] = colors[2];

                        count++;
                    }
                }
                final int SORTED_CHANNEL_COLORS_ARRAY_MEDIAN_INDEX = (int) Math.pow(MEDIAN_BLUR_MATRIX_SIZE, 2) / 2 + 1;

                Arrays.sort(r);
                colors[0] = r[SORTED_CHANNEL_COLORS_ARRAY_MEDIAN_INDEX];

                Arrays.sort(g);
                colors[1] = g[SORTED_CHANNEL_COLORS_ARRAY_MEDIAN_INDEX];

                Arrays.sort(b);
                colors[2] = b[SORTED_CHANNEL_COLORS_ARRAY_MEDIAN_INDEX];

                destinationRaster.setPixel(x, y, colors);
            }
        }

        return destinationRaster;
    }

    private static double getNormalizedColor(double value) {
        if (value < MIN_COLOR_VALUE_IN_RGB) {
            return MIN_COLOR_VALUE_IN_RGB;
        }
        if (value > MAX_COLOR_VALUE_IN_RGB) {
            return MAX_COLOR_VALUE_IN_RGB;
        }

        return value;
    }

    private static void writeRasterToImageFile(WritableRaster raster, ColorModel colorModel, String path, String format) {
        try {
            ImageIO.write(new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), null), format, new File(path));
        } catch (IOException e) {
            System.out.println("Файл " + path + " не записываются.");
        }
    }
}