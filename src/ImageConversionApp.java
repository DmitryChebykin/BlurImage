import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class ImageConversionApp {
    public static final int COLORS_COUNT_IN_RGB = 3;
    public static final int MAX_COLOR_VALUE_IN_RGB = 255;
    public static final int MIN_COLOR_VALUE_IN_RGB = 0;
    public static final int WATERCOLOR_MATRIX_MEDIAN_INDEX = 13;
    public static final int WATERCOLOR_MATRIX_SIZE = 5;

    public static final double[][] BLUR_MATRIX = new double[][]{
            {1 / 9.0, 1 / 9.0, 1 / 9.0},
            {1 / 9.0, 1 / 9.0, 1 / 9.0},
            {1 / 9.0, 1 / 9.0, 1 / 9.0}};

    public static final double[][] GAUSS_BLUR_MATRIX = new double[][]{
            {1 / 256.0, 4 / 256.0, 6 / 256.0, 4 / 256.0, 1 / 256.0},
            {4 / 256.0, 16 / 256.0, 24 / 256.0, 16 / 256.0, 4 / 256.0},
            {6 / 256.0, 24 / 256.0, 36 / 256.0, 24 / 256.0, 6 / 256.0},
            {4 / 256.0, 16 / 256.0, 24 / 256.0, 16 / 256.0, 4 / 256.0},
            {1 / 256.0, 4 / 256.0, 6 / 256.0, 4 / 256.0, 1 / 256.0}};

    public static final double[][] SHARP_MATRIX = new double[][]{
            {0, -0.25, 0},
            {-0.25, 2, -0.25},
            {0, -0.25, 0}};

    public static void main(String[] args) {
        File file = new File("image.jpg");
        BufferedImage source;

        try {
            source = ImageIO.read(file);
        } catch (IOException e) {
            System.out.println("Файл не читается.");
            return;
        }

        WritableRaster raster = source.getRaster();

        // Create new images of the same size
        BufferedImage blurImage = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        WritableRaster blurRaster = blurImage.getRaster();
        convertRaster(raster, blurRaster, BLUR_MATRIX);
        File output1 = new File("image_blur.jpg");

        BufferedImage gaussBlurImage = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        WritableRaster gaussBlurRaster = gaussBlurImage.getRaster();
        convertRaster(raster, gaussBlurRaster, GAUSS_BLUR_MATRIX);
        File output2 = new File("image_sharp.jpg");

        BufferedImage sharpImage = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        WritableRaster sharpRaster = sharpImage.getRaster();
        convertRaster(raster, sharpRaster, SHARP_MATRIX);
        File output3 = new File("image_gauss_blur.jpg");

        BufferedImage watercolorImage = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        WritableRaster watercolorRaster = watercolorImage.getRaster();
        convertRaster(gaussBlurRaster, watercolorRaster);
        File output4 = new File("image_watercolor.jpg");

        try {
            ImageIO.write(blurImage, "jpg", output1);
            ImageIO.write(sharpImage, "jpg", output2);
            ImageIO.write(gaussBlurImage, "jpg", output3);
            ImageIO.write(watercolorImage, "jpg", output4);
        } catch (IOException e) {
            System.out.println("Файлы не записываются.");
        }
    }

    private static void convertRaster(WritableRaster sourceRaster, WritableRaster destinationRaster, double[][] matrix) {
        int rasterHeight = sourceRaster.getHeight();
        int rasterWidth = sourceRaster.getWidth();
        int matrixSize = matrix.length;

        if (rasterHeight < matrixSize || rasterWidth < matrixSize) {
            destinationRaster.setDataElements(0, 0, sourceRaster);
            return; // the image does not meet the minimum size for transformations, just copy raster
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

    private static void convertRaster(WritableRaster sourceRaster, WritableRaster destinationRaster) {
        int rasterHeight = sourceRaster.getHeight();
        int rasterWidth = sourceRaster.getWidth();

        if (rasterHeight < WATERCOLOR_MATRIX_SIZE || rasterWidth < WATERCOLOR_MATRIX_SIZE) {
            destinationRaster.setDataElements(0, 0, sourceRaster); // the image does not meet the minimum size for transformations, just copy raster
            return;
        }

        int leftIndexMargin = -WATERCOLOR_MATRIX_SIZE / 2;
        int rightIndexMargin = WATERCOLOR_MATRIX_SIZE / 2;

        int[] colors = new int[COLORS_COUNT_IN_RGB];

        int[] r = new int[WATERCOLOR_MATRIX_SIZE * WATERCOLOR_MATRIX_SIZE];
        int[] g = new int[WATERCOLOR_MATRIX_SIZE * WATERCOLOR_MATRIX_SIZE];
        int[] b = new int[WATERCOLOR_MATRIX_SIZE * WATERCOLOR_MATRIX_SIZE];

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

                Arrays.sort(r);
                colors[0] = r[WATERCOLOR_MATRIX_MEDIAN_INDEX];

                Arrays.sort(g);
                colors[1] = g[WATERCOLOR_MATRIX_MEDIAN_INDEX];

                Arrays.sort(b);
                colors[2] = b[WATERCOLOR_MATRIX_MEDIAN_INDEX];

                destinationRaster.setPixel(x, y, colors);
            }
        }
    }
}