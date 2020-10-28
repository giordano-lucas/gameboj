package ch.epfl.gameboj.component;

import static ch.epfl.gameboj.Preconditions.checkBits16;

import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;

public final class Joypad implements Component {

	/**
	 * @author lucas Type énuméré représentant les diférents bit de registre P1
	 *
	 */
	private enum REG_P1 implements Bit {
		COL_0, COL_1, COL_2, COL_3, ROW_0, ROW_1, UNUSED_0, UNUSED_1;
	}

	/**
	 * @author lucas Type énumuré représentant les diférentes touches du GameBoy
	 *
	 */
	public enum Key implements Bit {
		RIGHT, LEFT, UP, DOWN, A, B, SELECT, START
	}

	private static final int KEYS_PER_ROW = 4;
	private final Cpu cpu;
	private int P1;
	private boolean row0Activated;
	private boolean row1Activated;
	private int row0;
	private int row1;

	/**
	 * Construit le joypad
	 * 
	 * @param cpu
	 *            : cpu du gameboy auquel appartient le joypad, doit être non null;
	 */
	public Joypad(Cpu cpu) {
		Objects.requireNonNull(cpu);
		this.cpu = cpu;
		P1 = 0;
	}

	@Override
	public int read(int address) {
		checkBits16(address);
		if (address == AddressMap.REG_P1) {
			return Bits.complement8(P1);
		}
		return NO_DATA;
	}

	@Override
	public void write(int address, int data) {
		Preconditions.checkBits16(address);
		Preconditions.checkBits8(data);

		if (address == AddressMap.REG_P1) {
			row0Activated = !Bits.test(data, REG_P1.ROW_0);
			row1Activated = !Bits.test(data, REG_P1.ROW_1);
			P1 = Bits.set(P1, REG_P1.ROW_0.index(), row0Activated);
			P1 = Bits.set(P1, REG_P1.ROW_1.index(), row1Activated);
			updateP1();
		}
	}

	/**
	 * Permet de simuler la pression d'une touche
	 * 
	 * @param key
	 *            : touche que l'on désire presser;
	 */
	public void keyPressed(Key key) {
		Objects.requireNonNull(key);

		if (!Bits.test(P1, key.ordinal() % KEYS_PER_ROW)) {
			cpu.requestInterrupt(Interrupt.JOYPAD);
			if (key.ordinal() < KEYS_PER_ROW) {
				row0 = Bits.set(row0, key.ordinal(), true);
			} else {
				row1 = Bits.set(row1, key.ordinal() - KEYS_PER_ROW, true);
			}
		}
		updateP1();
	}

	/**
	 * Permet de simuler le relâchement d'une touche
	 * 
	 * @param key
	 *            : touche que l'on désire relacher
	 */
	public void keyReleased(Key key) {
		Objects.requireNonNull(key);

		if (key.ordinal() < KEYS_PER_ROW) {
			row0 = Bits.set(row0, key.ordinal(), false);
		} else if (key.ordinal() >= KEYS_PER_ROW) {
			row1 = Bits.set(row1, key.ordinal() - KEYS_PER_ROW, false);
		}
		updateP1();
	}

	/**
	 * Permet de mettre à jour la valeur de P1 suivant les valeurs des attributs
	 * romActivated
	 */
	private void updateP1() {
		P1 = (Bits.extract(P1, KEYS_PER_ROW, KEYS_PER_ROW) << KEYS_PER_ROW)
				| ((row1Activated ? row1 : 0) | (row0Activated ? row0 : 0));
	}

}
