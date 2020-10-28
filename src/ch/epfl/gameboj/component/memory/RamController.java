/*package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;

public final class RamController implements Preconditions, Component {

	int startAddress;
	int endAddress;
	Ram ram;

	public RamController(Ram ram, int startAddress, int endAddress) {
		if (ram == null)
			throw new NullPointerException();
		checkBits16(startAddress);
		checkBits16(endAddress);
		checkArgument( (endAddress - startAddress) >= 0 && (endAddress - startAddress) < ram.size()); // quid de la valeur zéro
		this.ram = ram;
		this.startAddress = startAddress;
		this.endAddress = endAddress;
	}

	public RamController(Ram ram, int startAddress) {
		this(ram, startAddress, ram.size()-1);
	}

	@Override
	public int read(int address) {
		// TODO Auto-generated method stub
		if (address >= startAddress && address <= endAddress) {
			return ram.read(address);
		} else
			throw new IndexOutOfBoundsException();
	}

	@Override
	public void write(int address, int data) {
		// TODO Auto-generated method stub
		if (address >= startAddress && address <= endAddress) {
			ram.write(address, data);
		} else
			throw new IndexOutOfBoundsException();

	}

}*/
package ch.epfl.gameboj.component.memory;

import java.util.Objects;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;

public final class RamController implements Component {
	private final Ram ram;
	private final int startAddress;
	private final int endAddress;

	/**
	 * Construit un contrôleur pour la mémoire vive donnée, accessible entre
	 * l'adresse startAddress (inclue) et endAddress (exclue), dont l'intervalle ne
	 * peut être nul ou négatif
	 * 
	 * @param ram
	 *            : mémoire vive à restraindre, doit être non nulle
	 * @param startAddress
	 *            : adresse à partir de laquelle on peut accéder à la mémoire, doit
	 *            être une valeur de 16 bits
	 * @param endAddress
	 *            : : adresse à partir de laquelle on ne peut plus accéder à la
	 *            mémoire, doit être une valeur de 16 bits
	 * 
	 */
	public RamController(Ram ram, int startAddress, int endAddress) {
		this.ram = Objects.requireNonNull(ram);
		Preconditions.checkArgument((endAddress - startAddress) <= ram.size());
		this.startAddress = Preconditions.checkBits16(startAddress);
		this.endAddress = Preconditions.checkBits16(endAddress - 1) + 1;

	}

	/**
	 * appelle le premier constructeur en lui passant une adresse de fin telle que
	 * la totalité de la mémoire vive soit accessible au travers du contrôleur.
	 */
	public RamController(Ram ram, int startAddress) {
		this(ram, startAddress, ram.size() + startAddress);
	}

	@Override
	public int read(int address) {
		address = Preconditions.checkBits16(address);
		Preconditions.checkArgument(address >= 0);
		return (address < startAddress || address >= endAddress) ? Component.NO_DATA : ram.read(address - startAddress);
	}

	@Override
	public void write(int address, int data) {
		address = Preconditions.checkBits16(address);
		data = Preconditions.checkBits8(data);
		if ((address >= startAddress) && (address < endAddress)) {
			ram.write(address - startAddress, data);
		}

	}
}
