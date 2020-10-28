package ch.epfl.gameboj.component;

public interface Clocked {

	/**
	 * Demande au composant d'évoluer en exécutant toutes les opérations qu'il doit
	 * exécuter durant le cycle d'index donné en argument.
	 * 
	 * @param cycle
	 *            : valeur correspondant au cycle à effecter
	 * 
	 */
	public void cycle(long cycle);

}
