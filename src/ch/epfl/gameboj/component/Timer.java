package ch.epfl.gameboj.component;



import java.util.Objects;
import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;

public final class Timer implements Component, Clocked {

	private final Cpu cpu;
	private int div, tima, tma, tac;

	/**
	 * Construit un minuteur associé au processeur donné, ou lève l'exception
	 * NullPointerException si celui-ci est nul.
	 * 
	 * @param cpu
	 *            : processeur du gameboy, doit être non null
	 */
	public Timer(Cpu cpu) {
		this.cpu = Objects.requireNonNull(cpu);
		div = 0;
		tima = 0;
		tma = 0;
		tac = 0;

	}

	@Override
	public void cycle(long cycle) {
		boolean previousState = state();
		div = Bits.clip(16, div + 4);
		incIfChange(previousState);
	}

	@Override
	public int read(int address) {
		Preconditions.checkBits16(address);
		switch (address) {
		case AddressMap.REG_DIV:
			return Bits.extract(div, 8, 8);
		case AddressMap.REG_TIMA:
			return tima;
		case AddressMap.REG_TMA:
			return tma;
		case AddressMap.REG_TAC:
			return tac;
		default:
			return NO_DATA;
		}

	}

	@Override
	public void write(int address, int data) {
		Preconditions.checkBits16(address);
		Preconditions.checkBits8(data);

		boolean previousState;
		switch (address) {
		case AddressMap.REG_DIV:
			previousState = state();
			div = 0;
			incIfChange(previousState);
			break;
		case AddressMap.REG_TIMA:
			tima = data;
			break;
		case AddressMap.REG_TMA:
			tma = data;
			break;
		case AddressMap.REG_TAC:
			previousState = state();
			tac = data;
			incIfChange(previousState);
			break;
		default:
			return;
		}

	}

	/**
	 * @return l'état du minuteur, c-à-d la conjonction logique du bit 2 du registre
	 *         TAC et du bit du compteur principal désigné par les 2 bits de poids
	 *         faible de ce même registre,
	 */
	private boolean state() {

		int index = 0;
		switch (Bits.extract(read(AddressMap.REG_TAC), 0, 2)) {
		case 0: {
			index = 9;
		}
			break;
		case 1: {
			index = 3;
		}
			break;
		case 2: {
			index = 5;
		}
			break;
		case 3: {
			index = 7;
		}
			break;
		default:
			break;
		}
		return Bits.test(tac, 2) && Bits.test(div, index);

	}

	/**
	 * Incrémente le compteur secondaire si et seulement si l'état passé en argument
	 * est vrai et l'état actuel (retourné par state) est faux.
	 * 
	 * @param previousState
	 *            : valeur booléenne représentant l'état précédent
	 */
	private void incIfChange(boolean previousState) {

		if (previousState && !state()) {
			if (tima == 0xFF) {
				cpu.requestInterrupt(Interrupt.TIMER);
				tima = tma;
			} else {
				tima++;
			}
		}
	}

}