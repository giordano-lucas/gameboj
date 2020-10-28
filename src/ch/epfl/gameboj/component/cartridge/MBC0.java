package ch.epfl.gameboj.component.cartridge;

import java.util.Objects;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Rom;

public final class MBC0 implements Component {

	private final Rom rom;
	private final int MBC0_ROM_SIZE = 32768;

	/**
	 * construit un contrôleur de type 0 pour la mémoire donnée ; lève l'exception
	 * NullPointerException si la mémoire est nulle, @Throws IllegalArgumentException si
	 * la taille de la rom est invalide
	 * 
	 * @param rom
	 *            : mémoire morte, non nulle et contenenant exactemenent 32 768
	 *            octets.
	 */
	public MBC0(Rom rom) {
		rom = Objects.requireNonNull(rom);
		Preconditions.checkArgument(rom.size() == MBC0_ROM_SIZE);
		this.rom = rom;

	}

	@Override
	public int read(int address) {
		Preconditions.checkBits16(address);
		return (address < MBC0_ROM_SIZE) ? rom.read(address): NO_DATA;
	}

	@Override
	public void write(int address, int data) { // ne fait rien car on ne peut pas écrire dans une mémoire morte
	}

}