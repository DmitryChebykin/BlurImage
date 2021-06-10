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

    public static final double[][] BLUR_MATRIX = {
            {1 / 9d, 1 / 9d, 1 / 9d},
            {1 / 9d, 1 / 9d, 1 / 9d},
            {1 / 9d, 1 / 9d, 1 / 9d}
    };

    public static final double[][] GAUSS_BLUR_MATRIX = {
            {1d / 256, 4d / 256, 6d / 256, 4d / 256, 1d / 256},
            {4d / 256, 16d / 256, 24d / 256, 16d / 256, 4d / 256},
            {6d / 256, 24d / 256, 36d / 256, 24d / 256, 6d / 256},
            {4d / 256, 16d / 256, 24d / 256, 16d / 256, 4d / 256},
            {1d / 256, 4d / 256, 6d / 256, 4d / 256, 1d / 256}
    };

    public static final double[][] SHARP_MATRIX = {
            {0, -0.25, 0},
            {-0.25, 2, -0.25},
            {0, -0.25, 0}
    };

    public static final double[][] OUTLINE_MATRIX = {
            {-0.5, -0.5, -0.5},
            {-0.5, 5, -0.5},
            {-0.5, -0.5, -0.5}
    };

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

        WritableRaster blurRaster = getMatrixConvertedRaster(sourceRaster, BLUR_MATRIX);
        writeRasterToImageFile(blurRaster, colorModel, "image_blur.jpg", "jpg");

        WritableRaster gaussBlurRaster = getMatrixConvertedRaster(sourceRaster, GAUSS_BLUR_MATRIX);
        writeRasterToImageFile(gaussBlurRaster, colorModel, "image_gauss_blur.jpg", "jpg");

        WritableRaster sharpRaster = getMatrixConvertedRaster(sourceRaster, SHARP_MATRIX);
        writeRasterToImageFile(sharpRaster, colorModel, "image_sharp.jpg", "jpg");

        WritableRaster outlineRaster = getMatrixConvertedRaster(sourceRaster, OUTLINE_MATRIX);
        writeRasterToImageFile(outlineRaster, colorModel, "image_outline.jpg", "jpg");

        WritableRaster medianBlurRaster = getMedianBlurRaster(gaussBlurRaster, MEDIAN_BLUR_MATRIX_SIZE);
        writeRasterToImageFile(medianBlurRaster, colorModel, "image_median_blur.jpg", "jpg");

        WritableRaster watercolorRaster = getMatrixConvertedRaster(medianBlurRaster, OUTLINE_MATRIX);
        writeRasterToImageFile(watercolorRaster, colorModel, "image_watercolor.png", "png");
    }

    private static WritableRaster getMatrixConvertedRaster(WritableRaster sourceRaster, double[][] matrix) {
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
                double redColor = 0;
                double greenColor = 0;
                double blurColor = 0;

                for (int i = leftIndexMargin; i <= rightIndexMargin; i++) {
                    for (int j = leftIndexMargin; j <= rightIndexMargin; j++) {
                        int pixelByMatrixCoordinateX = x + j;
                        int pixelByMatrixCoordinateY = y + i;

                        // if the pixel by matrix is outside the image, get the data of pixels located mirrored from the current one
                        if (pixelByMatrixCoordinateX < 0 || pixelByMatrixCoordinateX >= rasterWidth) {
                            pixelByMatrixCoordinateX = x - j;
                        }

                        if (pixelByMatrixCoordinateY < 0 || pixelByMatrixCoordinateY >= rasterHeight) {
                            pixelByMatrixCoordinateY = y - i;
                        }

                        sourceRaster.getPixel(pixelByMatrixCoordinateX, pixelByMatrixCoordinateY, colors);
                        double multiplier = matrix[rightIndexMargin + i][rightIndexMargin + j];

                        redColor += colors[0] * multiplier;
                        greenColor += colors[1] * multiplier;
                        blurColor += colors[2] * multiplier;
                    }
                }

                colors[0] = getNormalizedColor(redColor);
                colors[1] = getNormalizedColor(greenColor);
                colors[2] = getNormalizedColor(blurColor);

                destinationRaster.setPixel(x, y, colors);
            }
        }

        return destinationRaster;
    }

    private static WritableRaster getMedianBlurRaster(WritableRaster sourceRaster, int medianBlurMatrixSize) {
        int rasterHeight = sourceRaster.getHeight();
        int rasterWidth = sourceRaster.getWidth();

        WritableRaster destinationRaster = sourceRaster.createCompatibleWritableRaster();

        if (rasterHeight < medianBlurMatrixSize || rasterWidth < medianBlurMatrixSize) {
            destinationRaster.setDataElements(0, 0, sourceRaster);
            return destinationRaster; // the image does not meet the minimum size for transformations, just copy raster
        }

        int leftIndexMargin = -medianBlurMatrixSize / 2;
        int rightIndexMargin = medianBlurMatrixSize / 2;

        int matrixElementsCount = (int) Math.pow(medianBlurMatrixSize, 2);

        int[] redValues = new int[matrixElementsCount];
        int[] greenValues = new int[matrixElementsCount];
        int[] blueValues = new int[matrixElementsCount];

        int[] colors = new int[COLORS_COUNT_IN_RGB];

        for (int y = 0; y < rasterHeight; y++) {
            for (int x = 0; x < rasterWidth; x++) {
                int count = 0;

                for (int i = leftIndexMargin; i <= rightIndexMargin; i++) {
                    for (int j = leftIndexMargin; j <= rightIndexMargin; j++) {
                        int pixelByMatrixCoordinateX = x + j;
                        int pixelByMatrixCoordinateY = y + i;

                        // if the matrix pixel is outside the image, get the data of pixels located mirrored from the current one
                        if (pixelByMatrixCoordinateX < 0 || pixelByMatrixCoordinateX >= rasterWidth) {
                            pixelByMatrixCoordinateX = x - j;
                        }

                        if (pixelByMatrixCoordinateY < 0 || pixelByMatrixCoordinateY >= rasterHeight) {
                            pixelByMatrixCoordinateY = y - i;
                        }

                        sourceRaster.getPixel(pixelByMatrixCoordinateX, pixelByMatrixCoordinateY, colors);
                        redValues[count] = colors[0];
                        greenValues[count] = colors[1];
                        blueValues[count] = colors[2];

                        count++;
                    }
                }

                final int SORTED_CHANNEL_COLORS_ARRAY_MEDIAN_INDEX = matrixElementsCount / 2 + 1;

                Arrays.sort(redValues);
                colors[0] = redValues[SORTED_CHANNEL_COLORS_ARRAY_MEDIAN_INDEX];

                Arrays.sort(greenValues);
                colors[1] = greenValues[SORTED_CHANNEL_COLORS_ARRAY_MEDIAN_INDEX];

                Arrays.sort(blueValues);
                colors[2] = blueValues[SORTED_CHANNEL_COLORS_ARRAY_MEDIAN_INDEX];

                destinationRaster.setPixel(x, y, colors);
            }
        }

        return destinationRaster;
    }

    private static int getNormalizedColor(double value) {
        if (value < MIN_COLOR_VALUE_IN_RGB) {
            return MIN_COLOR_VALUE_IN_RGB;
        }
        if (value > MAX_COLOR_VALUE_IN_RGB) {
            return MAX_COLOR_VALUE_IN_RGB;
        }

        return (int) Math.ceil(value);
    }

    private static void writeRasterToImageFile(WritableRaster raster, ColorModel colorModel, String path, String format) {
        try {
            ImageIO.write(new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), null), format, new File(path));
        } catch (IOException e) {
            System.out.println("Файл " + path + " не записывается.");
        }
    }
}