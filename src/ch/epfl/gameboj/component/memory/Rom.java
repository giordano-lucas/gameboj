package ch.epfl.gameboj.component.memory;

import java.util.Arrays;
import java.util.Objects;

import ch.epfl.gameboj.Preconditions;

public final class Rom {
	private final byte[] data;

	/**
	 * Construit une mémoire morte dont le contenu et la taille sont ceux du tableau
	 * d'octets donné en argument, ou lève NullPointerException si celui-ci est nul.
	 * 
	 * @param data
	 *            : tableau à partir duquel la rom va être construite, doit être non
	 *            null
	 */
	public Rom(byte[] data) {
		Objects.requireNonNull(data);
		Preconditions.checkArgument(data.length >= 0);
		this.data = Arrays.copyOf(data, data.length);
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
	 *            : entier correspondant à l'index de l'élement que l'on désire
	 *            obtenir. Doit être plus petit que la taille de la rom
	 * @return l'octet lu à l'index donné dans la mémoire
	 */
	public int read(int index) {
		Objects.checkIndex(index, data.length);
		return Byte.toUnsignedInt(data[index]);
	}

}
