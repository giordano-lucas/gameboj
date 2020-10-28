
package ch.epfl.gameboj.bits;

import ch.epfl.gameboj.Preconditions;

import java.util.Objects;

public final class Bits {

	private static final int[] reverse8 = new int[] { 0x00, 0x80, 0x40, 0xC0, 0x20, 0xA0, 0x60, 0xE0, 0x10, 0x90, 0x50,
			0xD0, 0x30, 0xB0, 0x70, 0xF0, 0x08, 0x88, 0x48, 0xC8, 0x28, 0xA8, 0x68, 0xE8, 0x18, 0x98, 0x58, 0xD8, 0x38,
			0xB8, 0x78, 0xF8, 0x04, 0x84, 0x44, 0xC4, 0x24, 0xA4, 0x64, 0xE4, 0x14, 0x94, 0x54, 0xD4, 0x34, 0xB4, 0x74,
			0xF4, 0x0C, 0x8C, 0x4C, 0xCC, 0x2C, 0xAC, 0x6C, 0xEC, 0x1C, 0x9C, 0x5C, 0xDC, 0x3C, 0xBC, 0x7C, 0xFC, 0x02,
			0x82, 0x42, 0xC2, 0x22, 0xA2, 0x62, 0xE2, 0x12, 0x92, 0x52, 0xD2, 0x32, 0xB2, 0x72, 0xF2, 0x0A, 0x8A, 0x4A,
			0xCA, 0x2A, 0xAA, 0x6A, 0xEA, 0x1A, 0x9A, 0x5A, 0xDA, 0x3A, 0xBA, 0x7A, 0xFA, 0x06, 0x86, 0x46, 0xC6, 0x26,
			0xA6, 0x66, 0xE6, 0x16, 0x96, 0x56, 0xD6, 0x36, 0xB6, 0x76, 0xF6, 0x0E, 0x8E, 0x4E, 0xCE, 0x2E, 0xAE, 0x6E,
			0xEE, 0x1E, 0x9E, 0x5E, 0xDE, 0x3E, 0xBE, 0x7E, 0xFE, 0x01, 0x81, 0x41, 0xC1, 0x21, 0xA1, 0x61, 0xE1, 0x11,
			0x91, 0x51, 0xD1, 0x31, 0xB1, 0x71, 0xF1, 0x09, 0x89, 0x49, 0xC9, 0x29, 0xA9, 0x69, 0xE9, 0x19, 0x99, 0x59,
			0xD9, 0x39, 0xB9, 0x79, 0xF9, 0x05, 0x85, 0x45, 0xC5, 0x25, 0xA5, 0x65, 0xE5, 0x15, 0x95, 0x55, 0xD5, 0x35,
			0xB5, 0x75, 0xF5, 0x0D, 0x8D, 0x4D, 0xCD, 0x2D, 0xAD, 0x6D, 0xED, 0x1D, 0x9D, 0x5D, 0xDD, 0x3D, 0xBD, 0x7D,
			0xFD, 0x03, 0x83, 0x43, 0xC3, 0x23, 0xA3, 0x63, 0xE3, 0x13, 0x93, 0x53, 0xD3, 0x33, 0xB3, 0x73, 0xF3, 0x0B,
			0x8B, 0x4B, 0xCB, 0x2B, 0xAB, 0x6B, 0xEB, 0x1B, 0x9B, 0x5B, 0xDB, 0x3B, 0xBB, 0x7B, 0xFB, 0x07, 0x87, 0x47,
			0xC7, 0x27, 0xA7, 0x67, 0xE7, 0x17, 0x97, 0x57, 0xD7, 0x37, 0xB7, 0x77, 0xF7, 0x0F, 0x8F, 0x4F, 0xCF, 0x2F,
			0xAF, 0x6F, 0xEF, 0x1F, 0x9F, 0x5F, 0xDF, 0x3F, 0xBF, 0x7F, 0xFF, };

	/**
	 * constructeur privé afin de rendre la classe non instanciable
	 */
	private Bits() {
	}

	/**
	 * Renvoie le masque correspondant à l'index donné
	 * 
	 * @param index
	 *            : index du masque, doit être compris entre 0 (inclus) et 32
	 *            (exclus)
	 * @return un entier int dont seul le bit d'index donné vaut 1 et lève
	 *         IndexOutOfBoundsException si l'index est invalide
	 */
	public static int mask(int index) {
		return 1 << Objects.checkIndex(index, Integer.SIZE);

	}

	/**
	 * Teste si le bit d'index donné de bits vaut 1
	 * 
	 * @param bits
	 *            : valeur dont on veut tester le bit d'index index
	 * @param index
	 *            : index du bit à tester. Doit être inférieur ou égal à la longueur
	 *            binaire de bits
	 * @return vrai ssi le bit d'index donné de bits vaut 1, ou lève
	 *         IndexOutOfBoundsException si l'index est invalide,
	 */
	public static boolean test(int bits, int index) {
		Objects.checkIndex(index, Integer.SIZE);
		int value = bits >>> index;
		return ((value & 0b1) == 1);

	}

	/**
	 * Teste si le bit d'index déterminé par la position ordinale de l'élément du
	 * type énuméré implémentant bit vaut 1
	 * 
	 * @param bits
	 *            : valeur dont on veut tester le bit bit
	 * @param bit
	 *            : sert à obtenir l'index à tester du bit donné,
	 * @return vrai ssi le bit d'index donné de bits vaut 1, ou lève
	 *         IndexOutOfBoundsException si l'index est invalide,
	 */
	public static boolean test(int bits, Bit bit) {
		return test(bits, bit.index());
	}

	/**
	 * Met à new Value le bit d'index donnée dans bits
	 * 
	 * @param bits
	 *            : entier dont on va modifier la valeur
	 * @param index
	 *            : index du bit à modfier, doit être inférieur ou égal à la
	 *            longueur binaire de bits
	 * @param newValue
	 *            : nouvelle valeur (1 ssi newValue est vrai) du bit d'index index
	 *            de bits
	 * @return une valeur dont tous les bits sont égaux à ceux de bits, sauf celui
	 *         d'index donné, qui est égal à newValue et lève
	 *         IndexOutOfBoundsException si l'index est invalide,
	 */
	public static int set(int bits, int index, boolean newValue) {
		index = Objects.checkIndex(index, Integer.SIZE);

		if (test(bits, index) != newValue) {
			return bits ^ mask(index);
		}
		return bits;
	}

	/**
	 * Retourne une valeur dont les size bits de poids faible sont égaux à ceux de
	 * bits, les autres valant 0. Lève IllegalArgumentException si size est invalide
	 * 
	 * @param size
	 *            : nombre de bits de bits à conserver, doit être compris entre 0
	 *            (inclus) et 32 (inclus !),
	 * @param bits
	 *            : valeur dont veut extraire les size bits de poids faible
	 * @return une valeur dont les size bits de poids faible sont égaux à ceux de
	 *         bits, les autres valant 0. Lève IllegalArgumentException si size est
	 *         invalide
	 */
	public static int clip(int size, int bits) {
		Objects.checkIndex(size, Integer.SIZE+1);

		if (size == 0) {
			return 0;
		}
		return (bits << (Integer.SIZE - size) >>> (Integer.SIZE - size));
	}

	/**
	 * Extrait les size bits de poids faible sont égaux à ceux de bits allant de
	 * l'index start (inclus) à l'index start + size (exclus) ; lève
	 * IndexOutOfBoundsException si start et size ne désignent pas une plage de bits
	 * valide
	 * 
	 * @param bits
	 *            : entier dont on veut extraire les size bits
	 * @param start
	 *            : indice à partir duquel on extrait, doit être positif
	 * @param size
	 *            : nombre de bits à extraire (doit être plus petit que la longueur
	 *            binaire de bits moirs start) et doit être positif
	 * @return une valeur dont les size bits de poids faible sont égaux à ceux de
	 *         bits allant de l'index start (inclus) à l'index start + size
	 *         (exclus) ; lève IndexOutOfBoundsException si start et size ne
	 *         désignent pas une plage de bits valide
	 */
	public static int extract(int bits, int start, int size) {
		Preconditions.checkArgument(start >= 0);
		Objects.checkFromIndexSize(start, size, Integer.SIZE);
		bits = bits >> start;
		return clip(size, bits);

	}

	/**
	 * Retourne une valeur dont les size bits de poids faible sont ceux de bits mais
	 * auxquels une rotation de la distance donnée a été appliquée ; si la distance
	 * est positive, la rotation se fait vers la gauche, sinon elle se fait vers la
	 * droite ; lève IllegalArgumentException si size est invalide ou si la valeur
	 * donnée n'est pas une valeur de size bits,
	 * 
	 * @param size
	 *            : nombre de bits auquels la rotation va être appliquée. Doit être
	 *            compris entre 0 (exclus) et 32 (inclus),
	 * @param bits
	 *            : entier de size bits auquel la rotation va être appliquée.
	 * @param distance
	 *            : entier correspondant au nombre de décallages désirés
	 * @return une valeur dont les size bits de poids faible sont ceux de bits mais
	 *         auxquels une rotation de la distance donnée a été appliquée ; si la
	 *         distance est positive, la rotation se fait vers la gauche, sinon elle
	 *         se fait vers la droite ; lève IllegalArgumentException si size est
	 *         invalide ou si la valeur donnée n'est pas une valeur de size bits,
	 */
	public static int rotate(int size, int bits, int distance) {
		if (size != Integer.SIZE && !(size <= 0 || size > Integer.SIZE)) {
			int newDistance = Math.floorMod(distance, size);
			int d = clip(size, bits);
			d = d << newDistance | d >> (size - newDistance);
			return (bits >>> size << size) | clip(size, d);
		} else if (size == Integer.SIZE) {
			int newDistance = Math.floorMod(distance, size);
			return (bits << newDistance | bits >>> (size - newDistance));
		} else {
			throw new IllegalArgumentException();
		}

	}

	/**
	 * « étend le signe » de la valeur 8 bits donnée, c-à-d copie le bit d'index 7
	 * dans les bits d'index 8 à 31 de la valeur retournée ; lève
	 * IllegalArgumentException si la valeur donnée n'est pas une valeur de 8 bits
	 * 
	 * @param b
	 *            : entier de 8 dont on veut étendre le bit de signe
	 * @return la valeur dont les bit de poids fort à été étendu
	 */
	public static int signExtend8(int b) {
		b = Preconditions.checkBits8(b);
		byte y = (byte) b;
		return (int) y;
	}

	/**
	 * Retourne une valeur égale à celle donnée, si ce n'est que les 8 bits de poids
	 * faible ont été renversés, c-à-d que les bits d'index 0 et 7 ont été échangés,
	 * de même que ceux d'index 1 et 6, 2 et 5, et 3 et 4 ; lève
	 * IllegalArgumentException si la valeur donnée n'est pas une valeur de 8 bits,
	 * 
	 * @param b
	 *            : entier de 8 bits que l'on désire reverser
	 * @return la valeur dont les bits on été renversés
	 */
	public static int reverse8(int b) {
		b = Preconditions.checkBits8(b);
		return reverse8[b];
	}

	/**
	 * Retourne une valeur égale à celle donnée, si ce n'est que les 8 bits de poids
	 * faible ont été inversés bit à bit, c-à-d que les 0 et les 1 ont été
	 * échangés ; lève IllegalArgumentException si la valeur donnée n'est pas une
	 * valeur de 8 bits,
	 * 
	 * @param b
	 *            : entier de 8 bits dont on désire le complément
	 * @return une valeur étant le complément de b
	 */
	public static int complement8(int b) {
		b = Preconditions.checkBits8(b);
		int temp = ~(b | 0b11111111_11111111_11111111_00000000);
		return temp | ((b >>> 8) << 8);

	}

	/**
	 * Retourne une valeur 16 bits dont les 8 bits de poids forts sont les 8 bits de
	 * poids faible de highB, et dont les 8 bits de poids faible sont ceux de lowB ;
	 * 
	 * lève IllegalArgumentException si l'une des deux valeurs données n'est pas une
	 * valeur de 8 bits.
	 * 
	 * @param highB
	 *            : entier de 8 bits correspondant au futurs 8 bits de poids fort
	 * @param lowB
	 *            : entier de 8 bits correspondant au futurs 8 bits de poids faible
	 * @return la concaténation le highB et lowB
	 */
	public static int make16(int highB, int lowB) {
		highB = Preconditions.checkBits8(highB);
		lowB = Preconditions.checkBits8(lowB);
		return (highB << 8) | lowB;
	}
}