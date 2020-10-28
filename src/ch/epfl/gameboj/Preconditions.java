package ch.epfl.gameboj;

public interface Preconditions {

	/**
	 * renvoie une IllegalArgumentException si b est faux
	 * 
	 * @param b
	 *            : expression booléenne à évaluer
	 */
	static void checkArgument(boolean b) {
		if (!b) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * vérifie que la valeur donnée est bien une valeur 8 bits
	 * 
	 * @param v
	 *            : entier à évaluer
	 * @return v si c'est une valeur 8 bits. Lève une IllegalArgumentException sinon
	 */
	static int checkBits8(int v) {
		checkArgument(v >= 0 && v < 256);
		return v;
	}

	/**
	 * vérifie que la valeur donnée est bien une valeur 16 bits
	 * 
	 * @param v
	 *            : entier à évaluer
	 * @return v si c'est une valeur 16 bits. Lève une IllegalArgumentException
	 *         sinon
	 */
	static int checkBits16(int v) {
		checkArgument(v >= 0 && v < 65536);
		return v;
	}
}
