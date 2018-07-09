package com.oceanai.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;

public class ImageUtils {

	/**
	 * Converts an image to byte buffer representing PNG (bytes as they would exist on disk)
	 * @param image
	 * @param encoding the encoding to be used, one of: png, jpeg, bmp, wbmp, gif
	 * @return byte[] representing the image
	 * @throws IOException if the bytes[] could not be written
	 */
	public static byte[] imageToBytes(BufferedImage image, String encoding) throws IOException{
		if(image == null || encoding == null)
			return null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, encoding, baos);
		return baos.toByteArray();
	}
	
	/**
	 * Converts the provided byte buffer into an BufferedImage
	 * @param buf byte[] of an image as it would exist on disk
	 * @return
	 * @throws IOException
	 */
	public static BufferedImage bytesToImage(byte[] buf) throws IOException{
		ByteArrayInputStream bais = new ByteArrayInputStream(buf);
		return ImageIO.read(bais);
	}

	/**
	 *
	 * @param pixels
	 * @param width
	 * @param height
     * @return
	 *
	 * @author WangRupeng
     */
	public static BufferedImage getImageFromArray(byte[] pixels, int width, int height) {
		if(pixels == null || width <= 0 || height <= 0)
			return null;
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		byte[] array = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		//int n=  array.length;
		//int m = pixels.length;
		//System.out.println(array.length);
		System.arraycopy(pixels, 0, array, 0, array.length);
		return image;
	}

	public static boolean saveToFile(BufferedImage bufferedImage, String path,String fileName,String imageType) throws IOException {
		if (bufferedImage == null || path == null) {
			return false;
		}
		if (!path.endsWith("/")) {
			path += "/";
		}
		File file = new File(path + fileName + "." + imageType);
		ImageIO.write(bufferedImage, imageType, file);
		return true;
	}

	/**
	 * Check if the image is fail safe for color based features that are actually using 8 bits per pixel RGB.
	 *
	 * @param bufferedImage
	 * @return
	 */
	public static BufferedImage get8BitRGBImage(BufferedImage bufferedImage) {
		// check if it's (i) RGB and (ii) 8 bits per pixel.
		if (bufferedImage.getType() != BufferedImage.TYPE_INT_RGB || bufferedImage.getSampleModel().getSampleSize(0) != 8) {
			BufferedImage img = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
			img.getGraphics().drawImage(bufferedImage, 0, 0, null);
			bufferedImage = img;
		}
		return bufferedImage;
	}
}
