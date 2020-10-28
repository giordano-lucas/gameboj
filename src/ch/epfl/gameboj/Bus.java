package ch.epfl.gameboj;

import java.util.ArrayList;
import java.util.Objects;

import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.Preconditions;

public final class Bus {

	private final ArrayList<Component> tab = new ArrayList<Component>();


	/**
	 * attache le composant au bus
	 * 
	 * @param component
	 *            : composant à attacher, non null attache le composant donné au bus
	 */
	public void attach(Component component) {
		component = Objects.requireNonNull(component);
		tab.add(component);
	}

	/**
	 * lit l'octed se trouvant à l'addresse donnée
	 * 
	 * @param address
	 *            : adresse de l'octect que l'on désire lire, doit être une valeur
	 *            de 16 bits
	 * @return l'octet se trouvant à l'adresse donnée
	 */

	public int read(int address) {
		address = Preconditions.checkBits16(address);
		for (int i = 0; i < tab.size(); ++i) {
			if (tab.get(i).read(address) != Component.NO_DATA) {
				return tab.get(i).read(address);
			}
		}
		return 255;

	}

	/**
	 * écrit à l'addresse donnée la valeur donnée
	 * 
	 * @param address
	 *            : addresse où affecter la valeur dans la mémoire, doit être une
	 *            valeur de 16 bits
	 * @param data:
	 *            valeur à affecter, doit être de 8 bits affecte la valeur donnée à
	 *            l'espace d'adresse address dans la mémoire
	 */

	public void write(int address, int data) {
		address = Preconditions.checkBits16(address);
		data = Preconditions.checkBits8(data);
		for (int i = 0; i < tab.size(); ++i) {
			tab.get(i).write(address, data);
		}
	}
}