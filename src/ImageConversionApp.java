import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ImageConversionApp {
    public static final int COLORS_COUNT_IN_RGB = 3;

    public static final Map<Double, int[][]> BLUR_MATRIX = new HashMap<Double, int[][]>() {{
        put(9.0, new int[][]{{1, 1, 1}, {1, 1, 1}, {1, 1, 1}});
    }};

    public static final Map<Double, int[][]> SHARP_MATRIX = new HashMap<Double, int[][]>() {{
        put(4.0, new int[][]{{0, -1, 0}, {-1, 8, -1}, {0, -1, 0}});
    }};

    public static void main(String[] args) {
        File file = new File("image.jpg");
        BufferedImage source = null;

        try {
            source = ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create new images of the same size
        BufferedImage blurImage = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        WritableRaster blurRaster = blurImage.getRaster();

        BufferedImage sharpImage = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        WritableRaster sharpRaster = sharpImage.getRaster();

        WritableRaster raster = source.getRaster();

        int newWidth = source.getWidth() - 1;
        int newHeight = source.getHeight() - 1;

        for (int x = 1; x < newWidth; x++) {
            for (int y = 1; y < newHeight; y++) {

                int[] colors = new int[COLORS_COUNT_IN_RGB];

                raster.getPixel(x, y, colors);

                Pixel currentPixel = new Pixel(x, y);

                blurRaster.setPixel(x, y, getModifiedColors(raster, currentPixel, (HashMap<Double, int[][]>) BLUR_MATRIX));
                sharpRaster.setPixel(x, y, getModifiedColors(raster, currentPixel, (HashMap<Double, int[][]>) SHARP_MATRIX));
            }
        }

        File output1 = new File("image_blur.jpg");
        File output2 = new File("image_sharp.jpg");

        try {
            ImageIO.write(blurImage, "jpg", output1);
            ImageIO.write(sharpImage, "jpg", output2);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int[] getModifiedColors(WritableRaster raster, Pixel currentPixel, HashMap<Double, int[][]> convertMatrix) {

        Map.Entry<Double, int[][]> entry = convertMatrix.entrySet().stream().findFirst().get();

        double divisor = entry.getKey();

        int[][] matrix = entry.getValue();

        int[] newColors = new int[COLORS_COUNT_IN_RGB];

        int[] colors = new int[COLORS_COUNT_IN_RGB];

        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {

                raster.getPixel(currentPixel.getX() + i, currentPixel.getY() + j, colors);

                for (int k = 0; k < COLORS_COUNT_IN_RGB; k++) {
                    newColors[k] = newColors[k] + colors[k] * matrix[i + 1][j + 1];
                }
            }
        }

        for (int k = 0; k < COLORS_COUNT_IN_RGB; k++) {
            newColors[k] = (int) (newColors[k] / divisor);
        }

        return newColors;
    }
}