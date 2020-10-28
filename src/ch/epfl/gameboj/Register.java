package ch.epfl.gameboj;

public interface Register {
	/**
	 * (destiné à être implémenter par un ou des types énumérés)
	 */

	/**
	 @return l'index de l'élément du type énuméré implémentant l'interface
	 *         (suivant l'orde au moment de la déclaration) 
	 */
	public int ordinal();

	/**
	 * @return l'index de l'élément du type énuméré implémentant l'interface
	 *         (suivant l'orde au moment de la déclaration)
	 */
	public default int index() {
		return ordinal();
	}
}