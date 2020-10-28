package ch.epfl.gameboj.component.cartridge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Rom;

public final class Cartridge implements Component {

	private final Component mbc;
	private final static int MBC0_ROM_SIZE = 32768;
	private final static int CARTRIDGE_TYPE_ADDRESS = 0x147;
	private final static int RAM_SIZE_ADDRESS = 0x149;
	private final static int MBC0 = 0;
	private final static int MAX_VALUE_MBC1 = 3;
	private final static int[] RAM_SIZE = { 0, 2048, 8192, 32768 };

	/**
	 * Construit la cartouche
	 * 
	 * @param memoryBankController
	 *            : le controle de banc de mémoire à associé à la cartouche
	 */
	private Cartridge(Component memoryBankController) {
		mbc = memoryBankController;
	}

	/**
	 * Aide à contruire la cartouche. Premièrement, au moyen des flots d'entrées, en
	 * lisant les octets contenus dans le fichier rom poun en faire une rom. La
	 * méthode construit alors réellement la cartouche au moyen du constructeur
	 * privé de la classe
	 * 
	 * @param romFile
	 *            : ficher dont on va extraire les différents octets de la future
	 *            Rom
	 * @return une cartouche dont la mémoire morte contient les octets du fichier
	 *         donné ; et l'exception IllegalArgumentException si le fichier en
	 *         question ne contient pas 0 à la position 14716.
	 * @throws FileNotFoundException,
	 *             IOException
	 * @throws IOException
	 *             en en cas d'erreur d'entrée-sortie, y compris si le fichier donné
	 *             n'existe pas
	 */
	public static Cartridge ofFile(File romFile) throws IOException {
		Objects.requireNonNull(romFile);
		
		byte[] romArray = new byte[(int) romFile.length()];
		try (FileInputStream s = new FileInputStream(romFile)) {
			int sumByteRead = 0;
			while (sumByteRead < romArray.length) {
				sumByteRead += s.read(romArray, sumByteRead, romArray.length - sumByteRead);
			}
		}
		Component memoryBankController;
		Rom rom = new Rom(romArray);
		int type = rom.read(CARTRIDGE_TYPE_ADDRESS);

		if (type == MBC0) {
			Preconditions.checkArgument(rom.size() == MBC0_ROM_SIZE);
			memoryBankController = new MBC0(rom);
		} else if (type <= MAX_VALUE_MBC1)
			memoryBankController = new MBC1(rom, RAM_SIZE[rom.read(RAM_SIZE_ADDRESS)]);
		else
			throw new IllegalArgumentException();

		return new Cartridge(memoryBankController);
	}

	@Override
	public int read(int address) {
		Preconditions.checkBits16(address);
		return mbc.read(address);

	}

	@Override
	public void write(int address, int data) {
		Preconditions.checkBits16(address);
		Preconditions.checkBits8(data);
		mbc.write(address, data);
	}

}