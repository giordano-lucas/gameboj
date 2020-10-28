package ch.epfl.gameboj;

import java.util.Objects;

import ch.epfl.gameboj.component.Joypad;
import ch.epfl.gameboj.component.Timer;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.lcd.LcdController;
import ch.epfl.gameboj.component.memory.BootRomController;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;
import ch.epfl.gameboj.AddressMap;

public class GameBoy {
	
	private final Bus bus;
	private final Ram workRam;
	private final RamController workRamController;
	private final RamController echoRamController;
	private final Cpu cpu;
	private long simulatedCycles;
	private final Cartridge cartridge;
	private final BootRomController bootRomController;
	private final Timer timer;
	private final LcdController lcdController;
	private final Joypad joypad;
	public static final long CYCLES_PER_SECOND = (1 << 20);
	public static final double CYCLES_PER_NANOSECOND = CYCLES_PER_SECOND/Math.pow(10, 9);

	/**
	 * construit leGameBoy
	 * 
	 * @param cartridge
	 *            : cartouche du jeu à simuler. Doit être non null
	 */
	public GameBoy(Cartridge cartridge) {
		simulatedCycles = 0;
		this.cartridge = Objects.requireNonNull(cartridge);
		bus = new Bus();
		workRam = new Ram(AddressMap.WORK_RAM_SIZE);
		cpu = new Cpu();
		timer = new Timer(cpu);
		lcdController = new LcdController(cpu);
		joypad = new Joypad(cpu);

		workRamController = new RamController(workRam, AddressMap.WORK_RAM_START, AddressMap.WORK_RAM_END);
		echoRamController = new RamController(workRam, AddressMap.ECHO_RAM_START, AddressMap.ECHO_RAM_END);
		bootRomController = new BootRomController(cartridge);

		cpu.attachTo(bus);
		timer.attachTo(bus);
		lcdController.attachTo(bus);
		joypad.attachTo(bus);
		
		workRamController.attachTo(bus);
		echoRamController.attachTo(bus);
		bootRomController.attachTo(bus);

	}


	/**
	 * @return le bus associé au GameBoy
	 */
	public Bus bus() {
		return bus;
	}

	/**
	 * @return le cpu asssocié au GameBoy
	 */
	public Cpu cpu() {
		return cpu;
	}

	/**
	 * @return le timer associé au GameBoy
	 */
	public Timer timer() {
		return timer;
	}
	
	/**
	 * @return le lcdController associé au GameBoy
	 */
	public LcdController lcdController() {
		return lcdController;
	}
	
	/**
	 * @return le Joypad associé au GameBoy
	 */
	public Joypad joypad() {
		return joypad;
	}

	/**
	 * simule le fonctionnement du GameBoy jusqu'au cycle donné moins 1,
	 * 
	 * @param cycle
	 *            : nombre de cycles à effectuer, doit être inférieur ou égal au
	 *            nombre de cycles déjà simulés (lève l'exception
	 *            IllegalArgumentException sinon)
	 */
	public void runUntil(long cycle) {
		Preconditions.checkArgument(cycle >= simulatedCycles);

		for (long i = simulatedCycles; i < cycle; ++i) {
			timer.cycle(i);
			lcdController.cycle(i);
			cpu.cycle(i);
			++simulatedCycles;
		}
	}

	/**
	 * @return le nombre de cycles déjà simulés.
	 */
	public long cycles() {
		return simulatedCycles;
	}
}
