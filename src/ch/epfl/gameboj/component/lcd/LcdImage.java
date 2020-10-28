package ch.epfl.gameboj.component.lcd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.BitVector;

public final class LcdImage {

	private final int height;
	private final int width;
	private final List<LcdImageLine> image;

	/**
	 * Construit l'image
	 * 
	 * @param height
	 *            : la hauteur de l'image
	 * @param width
	 *            : la largeur de l'image
	 * @param image
	 *            : liste des lignes de l'image. Doit être non null
	 * 
	 * @throws IllegalArgumentException
	 *             si height ou width n'est pas strictement positif ou si image n'a
	 *             pas la même taille que height
	 */
	public LcdImage(int height, int width, List<LcdImageLine> image) {
		Objects.requireNonNull(image);
		Preconditions.checkArgument(height > 0 && width > 0);
		Preconditions.checkArgument(image.size() == height);

		this.height = height;
		this.width = width;
		this.image = Collections.unmodifiableList(new ArrayList<>(image));
	}

	/**
	 * @return la hauteur de l'image
	 */
	public int height() {
		return height;
	}

	/**
	 * @return la largeur de l'image
	 */
	public int width() {
		return width;
	}

	/**
	 * Retourne sous la forme d'un entier compris entre 0 et 3, la couleur du pixel
	 * d'index (x, y)
	 * 
	 * @param x
	 *            : abscisse du pixel dont on veut déterminer la couleur
	 * @param y
	 *            : ordonnée du pixel dont on veut déterminer la couleur
	 * @return : la couleur du pixel
	 * 
	 * @throws IndexOutOfBoundsException
	 *             si x ou y n'est pas un index valide;
	 */
	public int get(int x, int y) {
		Objects.checkIndex(x, width);
		Objects.checkIndex(y, height);

		LcdImageLine line = image.get(y);
		int color = (line.msb().testBit(x)) ? 2 : 0;
		return (line.lsb().testBit(x)) ? color + 1 : color;
	}

	@Override
	public boolean equals(Object that) {
		return (that instanceof LcdImage) && (image.equals(((LcdImage) that).image));
	}

	@Override
	public int hashCode() {
		return Objects.hash(height, width, image);
	}

	public final static class Builder {

		private final int height;
		private final int width;
		private final List<LcdImageLine> image;
		private boolean built =false;

		/**
		 * Construit le builder
		 * 
		 * @param height
		 *            : la hauteur de l'image
		 * @param width
		 *            : la largeur de l'imag
		 * @throws IllegalArgumentException
		 *             si height ou width n'est pas strictement positif
		 */
		public Builder(int height, int width) {
			Preconditions.checkArgument(height > 0 && width > 0);

			this.height = height;
			this.width = width;
			image = new ArrayList<>();
			for (int i = 0; i < height; ++i)
				image.add(new LcdImageLine.Builder(width).build());
		}

		/**
		 * Modifie la ligne d'index donné
		 * 
		 * @param index
		 *            : index de la ligne à modifier
		 * @param newLine
		 *            : nouvelle ligne à affecter à l'image
		 * @return le builder lui-même
		 * 
		 * @throws IndexOutOfBoundsException
		 *             si l'index n'est pas valide
		 * @throws IllegalStateException
		 *             si l'image a déjà contruite
		 */
		public Builder setLine(int index, LcdImageLine newLine) {
			Objects.requireNonNull(newLine);
			Objects.checkIndex(index, height);
			if (built) {
				throw new IllegalStateException();
			}
			image.set(index, newLine);
			return this;
		}

		/**
		 * @return l'image LCD'construite
		 * @throws IllegalStateException
		 *             si l'image a déjà contruite
		 */
		public LcdImage build() {
			if (built) {
				throw new IllegalStateException();
			}
			built = true;
			return new LcdImage(height, width, image);
		}

	}
}
