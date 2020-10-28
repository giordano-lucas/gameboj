package ch.epfl.gameboj.gui;

import java.util.Objects;

import ch.epfl.gameboj.component.lcd.LcdImage;
import javafx.scene.image.*;

public final class ImageConverter {

	private static final int COLOR_0 = 0xFFFFFFFF;
	private static final int COLOR_1 = 0xFFD3D3D3;
	private static final int COLOR_2 = 0xFFA9A9A9;
	private static final int COLOR_3 = 0xFF000000;
	private static int[] COLOR_MAP = { COLOR_0, COLOR_1, COLOR_2, COLOR_3 };

	/**
	 * Sert a convertir une image LcdImage en une image javafx
	 * 
	 * @param image
	 *            : image à convertir, non nulle
	 * @return : l'image javafx correspondante
	 */
	public static javafx.scene.image.Image convert(LcdImage image) {
		Objects.requireNonNull(image);

		WritableImage convert = new WritableImage(image.width(), image.height());
		PixelWriter writer = convert.getPixelWriter();

		for (int y = 0; y < image.height(); ++y) {
			for (int x = 0; x < image.width(); ++x) {
				int argb = COLOR_MAP[image.get(x, y)];
				writer.setArgb(x, y, argb);
			}
		}

		return convert;

	}
}
