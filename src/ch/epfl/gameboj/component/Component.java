package ch.epfl.gameboj.component;

import ch.epfl.gameboj.Bus;

public interface Component {

	public static final int NO_DATA = 256;

	/**
	 * Retourne l'octet stocké à l'adresse donnée par le composant , ou NO_DATA si
	 * le composant ne possède aucune valeur à cette adresse ; lève l'exception
	 * IllegalArgumentException si l'adresse n'est pas une valeur 16 bits,
	 * 
	 * @param address
	 *            : valeur de 16 bits à laquelle on va lire les données
	 * @return la valeur lue à l'addresse
	 */
	public abstract int read(int address);

	/**
	 * Stocke la valeur donnée à l'adresse donnée dans le composant, ou ne fait rien
	 * si le composant ne permet pas de stocker de valeur à cette adresse ; lève
	 * l'exception IllegalArgumentException si l'adresse n'est pas une valeur 16
	 * bits ou si la donnée n'est pas une valeur 8 bits.
	 * 
	 * @param address
	 *            : valeur de 16 bits à laquelle on va écrire les données
	 * @param data
	 *            : entier de 8 bits que l'on va écrire à l'adresse donnée
	 */
	public abstract void write(int address, int data);

	/**
	 * Attache le composant au bus donné
	 * 
	 * @param bus
	 *            : bus auquel on désire attacher le composant
	 */
	public default void attachTo(Bus bus) {
		bus.attach(this);

	}
}
