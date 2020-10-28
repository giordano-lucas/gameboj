package ch.epfl.gameboj.bits;

import java.util.Arrays;
import java.util.Objects;

import static java.lang.Math.*;
import ch.epfl.gameboj.Preconditions;

public final class BitVector {

	private final int[] vector;

	/**
	 * Construit un vecteur de bits de la taille donnée, dont tous les bits ont la
	 * valeur donnée (1 ssi value est vraie)
	 * 
	 * @param size
	 *            : taille du vecteur de bits à construire, doit être un multiple de
	 *            32 non négatif.
	 * @param value
	 *            : valeur de l'entrièreté des bits du vecteur de bits (1 ssi est
	 *            vraie)
	 * @throws IllegalArgumentException
	 *             si size n'est pas positif ou un multiple de 32
	 */
	public BitVector(int size, boolean value) {
		this(newVector(size, value));
	}

	/**
	 * Construit un vecteur de bits de taille size passée en argument et initialise
	 * tous les bits à 0
	 * 
	 * @param size
	 *            : taille du vecteur de bits à construire, doit être un multiple de
	 *            32 non négatif.
	 * @throws IllegalArgumentException
	 *             si size n'est pas positif ou un multiple de 32
	 */
	public BitVector(int size) {
		this(size, false);
	}

	/**
	 * @param arrayVector
	 *            : tableau à partir du quel va être construit le vecteur de bits
	 */
	private BitVector(int[] arrayVector) {
		this.vector = arrayVector;
	}

	/**
	 * Aide les constructeurs de la classe à initialiser les attributs en créant un
	 * talbeau représentant le vecteur de bit (uniquement composé du bit
	 * correspondant à value)
	 * 
	 * @param size
	 *            : taille du vecteur de bits à construire, doit être un multiple de
	 *            32 non négatif.
	 * @param value
	 *            : valeur à affecter à tous les bits du vecteur de bit (1 ssi value
	 *            est vrai):
	 * @return : la talbeau correspondant au vecteur de bits
	 * 
	 * @throws IllegalArgumentException
	 *             si size n'est pas positif ou un multiple de 32
	 */
	private static int[] newVector(int size, boolean value) {
		Preconditions.checkArgument(size > 0 && (size % Integer.SIZE) == 0);
		int v = (value) ? -1 : 0;
		int[] array = new int[size / Integer.SIZE];
		Arrays.fill(array, v);
		return array;
	}

	/**
	 * @return la taille du vecteur, en bits
	 */
	public int size() {
		return vector.length * Integer.SIZE;
	}

	/**
	 * déterminer si le bit d'index donné est vrai ou faux
	 * 
	 * @param index
	 *            : index du bit à tester
	 * @return vrai ssi le bit d'index donné est vrai;
	 * 
	 * @throws IndexOutOfBoundsException
	 *             si l'index est invalide
	 */
	public boolean testBit(int index) {
		Objects.checkIndex(index, vector.length * Integer.SIZE);
		return Bits.test(vector[index / Integer.SIZE], index % Integer.SIZE);
	}

	/**
	 * @return un vecteur de bits correspondant au complément du vecteur de bits de
	 *         l'instance courrante
	 */
	public BitVector not() {
		int[] complement = new int[vector.length];
		for (int i = 0; i < vector.length; ++i) {
			complement[i] = ~vector[i];
		}
		return new BitVector(complement);
	}

	/**
	 * Retourne un vecteur de bits correspondant à la conjonction bit à bit avec le
	 * vecteur de même taille passé en argument
	 * 
	 * @param that
	 *            : vecteur de bits avec lequel on va calculer la conjonction. Doit
	 *            être non null.
	 * @return le vecteur issus de la conjonction entre les deux vecteurs
	 * 
	 * @throws IllegalArgumentException
	 *             si this et that n'ont pas la même taille
	 */
	public BitVector and(BitVector that) {
		Objects.requireNonNull(that);
		Preconditions.checkArgument(size() == that.size());
		int[] and = new int[vector.length];
		for (int i = 0; i < vector.length; ++i) {
			and[i] = vector[i] & that.vector[i];
		}
		return new BitVector(and);
	}

	/**
	 * Retourne un vecteur de bits correspondant à la disjonction bit à bit avec le
	 * vecteur de même taille passé en argument
	 * 
	 * @param that
	 *            : vecteur de bits avec lequel on va calculer la disjonction. Doit
	 *            être non null.
	 * @return le vecteur issus de la disjonction entre les deux vecteurs
	 * 
	 * @throws IllegalArgumentException
	 *             si this et that n'ont pas la même taille
	 */
	public BitVector or(BitVector that) {
		Objects.requireNonNull(that);
		Preconditions.checkArgument(size() == that.size());
		int[] or = new int[vector.length];
		for (int i = 0; i < vector.length; ++i) {
			or[i] = vector[i] | that.vector[i];
		}
		return new BitVector(or);
	}

	/**
	 * Extrait un vecteur de taille donnée de l'extension par 0 du vecteur de
	 * l'instance courrante
	 * 
	 * @param index
	 *            : index à partir duquel on commence à extraire
	 * @param size
	 *            : taille du vecteur à extraire. Doit être un multiple de 32
	 * @return le vecteur de bits extrait
	 * 
	 * @throws IllegalArgumentException
	 *             size n'est pas strictement positif ou si size n'est pas un
	 *             multiple de 32
	 */
	public BitVector extractZeroExtended(int index, int size) {
		Preconditions.checkArgument(size > 0 && size % Integer.SIZE == 0);
		return extract(index, size, true);
	}

	/**
	 * Extrait un vecteur de taille donnée de l'extension par enroulement du vecteur
	 * de l'instance courrante
	 * 
	 * @param index
	 *            : index à partir duquel on commence à extraire
	 * @param size
	 *            : taille du vecteur à extraire. Doit être un multiple de 32
	 * @return le vecteur de bits extrait
	 * 
	 * @throws IllegalArgumentException
	 *             size n'est pas strictement positif ou si size n'est pas un
	 *             multiple de 32
	 */
	public BitVector extractWrapped(int index, int size) {
		Preconditions.checkArgument(size > 0 && size % Integer.SIZE == 0);
		return extract(index, size, false);

	}

	/**
	 * Aide les méthodes d'extraction
	 * 
	 * @param index
	 *            : index à partir duquel on va extraire les size bits
	 * @param size
	 *            : nombre de bits à extraire
	 * @param isZeroExtended
	 *            : vrai si l'extraction se fait selon l'extension par 0
	 * @return le BitVector correspondant à l'extraction
	 */
	private BitVector extract(int startIndex, int size, boolean isZeroExtended) {
		int[] extracted = new int[size / Integer.SIZE];
		int floorIndex = floorDiv(startIndex, Integer.SIZE);
		int shift = floorMod(startIndex, Integer.SIZE);

		if (startIndex % Integer.SIZE == 0) {
			for (int i = 0; i < (size / Integer.SIZE); ++i)
				extracted[i] = getElementOfInfiniteExtentionArray(floorIndex + i, isZeroExtended);

		} else {
			for (int i = 0; i < (size / Integer.SIZE); ++i) {
				int lowValue = Bits.extract(getElementOfInfiniteExtentionArray(floorIndex + i, isZeroExtended), shift,
						Integer.SIZE - shift);
				int highValue = getElementOfInfiniteExtentionArray(floorIndex + i + 1, isZeroExtended);
				extracted[i] = (highValue << (Integer.SIZE - shift)) | lowValue;
			}
		}
		return new BitVector(extracted);
	}

	/**
	 * Retourne l'élement de l'extention infinie correspondant à l'index donné
	 * 
	 * @param index
	 *            : index de l'élement à retourner, selon le type de l'extraction
	 * @param isZeroExtended
	 *            : vrai si l'extraction se fait selon l'extension par 0
	 * @return l'élement de l'extention infinie
	 */
	private int getElementOfInfiniteExtentionArray(int index, boolean isZeroExtended) {
		int valueOutOfVectorArray = (isZeroExtended) ? 0 : vector[floorMod(vector.length + index, vector.length)];
		return (index >= 0 && index < vector.length) ? vector[index] : valueOutOfVectorArray;
	}

	/**
	 * décale le vecteur de l'instance courante d'une distance quelconque, en
	 * utilisant la convention habituelle qu'une distance positive représente un
	 * décalage à gauche, une distance négative un décalage à droite
	 * 
	 * @param size
	 *            : nombre de bits de décalage
	 * @return le vecteur correspondant au décalage des size bits de l'instance
	 *         courante
	 */
	public BitVector shift(int size) {
		return extractZeroExtended(-size, size());
	}

	@Override
	public boolean equals(Object object) {
		return (object instanceof BitVector) && Arrays.equals(((BitVector) object).vector, vector);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(vector);
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		for (int i = vector.length - 1; i >= 0; --i) {
			int value = vector[i];
			for (int j = 0; j < Integer.numberOfLeadingZeros(value); ++j) {
				b.append("0");
			}
			if (value != 0) {
				b.append(Integer.toBinaryString(value));
			}

		}
		return b.toString();

	}

	/**
	 * @return une représention hexadécimale de toString
	 */
	public String toHexString() {
		StringBuilder b = new StringBuilder();
		for (int i = vector.length - 1; i >= 0; --i) {
			int value = vector[i];

			for (int j = 0; j < 8; ++j) {
				if ((value >>> Integer.SIZE - (4 * j)) == 0) {
					b.append("0");
				}
				continue;
			}

			if (value != 0) {
				b.append(Integer.toHexString(value));
			}
		}
		return b.toString();
	}
	
	

	/**
	 * @author lucas
	 * Builder de BitVector
	 *
	 */
	public final static class Builder {

		private int[] vector;
		private static final int[] MASK = {0xFFFFFF00, 0xFFFF00FF, 0xFF00FFFF, 0x00FFFFFF};
		private static final int BYTES_PER_INTEGER = 4;

		/**
		 * @param size
		 *            : taille du vecteur de bits à construire, doit être un multiple de
		 *            32 non négatif.
		 * @throws IllegalArgumentException
		 *             size n'est pas strictement positif ou si size n'est pas un
		 *             multiple de 32
		 */
		public Builder(int size) {
			Preconditions.checkArgument(size > 0 && (size % Integer.SIZE) == 0);
			vector = new int[size / Integer.SIZE];
		}

		/**
		 * Définir la valeur d'un octet désigné par son index
		 * 
		 * @param index
		 *            : index du byte dont on va assigner la valeur newValue. Doit être
		 *            non négatif
		 * @param newValue
		 *            : valeur de l'octet à définir. Doit être une valeur 8 bits
		 * 
		 * @throws IllegalStateException
		 *             si le bitVector a déjà été construit
		 * @throws IndexOutOfBoundsException
		 *             si l'index est invalide
		 * @throws IllegalArgumentException
		 *             si newValue n'est pas une valeur 8 bits
		 */
		public Builder setByte(int index, int newValue) {
			if (vector == null) {
				throw new IllegalStateException();
			}
			Objects.checkIndex(index, BYTES_PER_INTEGER * vector.length);
			Preconditions.checkBits8(newValue);
			int arrayIndex = index / BYTES_PER_INTEGER;
			int byteIndex = index %BYTES_PER_INTEGER;
			int byteShift = Byte.SIZE * (byteIndex);
			int v = vector[arrayIndex];

			vector[arrayIndex] = v & MASK[byteIndex]| newValue << byteShift;
			return this;
		}

		/**
		 * @return le vecteur de bit immuable constuit
		 * 
		 * @throws IllegalStateException
		 *             si le bitVector a déjà été construit
		 */
		public BitVector build() {
			if (vector == null) {
				throw new IllegalStateException();
			}
			BitVector b = new BitVector(vector);
			vector = null;
			return b;
		}
	}
}
