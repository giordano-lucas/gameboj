package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.Preconditions;

public final class Ram {
	byte[] data;

	/**
	 * Construit une nouvelle mémoire vive de taille donnée (en octets) ou lève
	 * IllegalArgumentException si celle-ci est strictement négative.
	 * 
	 * @param size
	 *            : taille de la mémoire, >= 0
	 */
	public Ram(int size) {
		Preconditions.checkArgument(size >= 0);
		data = new byte[size];
	}

	/**
	 * @return retourne la taille, en octets, de la mémoire
	 */
	public int size() {
		return data.length;
	}

	/**
	 * Retourne l'octet se trouvant à l'index donné, sous la forme d'une valeur
	 * comprise entre 0 et FF16, ou lève l'exception IndexOutOfBoundsException si
	 * l'index est invalide.
	 * 
	 * @param index
	 *            : entier auquel on va aller chercher les données dans la ram. Doit
	 *            être plus petit ou égal à la taille de la ram;
	 * @return l'octet lu à l'index donné
	 */
	public int read(int index) {
		if (index >= 0 && index < size()) {
			return Byte.toUnsignedInt(data[index]);
		} else {
			throw new IndexOutOfBoundsException();
		}
	}

	/**
	 * Modifie le contenu de la mémoire à l'index donné pour qu'il soit égal à la
	 * valeur donnée ; lève l'exception IndexOutOfBoundsException si l'index est
	 * invalide, et l'exception IllegalArgumentException si la valeur n'est pas une
	 * valeur 8 bits
	 * 
	 * @param index
	 *            : entier correspondant à l'index auquel on va écrire la valeur.
	 *            Doit être plus petit ou égal à la taille de la ram;
	 * @param value
	 *            : nouvelle valeur de 8 bits à stocker dans la ram
	 */
	public void write(int index, int value) {
		if (index >= 0 && index < size()) {
			Preconditions.checkArgument(value >= 0 && value <= 255);
			data[index] = (byte) value;
		} else {
			throw new IndexOutOfBoundsException();
		}
	}
}
