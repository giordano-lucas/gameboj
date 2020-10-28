package ch.epfl.gameboj.component.lcd;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import static ch.epfl.gameboj.AddressMap.*;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;
import ch.epfl.gameboj.component.memory.Ram;

public final class LcdController implements Component, Clocked {

	public static final int LCD_WIDTH = 160;
	public static final int LCD_HEIGHT = 144;
	private static final int LCD_FULLSIZE = 256;
	private static final int CYCLES_PER_LINE = 114;
	private static final int CYCLES_PER_IMAGE = 17556;
	private static final int CYCLES_MODE_0 = 51;
	private static final int CYCLES_MODE_2 = 20;
	private static final int CYCLES_MODE_3 = 43;
	private static final int START_MODE_0 = 63;
	private static final int START_MODE_2 = 0;
	private static final int START_MODE_3 = 20;
	private static final int UPDATE_INDEX_IN_REG_STAT = 3;
	private static final int BYTES_PER_SPRITE = 4;
	private static final int TILES_PER_LIGNE = 32;
	private static final int BITS_PER_TILE_SIDE = 8;
	private static final int STANDARD_SPRITE_SIZE = 8;
	private static final int BYTES_PER_TILE = 16;
	private static final int AJUST_WX = 7;
	private static final int ADJUST_X_SPRITE = 8;
	private static final int ADJUST_Y_SPRITE = 16;
	private static final int MAXIMUM_NUMBER_OF_SPRITES = 10;
	private static final int MID_INTERVAL_VALUE_TILE_INDEX = 0x80;
	private static final int ADJUST_PLAGE_TILE = 0x800;

	private final Cpu cpu;
	private Bus bus;
	private LcdImage image;
	private final RegisterFile<Reg> registerFile;
	private final Ram videoRam;
	private final Ram OAMram;
	private long nextNonIdleCycle;
	private long lcdOnCycle;
	private LcdImage.Builder nextImageBuilder;
	private int winY;
	private int currentCopyAddress;

	/**
	 * @author lucas Type énuméré représentant toutes les registres du LCDcontroller
	 *
	 */
	private enum Reg implements Register {
		LCDC, STAT, SCY, SCX, LY, LYC, DMA, BGP, OBP0, OBP1, WY, WX
	}

	/**
	 * @author lucas Type énuméré représentant les diférents bit du registre LCDC
	 *
	 */
	private enum LCDC implements Bit {
		BG, OBJ, OBJ_SIZE, BG_AREA, TILE_SOURCE, WIN, WIN_AREA, LCD_STATUS
	}

	/**
	 * @author lucas Type énuméré représentant les diférents bit du registre STAT
	 *
	 */
	private enum STAT implements Bit {
		MODE0, MODE1, LYC_EQ_LY, INT_MODE0, INT_MODE1, INT_MODE2, INT_LYC, UNUSED;
	}

	/**
	 * @author lucas Type énuméré représentant les 4 diférents bytes d'informations
	 *         d'un sprite
	 *
	 */
	private enum SpriteAttributs implements Bit {
		Y, X, TILE_INDEX, INFO
	}

	/**
	 * @author lucas Type énuméré representant les diférents bit du dernier byte (4
	 *         sur 4) d'information d'un sprite
	 *
	 */
	private enum InfoSprite implements Bit {
		UNUSED_0, UNUSED_1, UNUSED_2, UNUSED_3, PALETTE, FLIP_H, FLIP_V, BEHIND_BG
	}

	/**
	 * @author lucas Type énuméré répresentant les diférents modes dans lequel le
	 *         LCD controller peut se trouver
	 *
	 */
	private enum MODE implements Bit {
		MODE_0, MODE_1, MODE_2, MODE_3
	}

	public LcdController(Cpu cpu) {
		Objects.requireNonNull(cpu);
		this.cpu = cpu;
		registerFile = new RegisterFile<>(Reg.values());
		for (Reg r : Reg.values()) {
			registerFile.set(r, 0);
		}
		videoRam = new Ram(VIDEO_RAM_SIZE);
		OAMram = new Ram(OAM_RAM_SIZE);
		nextNonIdleCycle = Long.MAX_VALUE;
		lcdOnCycle = 0;
		nextImageBuilder = new LcdImage.Builder(LCD_HEIGHT, LCD_WIDTH);
		image = nextImageBuilder.build();
		winY = 0;
		currentCopyAddress = AddressMap.OAM_RAM_SIZE;
	}

	@Override
	public void cycle(long cycle) {
		if (nextNonIdleCycle == Long.MAX_VALUE && registerFile.testBit(Reg.LCDC, LCDC.LCD_STATUS)) {
			nextNonIdleCycle = cycle;
			lcdOnCycle = cycle;
		}
		if (currentCopyAddress < OAM_RAM_SIZE) {
			int value = bus.read((registerFile.get(Reg.DMA) << 8) | currentCopyAddress);
			OAMram.write(currentCopyAddress, value);
			++currentCopyAddress;
		}
		if (cycle == nextNonIdleCycle)
			reallyCycle(cycle);
	}

	private void reallyCycle(long cycle) {
		int elapsedCycles = (int) (cycle - lcdOnCycle) % CYCLES_PER_IMAGE;
		int currentLine = elapsedCycles / CYCLES_PER_LINE;

		if (elapsedCycles < LCD_HEIGHT * CYCLES_PER_LINE) {
			switch (elapsedCycles % CYCLES_PER_LINE) {
			case START_MODE_2: {
				setMode(MODE.MODE_2);
				updateLYorLYC(Reg.LY, currentLine);
				nextNonIdleCycle += CYCLES_MODE_2;

				if (elapsedCycles == 0) {
					nextImageBuilder = new LcdImage.Builder(LCD_HEIGHT, LCD_WIDTH);
					winY = 0;
				}
			}
				break;
			case START_MODE_3: {
				setMode(MODE.MODE_3);
				computeLine(currentLine);
				nextNonIdleCycle += CYCLES_MODE_3;

			}
				break;
			case START_MODE_0: {
				setMode(MODE.MODE_0);
				nextNonIdleCycle += CYCLES_MODE_0;
			}
				break;
			}
		} else {
			if (elapsedCycles == LCD_HEIGHT * CYCLES_PER_LINE) {
				setMode(MODE.MODE_1);
				image = nextImageBuilder.build();
				cpu.requestInterrupt(Interrupt.VBLANK);
			}
			updateLYorLYC(Reg.LY, currentLine);
			nextNonIdleCycle += CYCLES_PER_LINE;
		}
	}

	@Override
	public int read(int address) {
		Preconditions.checkBits16(address);

		if (address >= AddressMap.VIDEO_RAM_START && address < AddressMap.VIDEO_RAM_END)
			return videoRam.read(address - AddressMap.VIDEO_RAM_START);

		else if (address >= AddressMap.REGS_LCDC_START && address < AddressMap.REGS_LCDC_END)
			return registerFile.get(Reg.values()[address - AddressMap.REGS_LCDC_START]);

		else if (address >= AddressMap.OAM_START && address < AddressMap.OAM_END)
			return OAMram.read(address - AddressMap.OAM_START);

		else
			return NO_DATA;
	}

	@Override
	public void write(int address, int data) {
		Preconditions.checkBits16(address);
		Preconditions.checkBits8(data);

		if (address >= VIDEO_RAM_START && address < VIDEO_RAM_END)
			videoRam.write(address - VIDEO_RAM_START, data);

		else if (address >= OAM_START && address < OAM_END)
			OAMram.write(address - OAM_START, data);

		else if (address >= REGS_LCDC_START && address < REGS_LCDC_END) {
			Reg reg = Reg.values()[address - REGS_LCDC_START];

			switch (reg) {
			case LCDC: {
				if (registerFile.testBit(Reg.LCDC, LCDC.LCD_STATUS) && !Bits.test(data, LCDC.LCD_STATUS)) {
					setMode(MODE.MODE_0);
					updateLYorLYC(Reg.LY, 0);
					nextNonIdleCycle = Long.MAX_VALUE;
				}
				registerFile.set(reg, data);
			}
				break;
			case STAT: {
				int value = Bits.extract(registerFile.get(Reg.STAT), 0, 3);
				value |= (Bits.extract(data, 3, 5) << 3);
				registerFile.set(reg, value);
			}
				break;
			case LY: {
			}
				break;
			case LYC: {
				updateLYorLYC(reg, data);
			}
				break;
			case DMA: {
				currentCopyAddress = 0;
				registerFile.set(reg, data);
			}
				break;
			default: {
				registerFile.set(reg, data);
			}
				break;
			}

		}

	}

	@Override
	public void attachTo(Bus bus) {
		Objects.requireNonNull(bus);
		this.bus = bus;
		this.bus.attach(this);
	}

	/**
	 * @return l'image actuellement affichée à l'écran
	 */
	public LcdImage currentImage() {
		return image;
	}

	/**
	 * se charge de la mise à jour du bit LYC_EQ_LY (2) du registre STAT, et de la
	 * levée éventuelle de l'interruption LCD_STAT et stocke la valeur de data dans
	 * le registre correspondant(LY ou LYC)
	 * 
	 * @param reg
	 *            : registre dans lequel on va stocker data (LY ou LYC)
	 * @param data
	 *            : valeur 8 bits à stocker dans reg
	 */
	private void updateLYorLYC(Reg reg, int data) {
		registerFile.set(reg, data);

		if (registerFile.get(Reg.LY) == registerFile.get(Reg.LYC)) {

			registerFile.setBit(Reg.STAT, STAT.LYC_EQ_LY, true);

			if (registerFile.testBit(Reg.STAT, STAT.INT_LYC)) {
				cpu.requestInterrupt(Interrupt.LCD_STAT);
			}
		} else
			registerFile.setBit(Reg.STAT, STAT.LYC_EQ_LY, false);

	}

	/**
	 * lance l'interruption LCD_STAT lors d'un changement de mode si besoin
	 * 
	 * @param r
	 *            : bit de STAT à tester (INT_MODE0,INT_MODE1 ou INT_MODE2)
	 */
	private void throwInterruptIfNeeded(STAT r) {
		if (registerFile.testBit(Reg.STAT, r)) {
			cpu.requestInterrupt(Interrupt.LCD_STAT);
		}
	}

	/**
	 * Sert à changer de mode et à lever l'interruption STAT si besoin
	 * 
	 * @param m
	 *            : mode dans lequel on veut passer
	 */
	private void setMode(MODE m) {

		setMode(Bits.test(m.index(), 1), Bits.test(m.index(), 0));
		// Transforme l'index du mode en un index de STAT
		if (m != MODE.MODE_3)
			throwInterruptIfNeeded(STAT.values()[m.index() + UPDATE_INDEX_IN_REG_STAT]);
	}

	/**
	 * Est utilisé pour changer de mode
	 * 
	 * @param b1
	 *            : bit d'index 1 du mode, 1 ssi est vrai
	 * @param b0
	 *            : bit d'index 0 du mode, 1 ssi est vrai
	 */
	private void setMode(boolean b1, boolean b0) {
		registerFile.setBit(Reg.STAT, STAT.MODE1, b1);
		registerFile.setBit(Reg.STAT, STAT.MODE0, b0);
	}

	/**
	 * Calcule la ligne d'index y et la stocke dans le batisseur d'image
	 * 
	 * @param y
	 *            : index de la ligne à calculer
	 */
	private void computeLine(int y) {

		boolean bitTileSource = registerFile.testBit(Reg.LCDC, LCDC.TILE_SOURCE);
		int tileImageStart = AddressMap.TILE_SOURCE[(bitTileSource) ? 1 : 0];
		int SCY = registerFile.get(Reg.SCY);
		int SCX = registerFile.get(Reg.SCX);
		int WX = Math.max(0, registerFile.get(Reg.WX) - AJUST_WX);
		int WY = registerFile.get(Reg.WY);
		int realY = (y + SCY) % LCD_FULLSIZE;
		int plageStart;

		LcdImageLine.Builder bg = new LcdImageLine.Builder(LCD_FULLSIZE);
		LcdImageLine.Builder window = new LcdImageLine.Builder(LCD_FULLSIZE);

		// Background
		if (registerFile.testBit(Reg.LCDC, LCDC.BG)) {
			plageStart = AddressMap.BG_DISPLAY_DATA[(registerFile.testBit(Reg.LCDC, LCDC.BG_AREA)) ? 1 : 0];
			setValueLine(realY, bg, bitTileSource, plageStart, tileImageStart);
		}
		LcdImageLine finalBG = bg.build().extractWrapped(SCX, LCD_WIDTH).mapColors(registerFile.get(Reg.BGP));

		// Window
		if (registerFile.testBit(Reg.LCDC, LCDC.WIN) && WX >= 0 && WX < LCD_WIDTH && y >= WY) {
			plageStart = AddressMap.BG_DISPLAY_DATA[(registerFile.testBit(Reg.LCDC, LCDC.WIN_AREA)) ? 1 : 0];
			setValueLine(winY, window, bitTileSource, plageStart, tileImageStart);

			LcdImageLine finalWindow = window.build();
			finalWindow = finalWindow.extractWrapped(0, LCD_WIDTH).mapColors(registerFile.get(Reg.BGP));
			finalBG = finalBG.join(WX, finalWindow.shift(WX));
			++winY;
		}

		// Sprites
		LcdImageLine spriteFG = new LcdImageLine.Builder(LCD_WIDTH).build();

		if (registerFile.testBit(Reg.LCDC, LCDC.OBJ)) {
			int[] sprites = new int[MAXIMUM_NUMBER_OF_SPRITES];
			LcdImageLine spriteBG = new LcdImageLine.Builder(LCD_WIDTH).build();

			for (int i = 0; i < spritesIntersectingLine(y, sprites); i++) {

				if (Bits.test(OAMram.read(BYTES_PER_SPRITE * Bits.clip(8, sprites[i]) + SpriteAttributs.INFO.index()),
						InfoSprite.BEHIND_BG))
					spriteBG = setSpriteLine(y, sprites[i]).below(spriteBG);
				else
					spriteFG = setSpriteLine(y, sprites[i]).below(spriteFG);
			}

			BitVector opacityBG = spriteBG.opacity().not().or(finalBG.opacity());
			finalBG = spriteBG.below(opacityBG, finalBG);

		}

		nextImageBuilder.setLine(y, finalBG.below(spriteFG));

	}

	/**
	 * Calcule réellement la ligne
	 * 
	 * @param y
	 *            : index de la ligne
	 * @param line
	 *            : ligne à construire
	 * @param bitTileSource
	 *            : vrai si le bit Tile source de LCDC vaut 1
	 * @param tileAddressStart
	 *            : plage [980016 à 9C0016] ou plage [9C0016 à A00016] obtenue à
	 *            partir de AddressMap.BG_DISPLAY_DATA
	 * @param tileImageStart
	 *            : plaqe 8000 ou 8800 obtenue à partir de AddressMap.TILE_SOURCE
	 */
	private void setValueLine(int y, LcdImageLine.Builder line, boolean bitTileSource, int tileAddressStart,
			int tileImageStart) {

		for (int x = 0; x < TILES_PER_LIGNE; ++x) {
			// obtient les 8 bits de poids faible de l'index de la tuile
			int tileIndex = read(tileAddressStart + (TILES_PER_LIGNE * (y / BITS_PER_TILE_SIDE)) + x);

			// sert à mettre à jour tileImageStart (de 0x8800 à 0x9000) si besoin
			int adjustPlage = (!bitTileSource && tileIndex < MID_INTERVAL_VALUE_TILE_INDEX) ? ADJUST_PLAGE_TILE : 0;

			// sert à mettre à jour tileIndex en retirant 0x80 si besoin
			int adjustTileIndex = (!bitTileSource && (tileIndex >= MID_INTERVAL_VALUE_TILE_INDEX))
					? -MID_INTERVAL_VALUE_TILE_INDEX
					: 0;

			int finalAddress = (tileImageStart + adjustPlage) + (tileIndex + adjustTileIndex) * BYTES_PER_TILE
					+ y % BITS_PER_TILE_SIDE * 2;

			int msb = Bits.reverse8(read(finalAddress + 1));
			int lsb = Bits.reverse8(read(finalAddress));
			line.setBytes(x, msb, lsb);
		}

	}

	/**
	 * Calcule la ligne correspondant au sprite donné, dans la ligne y
	 * 
	 * @param y
	 *            : ligne à partir de laquelle on veut calculer les sprites
	 * @param spriteValue
	 *            : valeur empaquetée avec dans les 8 bits de poids fort la
	 *            coordonée x du sprite et l'index du sprite dans les 8 bits de
	 *            poids faible
	 * @return : la ligne correspondant au sprite, shiftée et mapé avec les bonnes
	 *         couleurs
	 */
	private LcdImageLine setSpriteLine(int y, int spriteValue) {

		LcdImageLine.Builder line = new LcdImageLine.Builder(LCD_WIDTH);
		int spriteIndex = Bits.clip(8, spriteValue);
		int xSprite = Bits.extract(spriteValue, 8, 8) - ADJUST_X_SPRITE;
		int ySprite = OAMram.read(BYTES_PER_SPRITE * spriteIndex + SpriteAttributs.Y.index()) - ADJUST_Y_SPRITE;
		int tileIndex = OAMram.read(BYTES_PER_SPRITE * spriteIndex + SpriteAttributs.TILE_INDEX.index());
		int infoSprite = OAMram.read(BYTES_PER_SPRITE * spriteIndex + SpriteAttributs.INFO.index());
		int spriteSize = (registerFile.testBit(Reg.LCDC, LCDC.OBJ_SIZE)) ? STANDARD_SPRITE_SIZE * 2
				: STANDARD_SPRITE_SIZE;

		int palette = (Bits.test(infoSprite, InfoSprite.PALETTE)) ? registerFile.get(Reg.OBP1)
				: registerFile.get(Reg.OBP0);
		int realIndex = (Bits.test(infoSprite, InfoSprite.FLIP_V)) ? (spriteSize - 1) - (y - ySprite) : y - ySprite;

		int msb = videoRam.read(tileIndex * BYTES_PER_TILE + realIndex * 2 + 1);
		int lsb = videoRam.read(tileIndex * BYTES_PER_TILE + realIndex * 2);

		if (!Bits.test(infoSprite, InfoSprite.FLIP_H)) {
			msb = Bits.reverse8(msb);
			lsb = Bits.reverse8(lsb);
		}
		return line.setBytes(0, msb, lsb).build().shift(xSprite).mapColors(palette);

	}

	/**
	 * Calcule les sprites à afficher sur une ligne donnée (y) dont l'image
	 * intersecte la ligne donnée, stocke ceux-ci dans le tableau passé en paramètre
	 * et le trie selon l'ordre d'empilement.
	 * 
	 * @param y
	 *            : ligne à partir de laquelle on veut calculer les sprites
	 * @param sprites
	 *            : tableau contenant les futurs index des (au plus 10) sprites
	 * @return : le nombre de de sprites interectant y
	 */
	private int spritesIntersectingLine(int y, int[] sprites) {
		int spriteNumber = 0;
		int spriteSize = (registerFile.testBit(Reg.LCDC, LCDC.OBJ_SIZE)) ? STANDARD_SPRITE_SIZE * 2
				: STANDARD_SPRITE_SIZE;

		for (int index = 0; index < OAM_RAM_SIZE; index += BYTES_PER_SPRITE) {

			int ySprite = OAMram.read(index + SpriteAttributs.Y.index()) - ADJUST_Y_SPRITE;

			if (y >= ySprite && y < (ySprite + spriteSize) && spriteNumber < sprites.length) {
				int xSprite = (OAMram.read(index + SpriteAttributs.X.index()));
				int spriteIndex = index / BYTES_PER_SPRITE;
				sprites[spriteNumber] = (xSprite << 8) | spriteIndex;
				++spriteNumber;
			}
		}

		Arrays.sort(sprites, 0, spriteNumber);
		return spriteNumber;
	}
}