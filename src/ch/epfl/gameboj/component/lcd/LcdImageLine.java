package ch.epfl.gameboj.component.lcd;

import java.util.Objects;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.bits.Bits;

public final class LcdImageLine {

	private final static int SAME_COLORS = 0b11100100;
	private final BitVector ZERO_VALUE_BITVECTOR;
	private final BitVector msb, lsb, opacity;
	

	/**
	 * Construit une ligne de bitVector
	 * 
	 * @param msb
	 *            : BitVector de msb de la ligne à construire. Doit être non null
	 * @param lsb
	 *            : BitVector de lsb de la ligne à construire. Doit être non null
	 * @param opacity
	 *            : BitVector d'opacité de la ligne à construire. Doit être non null
	 * 
	 * @throws IllegalArgumentException
	 *             si msb, lsb et opacity n'ont pas la même taille
	 */
	public LcdImageLine(BitVector msb, BitVector lsb, BitVector opacity) {
		Objects.requireNonNull(msb);
		Objects.requireNonNull(lsb);
		Objects.requireNonNull(opacity);
		Preconditions.checkArgument(msb.size() == lsb.size() && msb.size() == opacity.size());
		this.msb = msb;
		this.lsb = lsb;
		this.opacity = opacity;
		ZERO_VALUE_BITVECTOR = new BitVector(size(), false);
	}

	/**
	 * @return la longueur, en pixels, de la ligne LCD
	 */
	public int size() {
		return msb.size();
	}

	/**
	 * @return le vecteur des bits de poids fort de la ligne LCD
	 */
	public BitVector msb() {
		return msb;
	}

	/**
	 * @return le vecteur des bits de poids faible de la ligne LCD
	 */
	public BitVector lsb() {
		return lsb;
	}

	/**
	 * @return le vecteur des bits de l'opacité de la ligne LCD
	 */
	public BitVector opacity() {
		return opacity;
	}

	/**
	 * décaler la ligne d'un nombre de pixels donné, en préservant sa longueur
	 * 
	 * @param size
	 *            : nombre de bits de décalage
	 * @return une nouvelle instance de LcdImageLine correspondant au décallage
	 */
	public LcdImageLine shift(int size) {
		return new LcdImageLine(msb.shift(size), lsb.shift(size), opacity.shift(size));
	}

	/**
	 * Retourne l'extraction de l'extension infinie par enroulement, à partir d'un
	 * pixel donné, de la ligne de longueur donnée
	 * 
	 * @param pixel
	 *            : pixel à partir duquel on va commencer à extraire l'extension
	 *            infinie
	 * @param size
	 *            : taille de l'extraction par enroulement
	 * @return une nouvelle instance de LcdImageLine correspondant à l'extension
	 *         infinie par enroulement
	 */
	public LcdImageLine extractWrapped(int pixel, int size) {
		Preconditions.checkArgument(size > 0);
		return new LcdImageLine(msb.extractWrapped(pixel, size), lsb.extractWrapped(pixel, size),
				opacity.extractWrapped(pixel, size));
	}

	/**
	 * Retounrne l'image LCD dont les couleurs ont été transformées en fonction
	 * d'une palette, donnée sous la forme d'un octet encodé
	 * 
	 * @param palette
	 *            : octet correspondant à la table de transition des couleurs
	 *            suivant le format descrit en cours.
	 * @return une nouvelle instance de LcdImageLine correspondant à la
	 *         tranfromation de couleurs de l'instance courante.
	 */
	public LcdImageLine mapColors(int palette) {
		Preconditions.checkBits8(palette);

		if (palette == SAME_COLORS) {
			return this;
		}
		LcdImageLine color3 = getLineOfColor(Bits.extract(palette, 6, 2), msb.and(lsb));
		LcdImageLine color2 = getLineOfColor(Bits.extract(palette, 4, 2), msb.and(lsb.not()));
		LcdImageLine color1 = getLineOfColor(Bits.extract(palette, 2, 2), msb.not().and(lsb));
		LcdImageLine color0 = getLineOfColor(Bits.clip(2, palette), msb.or(lsb).not());
		BitVector newMsb = color3.msb.or(color2.msb).or(color1.msb).or(color0.msb);
		BitVector newLsb = color3.lsb.or(color2.lsb).or(color1.lsb).or(color0.lsb);

		return new LcdImageLine(newMsb, newLsb, opacity);
	}

	/**
	 * @param newColor
	 *            : nouvelle couleur donnée par la palette
	 * @param indexesOfColor
	 *            : BitVector ayant des 1 à tous les index qui contiennent une
	 *            certaine couleur (0,1,2 ou 3) et 0 sinon. Par exemple msb.and(lsb)
	 *            est un vecteur de bits de ce type. Il est associé à la couleur 3.
	 *            Il signifie que à chaque index de bit valant 1 on retrouve la
	 *            couleur 3 dans this.
	 * @return une ligne ayant comme valeur 0 (pour msb ou lsb) si la palette
	 *         transforme la couleur en 0 ou sinon à indexesOf Color comme valeur
	 */
	private LcdImageLine getLineOfColor(int newColor, BitVector indexesOfColor) {
		BitVector msb = null;
		BitVector lsb = null;
		switch (newColor) {
		case 0: {
			msb = ZERO_VALUE_BITVECTOR;
			lsb = ZERO_VALUE_BITVECTOR;
		}
			break;
		case 1: {
			msb = ZERO_VALUE_BITVECTOR;
			lsb = indexesOfColor;
		}
			break;
		case 2: {
			msb = indexesOfColor;
			lsb = ZERO_VALUE_BITVECTOR;

		}
			break;
		case 3: {
			msb = indexesOfColor;
			lsb = indexesOfColor;

		}
			break;
		}

		return new LcdImageLine(msb, lsb, opacity);
	}

	/**
	 * Compose la ligne courante avec la seconde de même longueur, placée au-dessus
	 * d'elle, en utilisant l'opacité de la ligne supérieure pour effectuer la
	 * composition
	 * 
	 * @param that
	 *            : ligne que l'on va composer avec l'instance courante
	 * @return : la ligne composée avec l'instance courante
	 * 
	 */

	public LcdImageLine below(LcdImageLine that) {
		return below(that.opacity(), that);
	}

	/**
	 * Compose la ligne courante avec la seconde de même longueur, placée au-dessus
	 * d'elle, en utilisant un vecteur d'opacité passé en argument pour effectuer la
	 * composition, celui de la ligne supérieure étant ignoré
	 * 
	 * @param newOpacity
	 *            : vecteur de bit correspondant à l'opacité de la nouvelle ligne
	 *            composée
	 * @param that
	 *            : ligne que l'on va composer avec l'instance courante
	 * @return : la ligne composée avec l'instance courante
	 * 
	 * @throws IllegalArgumentException
	 *             si newOpacity ou that n'a pas la même taille que this
	 */

	public LcdImageLine below(BitVector newOpacity, LcdImageLine that) {
		Objects.requireNonNull(newOpacity);
		Objects.requireNonNull(that);
		Preconditions.checkArgument(that.size() == this.size() && newOpacity.size() == this.size());
		BitVector newMsb = (that.msb.and(newOpacity)).or(msb.and(newOpacity.not()));
		BitVector newLsb = (that.lsb.and(newOpacity)).or(lsb.and(newOpacity.not()));

		return new LcdImageLine(newMsb, newLsb, newOpacity.or(opacity));
	}

	/**
	 * Return la jointure avec la ligne passée en argument, à partir d'un pixel
	 * d'index
	 * 
	 * @param index
	 *            : index à partir duquel on va joindre les lignes
	 * @param that
	 *            : ligne que l'on va joindre avec l'instance courrante
	 * @return : la jointure de la ligne avec l'instance courrante
	 * 
	 * @throws IllegalArgumentException
	 *             si size n'a pas la même taille que that
	 * @throws IndexOutOfBoundsException
	 *             si l'index n'est pas valide
	 */
	public LcdImageLine join(int index, LcdImageLine that) {
		Objects.requireNonNull(that);
		Objects.checkIndex(index, size());
		Preconditions.checkArgument(size() == that.size());

		BitVector maskLeft = new BitVector(size(), true).shift(index);
		BitVector maskRight = maskLeft.not();
		return new LcdImageLine(that.msb.and(maskLeft).or(msb.and(maskRight)),
				that.lsb.and(maskLeft).or(lsb.and(maskRight)), that.opacity.and(maskLeft).or(opacity.and(maskRight)));
	}

	@Override
	public boolean equals(Object that) {
		return (that instanceof LcdImageLine) && (msb.equals(((LcdImageLine) that).msb))
				&& (lsb.equals(((LcdImageLine) that).lsb)) && (opacity.equals(((LcdImageLine) that).opacity));
	}

	@Override
	public int hashCode() {
		return Objects.hash(msb, lsb, opacity);
	}

	public static final class Builder {

		private final BitVector.Builder msbBuilder;
		private final BitVector.Builder lsbBuilder;
		private boolean built = false;

		/**
		 * Construit le builder
		 * 
		 * @param size
		 *            : nombre de pixels de la future image
		 * 
		 * @throws IllegalArgumentException
		 *             si size ne vérifie pas : size > 0 && (size % 32) == 0
		 */
		public Builder(int size) {
			Preconditions.checkArgument(size > 0 && (size % 32) == 0);
			msbBuilder = new BitVector.Builder(size);
			lsbBuilder = new BitVector.Builder(size);
		}

		/**
		 * Définit la valeur des octets de poids fort et de poids faible de la ligne, à
		 * un index donné
		 * 
		 * @param index
		 *            : index du byte dont on va assigner la valeur newValue. Doit être
		 *            non négatif
		 * @param valueMSB
		 *            : valeur de l'octet MSB à définir. Doit être une valeur 8 bits
		 * @param valueLSB
		 *            : valeur de l'octet LSB à définir. Doit être une valeur 8 bits
		 * @return le builder lui même;
		 */
		public Builder setBytes(int index, int valueMSB, int valueLSB) {
			if (built)
				throw new IllegalStateException();
			msbBuilder.setByte(index, valueMSB);
			lsbBuilder.setByte(index, valueLSB);
			return this;

		}

		/**
		 * @return la ligne LCD correspondante et construite
		 */
		public LcdImageLine build() {
			if (built)
				throw new IllegalStateException();
			
			built = true;
			BitVector msb = msbBuilder.build();
			BitVector lsb = lsbBuilder.build();
			BitVector opacity = msb.or(lsb);
			return new LcdImageLine(msb, lsb, opacity);
		}

	}

}
