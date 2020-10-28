package ch.epfl.gameboj.component.memory;

import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cartridge.Cartridge;

public final class BootRomController implements Component, BootRom {

	private final Cartridge cartridge;
	private boolean bootRomUnable;
	private final Rom bootRom;

	/**
	 * Construit un contrôleur de mémoire de démarrage auquel la cartouche donnée
	 * est attachée ; lève l'exception NullPointerException si cette cartouche est
	 * nulle
	 * 
	 * @param cartridge
	 *            : cartouche associée au BootRomController, non nulle
	 */
	public BootRomController(Cartridge cartridge) {
		this.cartridge = Objects.requireNonNull(cartridge);
		bootRomUnable = true;
		bootRom = new Rom(DATA);
	}

	@Override
	public int read(int address) {
		Preconditions.checkBits16(address);

		return (bootRomUnable && address < AddressMap.BOOT_ROM_END && address >= AddressMap.BOOT_ROM_START)
				? bootRom.read(address)
				: cartridge.read(address);
	}

	@Override
	public void write(int address, int data) {
		Preconditions.checkBits16(address);
		Preconditions.checkBits8(data);

		if (address == AddressMap.REG_BOOT_ROM_DISABLE) {
			bootRomUnable = false;
		}
		cartridge.write(address, data);

	}
}