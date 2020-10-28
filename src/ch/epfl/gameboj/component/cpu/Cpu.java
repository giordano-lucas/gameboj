package ch.epfl.gameboj.component.cpu;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Alu.Flag;
import ch.epfl.gameboj.component.cpu.Opcode;
import ch.epfl.gameboj.component.memory.Ram;

public final class Cpu implements Component, Clocked {

	/**
	 * Type énuméré correspondant aux différents registres simples du processeur
	 *
	 */
	private enum Reg implements Register {
		A, F, B, C, D, E, H, L
	}

	/**
	 * Type énuméré correspondant aux différentes paires de registres du processeur
	 */
	private enum Reg16 implements Register {
		AF, BC, DE, HL
	}

	/**
	 * Type énuméré définissant la provence d'un des fanions ou sa valeur : V0 => 0,
	 * V1 =>, ALU signifie que le fanion provient d'une méthode de la classe ALU CPU
	 * signifie que le fanion provient du bit correspondant au dit fanion dans le
	 * registre F du CPU
	 *
	 */
	private enum FlagSrc implements Bit {
		V0, V1, ALU, CPU
	}

	/**
	 * Type énuméré définissant les différents interruptions que le CPU peut gérer,
	 * définies dans l'ordre de priorité
	 */
	public enum Interrupt implements Bit {
		VBLANK, LCD_STAT, TIMER, SERIAL, JOYPAD
	}

	/**
	 * Type énuméré correspondant aux conditions attachées aux instructions
	 * conditionnelles Z signifie que le fanion Z est 1, NC signifie qu'il vaut 0.
	 * Pareil pour NC et C
	 *
	 */
	private enum Condition implements Bit {
		NZ, Z, NC, C
	}

	private static final Opcode[] DIRECT_OPCODE_TABLE = buildOpcodeTable(Opcode.Kind.DIRECT);
	private static final Opcode[] PREFIXED_OPCODE_TABLE = buildOpcodeTable(Opcode.Kind.PREFIXED);
	private static final Reg[][] register_16_TO_register_8 = { { Reg.A, Reg.F }, { Reg.B, Reg.C }, { Reg.D, Reg.E },
			{ Reg.H, Reg.L } };
	private static final Reg[] BINARY_TO_register_8 = { Reg.B, Reg.C, Reg.D, Reg.E, Reg.H, Reg.L, null, Reg.A };
	private static final Reg16[] BINARY_TO_register_16 = { Reg16.BC, Reg16.DE, Reg16.HL, Reg16.AF };
	private static final Condition[] BINARY_TO_CONDITION = { Condition.NZ, Condition.Z, Condition.NC, Condition.C };
	private static final int PREFIX = 0xCB;
	private Bus bus;
	private final RegisterFile<Reg> registerFile;
	private final Ram highRam;
	private int SP, PC;
	private long nextNonIdleCycle; 
	private int nextPC;
	private int currentValueInterrupt;
	private int IE, IF;
	private boolean IME;

	/**
	 * construit le CPU
	 */
	public Cpu() {
		registerFile = new RegisterFile<Reg>(Reg.values());
		highRam = new Ram(AddressMap.HIGH_RAM_SIZE);
		SP = 0;
		PC = 0;
		nextPC = 0;
	}

	@Override
	public void attachTo(Bus bus) {
		this.bus = bus;
		this.bus.attach(this);
	}

	@Override
	public int read(int address) {
		Preconditions.checkBits16(address);
		if (address == AddressMap.REG_IE)
			return IE;
		else if (address == AddressMap.REG_IF)
			return IF;
		else if (address >= AddressMap.HIGH_RAM_START && address < AddressMap.HIGH_RAM_END)
			return highRam.read(address - AddressMap.HIGH_RAM_START);
		else
			return Component.NO_DATA;
	}

	@Override
	public void write(int address, int data) {
		Preconditions.checkBits16(address);
		Preconditions.checkBits8(data);
		if (address == AddressMap.REG_IE)
			IE = data;
		else if (address == AddressMap.REG_IF)
			IF = data;
		else if (address >= AddressMap.HIGH_RAM_START && address < AddressMap.HIGH_RAM_END)
			highRam.write(address - AddressMap.HIGH_RAM_START, data);
		else
			return;

	}

	@Override
	public void cycle(long cycle) {
		if (nextNonIdleCycle == Long.MAX_VALUE && testInterrupt())
			nextNonIdleCycle = cycle;
		if (cycle == nextNonIdleCycle)
			reallyCycle(cycle);
	}

	/**
	 * regarde si les interruptions sont activées (c-à-d si IME est vrai) et si une
	 * interruption est en attente, auquel cas elle la gère ; sinon, elle exécute
	 * normalement la prochaine instruction.
	 */
	private void reallyCycle(long cycle) {
		if (IME && testInterrupt()) {
			interruptGestion(); // gère les interruptions
		} else {
			int instruction = read8(PC);
			Opcode opcode = (instruction == PREFIX) ? PREFIXED_OPCODE_TABLE[read8(PC + 1)]
					: DIRECT_OPCODE_TABLE[instruction];
			nextPC = PC + opcode.totalBytes;
			nextNonIdleCycle += opcode.cycles;
			dispatch(opcode);
			PC = nextPC;
		}
	}

	/**
	 * indentifie si une interruption est en attente et la traite si c'est le cas
	 */
	public void interruptGestion() {
		IME = false;
		IF = Bits.set(IF, currentValueInterrupt, false);
		push16(PC);
		PC = AddressMap.INTERRUPTS[currentValueInterrupt];
		nextNonIdleCycle += 5;
	}

	/**
	 * @return vrai si une interruption doit être gérée
	 */

	private boolean testInterrupt() {
		for (int j = 0; j <= 4; ++j) {
			if (Bits.test(IF, j) && (Bits.test(IE, j))) {
				currentValueInterrupt = j;
				return true;
			}
		}
		return false;
	}

	/**
	 * @return un tableau contenant, dans l'ordre, la valeur des registres PC, SP,
	 *         A, F, B, C, D, E, H et L.
	 */
	public int[] _testGetPcSpAFBCDEHL() {
		int[] tab = new int[10];
		tab[0] = PC;
		tab[1] = SP;
		for (Reg o : Reg.values()) {
			tab[o.index() + 2] = registerFile.get(o);
		}
		return tab;
	}

	/**
	 * construit un tableau de familles indexé par les 256 opcodes possibles
	 */
	private static Opcode[] buildOpcodeTable(Opcode.Kind k) {
		Opcode[] OpcodeTable = new Opcode[256];
		for (Opcode o : Opcode.values()) {
			if (k == o.kind)
				OpcodeTable[o.encoding] = o;
		}
		return OpcodeTable;
	}

	/**
	 * étant donné un octet contenant un opcode, exécute l'instruction
	 * correspondante — en lisant ou écrivant, au besoin, des valeurs depuis le bus
	 * ou les registres.
	 */
	private void dispatch(Opcode o) {

		switch (o.family) {
		case NOP: {
		}
			break;
		case LD_R8_HLR: {
			Reg r = extractReg(o, 3);
			loadToRegister(r, Reg16.HL);
		}
			break;
		case LD_A_HLRU: {
			int HLplusIncrement = reg16(Reg16.HL) + extractHlIncrement(o);
			loadToRegister(Reg.A, Reg16.HL);
			setReg16(Reg16.HL, Bits.clip(16, HLplusIncrement));

		}
			break;
		case LD_A_N8R: {
			loadToRegister(Reg.A, AddressMap.REGS_START + read8AfterOpcode());
		}
			break;
		case LD_A_CR: {
			loadToRegister(Reg.A, AddressMap.REGS_START + registerFile.get(Reg.C));
		}
			break;
		case LD_A_N16R: {
			loadToRegister(Reg.A, read16AfterOpcode());
		}
			break;
		case LD_A_BCR: {
			loadToRegister(Reg.A, Reg16.BC);
		}
			break;
		case LD_A_DER: {
			loadToRegister(Reg.A, Reg16.DE);
		}
			break;
		case LD_R8_N8: {
			Reg r = extractReg(o, 3);
			registerFile.set(r, read8AfterOpcode());
		}
			break;
		case LD_R16SP_N16: {
			Reg16 r = extractReg16(o);
			setReg16SP(r, read16AfterOpcode());
		}
			break;
		case POP_R16: {
			Reg16 r = extractReg16(o);
			setReg16(r, pop16());
		}
			break;
		case LD_HLR_R8: {
			Reg r = extractReg(o, 0);
			loadToBusAddress(r, Reg16.HL);
		}
			break;
		case LD_HLRU_A: {
			int increment = extractHlIncrement(o);
			loadToBusAddress(Reg.A, Reg16.HL);
			setReg16(Reg16.HL, Bits.clip(16,reg16(Reg16.HL) + increment));
		}
			break;
		case LD_N8R_A: {
			loadToBusAddress(Reg.A, AddressMap.REGS_START + read8AfterOpcode());
		}
			break;
		case LD_CR_A: {
			loadToBusAddress(Reg.A, AddressMap.REGS_START + registerFile.get(Reg.C));
		}
			break;
		case LD_N16R_A: {
			loadToBusAddress(Reg.A, read16AfterOpcode());
		}
			break;
		case LD_BCR_A: {
			loadToBusAddress(Reg.A, Reg16.BC);
		}
			break;
		case LD_DER_A: {
			loadToBusAddress(Reg.A, Reg16.DE);
		}
			break;
		case LD_HLR_N8: {
			write8AtHl(read8AfterOpcode());
		}
			break;
		case LD_N16R_SP: {
			write16(read16AfterOpcode(), SP);
		}
			break;
		case LD_R8_R8: {
			Reg r = extractReg(o, 3);
			Reg s = extractReg(o, 0);
			if (r != s)
				registerFile.set(r, registerFile.get(s));
		}
			break;
		case LD_SP_HL: {
			SP = reg16(Reg16.HL);
		}
			break;
		case PUSH_R16: {
			Reg16 r = extractReg16(o);
			push16(reg16(r));
		}
			break;

		// Add
		case ADD_A_R8: {
			Reg r = extractReg(o, 0);
			int value = Alu.add(registerFile.get(r), registerFile.get(Reg.A), addSubCarry(o));
			setRegFlags(Reg.A, value);
		}
			break;
		case ADD_A_N8: {
			int value = Alu.add(registerFile.get(Reg.A), read8AfterOpcode(), addSubCarry(o));
			setRegFlags(Reg.A, value);
		}
			break;
		case ADD_A_HLR: {
			int value = Alu.add(registerFile.get(Reg.A), read8AtHl(), addSubCarry(o));
			setRegFlags(Reg.A, value);

		}
			break;
		case INC_R8: {
			Reg r = extractReg(o, 3);
			int value = Alu.add(registerFile.get(r), 1);
			setRegCombineAluFlags(r, value, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.CPU);
		}
			break;
		case INC_HLR: {
			int value = Alu.add(read8AtHl(), 1);
			combineAluFlags(value, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.CPU);
			write8AtHl(Alu.unpackValue(value));
		}
			break;
		case INC_R16SP: {
			Reg16 r = extractReg16(o);
			int value = (r == Reg16.AF) ? Alu.add16H(SP, 1) : Alu.add16H(reg16(r), 1);
			setReg16SP(r, Alu.unpackValue(value));

		}
			break;
		case ADD_HL_R16SP: {
			Reg16 r = extractReg16(o);
			int value = (r == Reg16.AF) ? Alu.add16H(reg16(Reg16.HL), SP) : Alu.add16H(reg16(Reg16.HL), reg16(r));
			combineAluFlags(value, FlagSrc.CPU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.ALU);
			setReg16SP(Reg16.HL, Alu.unpackValue(value));
		}
			break;
		case LD_HLSP_S8: {
			int value = Alu.add16L(SP, Bits.clip(16, Bits.signExtend8(read8AfterOpcode())));
			setFlags(value);
			value = Alu.unpackValue(value);
			if (Bits.test(o.encoding, 4))
				setReg16(Reg16.HL, value);
			else
				SP = value;
		}
			break;

		// Subtract
		case SUB_A_R8: {
			Reg r = extractReg(o, 0);
			setRegFlags(Reg.A, Alu.sub(registerFile.get(Reg.A), registerFile.get(r), addSubCarry(o)));
		}
			break;
		case SUB_A_N8: {
			setRegFlags(Reg.A, Alu.sub(registerFile.get(Reg.A), read8AfterOpcode(), addSubCarry(o)));
		}
			break;
		case SUB_A_HLR: {
			setRegFlags(Reg.A, Alu.sub(registerFile.get(Reg.A), read8AtHl(), addSubCarry(o)));
		}
			break;
		case DEC_R8: {
			Reg r = extractReg(o, 3);
			int value = Alu.sub(registerFile.get(r), 1);
			setRegCombineAluFlags(r, value, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.CPU);
		}
			break;
		case DEC_HLR: {
			int value = Alu.sub(read8AtHl(), 1);
			combineAluFlags(value, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.CPU);
			write8AtHl(Alu.unpackValue(value));
		}
			break;
		case CP_A_R8: {
			Reg r = extractReg(o, 0);
			int value = Alu.sub(registerFile.get(Reg.A), registerFile.get(r));
			setFlags(value);
		}
			break;
		case CP_A_N8: {
			int value = Alu.sub(registerFile.get(Reg.A), read8AfterOpcode());
			setFlags(value);
		}
			break;
		case CP_A_HLR: {
			int value = Alu.sub(registerFile.get(Reg.A), read8AtHl());
			setFlags(value);
		}
			break;
		case DEC_R16SP: {
			Reg16 r = extractReg16(o);
			int value = (r == Reg16.AF) ? SP : reg16(r);
			setReg16SP(r, Bits.clip(16, --value));

		}
			break;

		// And, or, xor, complement
		case AND_A_N8: {
			ANDandStoreInRegA(read8AfterOpcode());
		}
			break;
		case AND_A_R8: {
			Reg r = extractReg(o, 0);
			ANDandStoreInRegA(registerFile.get(r));
		}
			break;
		case AND_A_HLR: {
			ANDandStoreInRegA(read8AtHl());
		}
			break;
		case OR_A_R8: {
			Reg r = extractReg(o, 0);
			ORandStoreInRegA(registerFile.get(r));
		}
			break;
		case OR_A_N8: {
			ORandStoreInRegA(read8AfterOpcode());
		}
			break;
		case OR_A_HLR: {
			ORandStoreInRegA(read8AtHl());
		}
			break;
		case XOR_A_R8: {
			Reg r = extractReg(o, 0);
			XORandStoreInRegA(registerFile.get(r));
		}
			break;
		case XOR_A_N8: {
			XORandStoreInRegA(read8AfterOpcode());

		}
			break;
		case XOR_A_HLR: {
			XORandStoreInRegA(read8AtHl());
		}
			break;
		case CPL: {
			int v = Bits.complement8(registerFile.get(Reg.A));
			registerFile.set(Reg.A, v);
			combineAluFlags(v, FlagSrc.CPU, FlagSrc.V1, FlagSrc.V1, FlagSrc.CPU);
		}
			break;

		// Rotate, shift
		case ROTCA: {
			int valA = registerFile.get(Reg.A);
			valA = Alu.rotate(extractRotDir(o), valA);
			setRegCombineAluFlags(Reg.A, valA, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
		}
			break;
		case ROTA: {
			int valA = registerFile.get(Reg.A);
			valA = Alu.rotate(extractRotDir(o), valA, registerFile.testBit(Reg.F, Alu.Flag.C));
			setRegCombineAluFlags(Reg.A, valA, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
		}
			break;
		case ROTC_R8: {
			Reg r = extractReg(o, 0);
			int v = registerFile.get(r);
			setRegFlags(r, Alu.rotate(extractRotDir(o), v));
		}
			break;
		case ROT_R8: {
			Reg r = extractReg(o, 0);
			int v = registerFile.get(r);
			setRegFlags(r, Alu.rotate(extractRotDir(o), v, registerFile.testBit(Reg.F, Alu.Flag.C)));
		}
			break;
		case ROTC_HLR: {
			int v = Alu.rotate(extractRotDir(o), read8AtHl());
			write8AtHlAndSetFlags(v);

		}
			break;
		case ROT_HLR: {
			int v = Alu.rotate(extractRotDir(o), read8AtHl(), registerFile.testBit(Reg.F, Alu.Flag.C));
			write8AtHlAndSetFlags(v);

		}
			break;
		case SWAP_R8: {
			Reg r = extractReg(o, 0);
			setRegFlags(r, Alu.swap(registerFile.get(r)));
		}
			break;
		case SWAP_HLR: {
			int v = Alu.swap(read8AtHl());
			write8AtHlAndSetFlags(v);
		}
			break;
		case SLA_R8: {
			Reg r = extractReg(o, 0);
			setRegFlags(r, Alu.shiftLeft(registerFile.get(r)));
		}
			break;
		case SRA_R8: {
			Reg r = extractReg(o, 0);
			setRegFlags(r, Alu.shiftRightA(registerFile.get(r)));
		}
			break;
		case SRL_R8: {
			Reg r = extractReg(o, 0);
			setRegFlags(r, Alu.shiftRightL(registerFile.get(r)));
		}
			break;
		case SLA_HLR: {
			int v = Alu.shiftLeft(read8AtHl());
			write8AtHlAndSetFlags(v);
		}
			break;
		case SRA_HLR: {
			int v = Alu.shiftRightA(read8AtHl());
			write8AtHlAndSetFlags(v);
		}
			break;
		case SRL_HLR: {
			int v = Alu.shiftRightL(read8AtHl());
			write8AtHlAndSetFlags(v);
		}
			break;

		// Bit test and set
		case BIT_U3_R8: {
			int n = extractN3Index(o);
			Reg r = extractReg(o, 0);
			testAndStoreBIT(registerFile.get(r), n);
		}
			break;
		case BIT_U3_HLR: {
			int n = extractN3Index(o);
			testAndStoreBIT(read8AtHl(), n);
		}
			break;
		case CHG_U3_R8: {
			Reg r = extractReg(o, 0);
			int v = valueOfCHG(o, registerFile.get(r));
			registerFile.set(r, v);
		}
			break;
		case CHG_U3_HLR: {
			int v = valueOfCHG(o, read8AtHl());
			write8AtHl(v);
		}
			break;

		// Misc. ALU
		case DAA: {
			int f = registerFile.get(Reg.F);
			int value = Alu.bcdAdjust(registerFile.get(Reg.A), Bits.test(f, 6), Bits.test(f, 5), Bits.test(f, 4));
			setRegCombineAluFlags(Reg.A, value, FlagSrc.ALU, FlagSrc.CPU, FlagSrc.V0, FlagSrc.ALU);
		}
			break;
		case SCCF: {
			if (isCCF(o) && Bits.test(registerFile.get(Reg.F), 4)) {
				combineAluFlags(0, FlagSrc.CPU, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0);
			} else {
				combineAluFlags(0, FlagSrc.CPU, FlagSrc.V0, FlagSrc.V0, FlagSrc.V1);
			}
		}
			break;
		// Jumps
		case JP_HL: {
			nextPC = reg16(Reg16.HL);
		}
			break;
		case JP_N16: {
			nextPC = read16AfterOpcode();

		}
			break;

		case JP_CC_N16: {
			if (extractConditionAndTest(o)) {
				nextPC = read16AfterOpcode();
				nextNonIdleCycle += o.additionalCycles;
			}
		}
			break;
		case JR_E8: {
			nextPC += Bits.signExtend8(read8AfterOpcode());
		}

			break;
		case JR_CC_E8: {
			if (extractConditionAndTest(o)) {
				nextPC += Bits.signExtend8(read8AfterOpcode());
				nextNonIdleCycle += o.additionalCycles;
			}
		}
			break;

		// Calls and returns
		case CALL_N16: {
			push16(nextPC);
			nextPC = read16AfterOpcode();
		}
			break;
		case CALL_CC_N16: {
			if (extractConditionAndTest(o)) {
				push16(nextPC);
				nextPC = read16AfterOpcode();
				nextNonIdleCycle += o.additionalCycles;
			}
		}
			break;
		case RST_U3: {
			push16(nextPC);
			nextPC = AddressMap.RESETS[Bits.extract(o.encoding, 3, 3)];

		}
			break;
		case RET: {
			nextPC = pop16();
		}
			break;
		case RET_CC: {
			if (extractConditionAndTest(o)) {
				nextPC = pop16();
				nextNonIdleCycle += o.additionalCycles;
			}
		}
			break;
		// Interrupts
		case EDI: {
			IME = Bits.test(o.encoding, 3);
		}
			break;
		case RETI: {
			IME = true;
			nextPC = pop16();
		}
			break;
		// Misc control
		case HALT: {
			nextNonIdleCycle = Long.MAX_VALUE;
		}
			break;
		case STOP:
			throw new Error("STOP is not implemented");
		default:
			break;
		}

	}

	/**
	 * Stocke dans le registre 8 bits la valeur stockée à l'addresse contenue dans
	 * le register 16 bits
	 * 
	 * 
	 * @param r
	 *            : registre 8 bits dans lequel va être stocker la valeur
	 * @param r16
	 *            : registre 16 bits à partir duquel va être extraite l'addresse de
	 *            la future valeur
	 */
	private void loadToRegister(Reg r, Reg16 r16) {
		int address = reg16(r16);
		registerFile.set(r, read8(address));
	}

	/**
	 * Stocke dans le registre r, la valeur stockée à l'addresse donnée
	 * 
	 * @param r
	 *            : registre 8 bits dans lequel va être stocker la valeur
	 * @param address
	 *            : addresse à laquelle on va aller chercher la nouvelle valeur de r
	 * 
	 */
	private void loadToRegister(Reg r, int address) {
		registerFile.set(r, read8(address));
	}

	/**
	 * Écrit dans le bus, à l'addresse contenue dans r16, la valeur stockée dans r
	 * 
	 * @param r
	 *            : registre 8 bits dont on va extraire la valeur à stocker
	 * @param r16
	 *            : registre 16 bits qui va nous donner l'addresse à laquelle on va
	 *            écrire dans le bus
	 */
	private void loadToBusAddress(Reg r, Reg16 r16) {
		int value = registerFile.get(r);
		write8(reg16(r16), value);
	}

	/**
	 * Écrit dans le bus, à l'addresse donnée, la valeur stockée dans r
	 * 
	 * @param r
	 *            : registre 8 bits dont on va extraire la valeur à stocker
	 * @param address
	 *            : addresse à laquelle on va écrire dans le bus
	 */
	private void loadToBusAddress(Reg r, int address) {
		int value = registerFile.get(r);
		write8(address, value);
	}

	/**
	 * Teste si un bit d'index donné vaut 0 ou 1 et stocke le résultat dans le
	 * fanion Z, qui vaut 1 ssi le bit en question vaut 0.
	 * 
	 * @param value
	 *            : valeur à tester
	 * @param index
	 *            : index du bit à tester
	 */
	private void testAndStoreBIT(int value, int index) {
		if (Bits.test(value, index)) {
			combineAluFlags(0, FlagSrc.V0, FlagSrc.V0, FlagSrc.V1, FlagSrc.CPU);
		} else {
			combineAluFlags(0, FlagSrc.V1, FlagSrc.V0, FlagSrc.V1, FlagSrc.CPU);
		}
	}

	/**
	 * Réalise A &= value et stocke les fanions Z010
	 * 
	 * @param value
	 *            : valeur 8 bits
	 */
	private void ANDandStoreInRegA(int value) {
		int A = registerFile.get(Reg.A);
		setRegFlags(Reg.A, Alu.and(A, value));
	}

	/**
	 * Réalise A |= value et stocke les fanions Z000
	 * 
	 * @param value
	 *            : valeur 8 bits
	 */
	private void ORandStoreInRegA(int value) {
		int A = registerFile.get(Reg.A);
		setRegFlags(Reg.A, Alu.or(A, value));
	}

	/**
	 * Réalise A ^= value et stocke les fanions Z000
	 * 
	 * @param value
	 *            : valeur 8 bits
	 */
	private void XORandStoreInRegA(int value) {
		int A = registerFile.get(Reg.A);
		setRegFlags(Reg.A, Alu.xor(A, value));
	}

	/**
	 * Retourne value |= 1 << n pour les instructions SET ou bien value &= ~(1 << n)
	 * pour les instructions RES
	 * 
	 * @param o
	 *            : opcode de famille SET ou RES
	 * @param value
	 *            : valeur 8 bits
	 * @return : l'entier résulant de l'opération définie ci-dessus
	 */
	private int valueOfCHG(Opcode o, int value) {
		int n = extractN3Index(o);
		return (isSet(o)) ? value | Bits.mask(n) : value & ~(Bits.mask(n));
	}

	/**
	 * Retourne la valeur 8 bits contenue à l'adresse donnée,
	 * 
	 * @param address
	 *            : addresse de 16 bits à laquelle on va lire la valeur
	 * @return la valeur 8 bits lue,
	 */
	private int read8(int address) {
		return bus.read(address);
	}

	/**
	 * @return la valeur 8 bits à l'adresse contenue dans la paire de registres HL,
	 */
	private int read8AtHl() {
		return read8(reg16(Reg16.HL));
	}

	/**
	 * @return la valeur 8 bits à l'adresse suivant celle contenue dans le compteur
	 *         de programme, c-à-d à l'adresse PC+1,
	 */
	private int read8AfterOpcode() {
		return read8(PC + 1);
	}

	/**
	 * Retourne la valeur 16 bits contenue à l'adresse donnée (et à l'addresse
	 * suivante),
	 * 
	 * @param address
	 *            : addresse de 16 bits à laquelle on va lire la valeur
	 * @return la valeur lue,
	 */
	private int read16(int address) {
		return Bits.make16(bus.read(address + 1), bus.read(address));
	}

	/**
	 * @return la valeur 16 bits à l'adresse suivant celle contenue dans le compteur
	 *         de programme, c-à-d à l'adresse PC+1,
	 */
	private int read16AfterOpcode() {
		return read16(PC + 1);
	}

	/**
	 * Écrit sur le bus, à l'adresse donnée, la valeur 8 bits donnée,
	 * 
	 * @param address
	 *            : addresse de 16 bits à laquelle on va stocker la valeur
	 * @param v
	 *            : entier de 8 bits qui va être stocké
	 */
	private void write8(int address, int v) {
		bus.write(address, v);
	}

	/**
	 * Écrit sur le bus, à l'adresse donnée, la valeur 16 bits donnée,
	 * 
	 * @param address
	 *            : addresse de 16 bits à laquelle on va stocker la valeur
	 * @param v
	 *            : entier de 16 bits qui va être stocké
	 */
	private void write16(int address, int v) {
		write8(address, Bits.clip(8, v));
		write8(address + 1, Bits.extract(v, 8, 8));
	}

	/**
	 * Écrit sur le bus, à l'adresse contenue dans la paire de registres HL, la
	 * valeur 8 bits donnée,
	 * 
	 * @param v
	 *            : entier de 8 bits qui va être stocké
	 */
	private void write8AtHl(int v) {
		int address = reg16(Reg16.HL);
		write8(address, v);
	}

	/**
	 * Décrémente l'adresse contenue dans le pointeur de pile (registre SP) de 2
	 * unités, puis écrit à cette nouvelle adresse la valeur 16 bits donnée,
	 * 
	 * @param v
	 *            : entier de 16 bits qui va être stocké à l'addresse SP
	 */
	private void push16(int v) {
		SP = Bits.clip(16, SP - 2);
		write16(SP, v);
	}

	/**
	 * @return la valeur 16 bits à l'adresse contenue dans le pointeur de pile
	 *         (registre SP), puis l'incrémente de 2 unités.
	 */
	private int pop16() {
		int i = read16(SP);
		SP = Bits.clip(16, SP + 2);
		return i;
	}

	/**
	 * Retourne la valeur contenue dans la paire de registres donnée,
	 * 
	 * @param r
	 *            : registre 16 bits duquel on va extraire la valeur
	 * @return la valeur du registre
	 */
	private int reg16(Reg16 r) {
		int i = r.index();
		return Bits.make16(registerFile.get(register_16_TO_register_8[i][0]),
				registerFile.get(register_16_TO_register_8[i][1]));
	}

	/**
	 * Modifie la valeur contenue dans la paire de registres donnée, en faisant
	 * attention de mettre à 0 les bits de poids faible si la paire en question est
	 * AF,
	 * 
	 * @param r
	 *            : registre 16 bits dans lequel on va stocker newV
	 * @param newV:
	 *            valeur de 16 bits que l'on va stocker dans r
	 */
	private void setReg16(Reg16 r, int newV) {
		newV = Preconditions.checkBits16(newV);
		int v = (r == Reg16.AF) ? (newV >>> 4) << 4: newV;
		registerFile.set(register_16_TO_register_8[r.index()][0], Bits.extract(v, 8, 8));
		registerFile.set(register_16_TO_register_8[r.index()][1], Bits.clip(8, v));
	}

	/**
	 * Fait la même chose que setReg16 sauf dans le cas où la paire passée est AF,
	 * auquel cas le registre SP est modifié en lieu et place de la paire AF.
	 * 
	 * @param r
	 *            : registre 16 bits dans lequel on va stocker newV
	 * @param newV
	 *            : valeur de 16 bits que l'on va stocker dans r
	 */
	private void setReg16SP(Reg16 r, int newV) {
		if (r == Reg16.AF) {
			SP = Preconditions.checkBits16(newV);
		} else {
			setReg16(r, newV);
		}
	}

	/**
	 * Retourne l'identité du registre 8 bits correspondant à l'opcode donné
	 * 
	 * @param opcode
	 *            : opcode à partir duquel on va extraire le registre 8 bits
	 * @param startBit
	 *            : bit à partir du quel on commence l'extraction
	 * @return l'identité d'un registre 8 bits
	 */
	private Reg extractReg(Opcode opcode, int startBit) {
		int codeReg = Bits.extract(opcode.encoding, startBit, 3);
		return BINARY_TO_register_8[codeReg];
	}

	/**
	 * Retourne la même chose que extractReg mais pour les paires de registres (ne
	 * prend pas le paramètre startBit car il vaut 4 pour toutes les instructions du
	 * processeur),
	 * 
	 * @param opcode
	 *            : opcode dont on désire extraire le registre 16 bits
	 * @return l'identité d'un registre 16 bits
	 */
	private Reg16 extractReg16(Opcode opcode) {
		int codeReg = Bits.extract(opcode.encoding, 4, 2);
		return BINARY_TO_register_16[codeReg];
	}

	/**
	 * Retourne -1 ou +1 en fonction du bit d'index 4, est utilisé pour encoder
	 * l'incrémentation ou la décrémentation de la paire HL dans différentes
	 * instructions.
	 * 
	 * @param opcode
	 *            : opcode de famille contenant le nom HLRU.
	 * @return -1 ou +1 en fonction du bit d'index 4,
	 */
	private int extractHlIncrement(Opcode opcode) {
		return Bits.test(opcode.encoding, 4) ? -1 : 1;
	}

	/**
	 * Extrait la valeur stockée dans la paire donnée et la place dans le registre
	 * donné,
	 * 
	 * @param r
	 *            : registre 8 bits dans lequel va être placé la valeur
	 * @param vf
	 *            : entier contenant une paire valeur/fanions retournée par l'une
	 *            des méthodes de la classe Alu
	 */
	private void setRegFromAlu(Reg r, int vf) {
		registerFile.set(r, Alu.unpackValue(vf));
	}

	/**
	 * Extrait les fanions stockés dans la paire donnée et les place dans le
	 * registre F,
	 * 
	 * @param valueFlags
	 *            : entier contenant une paire valeur/fanions retournée par l'une
	 *            des méthodes de la classe Alu
	 */
	private void setFlags(int valueFlags) {
		registerFile.set(Reg.F, Alu.unpackFlags(valueFlags));
	}

	/**
	 * Combine les effets de setRegFromAlu et setFlags,
	 * 
	 * @param r
	 *            : registre 8 bits dans lequel va être placé la valeur
	 * @param vf
	 *            : entier contenant une paire valeur/fanions retournée par l'une
	 *            des méthodes de la classe Alu
	 */
	private void setRegFlags(Reg r, int vf) {
		setRegFromAlu(r, vf);
		setFlags(vf);
	}

	/**
	 * Extrait la valeur stockée dans la paire donnée et l'écrit sur le bus à
	 * l'adresse contenue dans la paire de registres HL , puis extrait les fanions
	 * stockés dans la paire et les place dans le registre
	 * 
	 * @param vf
	 *            : entier contenant une paire valeur/fanions retournée par l'une
	 *            des méthodes de la classe Alu F.
	 */
	private void write8AtHlAndSetFlags(int vf) {
		write8AtHl(Alu.unpackValue(vf));
		setFlags(vf);
	}

	/**
	 * Combine les fanions stockés dans le registre F avec ceux contenus dans la
	 * paire vf, en fonction des quatre derniers paramètres, qui correspondent
	 * chacun à un fanion, et stocke le résultat dans le registre F.
	 * 
	 * @param vf
	 *            : entier contenant une paire valeur/fanions retournée par l'une
	 *            des méthodes de la classe Alu
	 * @param z
	 *            : origine du fanion z
	 * @param n
	 *            : origine du fanion n
	 * @param h
	 *            : origine du fanion h
	 * @param c
	 *            : origine du fanion c
	 */
	private void combineAluFlags(int vf, FlagSrc z, FlagSrc n, FlagSrc h, FlagSrc c) {
		vf = Alu.unpackFlags(vf);
		int flags = switchFlagsSrc(vf, z, 7) + switchFlagsSrc(vf, n, 6) + switchFlagsSrc(vf, h, 5)
				+ switchFlagsSrc(vf, c, 4);
		registerFile.set(Reg.F, flags);
	}

	/**
	 * Combine les effets de setRegFromAlu et combineAluFlag
	 * 
	 * @param r
	 *            : registre 8 bits dans lequel on va stocker la valeur
	 * @param vf
	 *            : entier contenant une paire valeur/fanions retournée par l'une
	 *            des méthodes de la classe Alu
	 * @param z
	 *            : origine du fanion z
	 * @param n
	 *            : origine du fanion n
	 * @param h
	 *            : origine du fanion h
	 * @param c
	 *            : origine du fanion c
	 */
	private void setRegCombineAluFlags(Reg r, int vf, FlagSrc z, FlagSrc n, FlagSrc h, FlagSrc c) {
		setRegFromAlu(r, vf);
		combineAluFlags(vf, z, n, h, c);
	}

	/**
	 * Retourne un masque d'index correspondant au fanion. Cette méthode est
	 * utilisée pour alléger l'exécution de la méthode combineAluFlags
	 * 
	 * @param v
	 *            : valeur correspondant à un entier (bits d'index >= 8) et à ces
	 *            fanions (bits 0 à 8)
	 * @param f
	 *            : origine du fanion
	 * @param index
	 *            : index servant à déterminre de quel fanion on parle
	 * @return un masque d'index correspondant au fanion.
	 */
	private int switchFlagsSrc(int v, FlagSrc f, int index) {

		switch (f.index()) {
		case 0: {
			return 0 << index;
		}
		case 1: {
			return 1 << index;
		}
		case 2: {
			return (Bits.mask(index) & v);
		}
		case 3: {
			return (Bits.mask(index) & registerFile.get(Reg.F));
		}
		}
		return 0;
	}

	/**
	 * Extrait la direction de rotation de l'opcode donné (pour des opcodes
	 * correspondants à des familles de rotation)
	 * 
	 * @param o
	 *            : opcode des famille de rotation dont on va extraire le sens de
	 *            rotation
	 * @return : la direction de rotation (bit 3), pour toutes les familles
	 *         regroupant des instructions de rotation à gauche et à droite,
	 */
	private Alu.RotDir extractRotDir(Opcode o) {
		return Bits.test(o.encoding, 3) ? Alu.RotDir.RIGHT : Alu.RotDir.LEFT;
	}

	/**
	 * Extrait la valeur 3 bits attachée à l'opcode (du type 0bXXnnnXXX)
	 * 
	 * @param o
	 *            : opcode dont on va extraire la valeur 3 bits
	 * @return : une valeur 3 bits corresondants aux bits 3,4 et 5 de l'opcode
	 */
	private int extractN3Index(Opcode o) {
		return Bits.extract(o.encoding, 3, 3);
	}

	/**
	 * Teste si l'opcode correspond à la famille SET
	 * 
	 * @param o
	 *            : opcode de famille SET ou RES
	 * @return true si l'opcode correspond à la famille SET
	 */
	private boolean isSet(Opcode o) {
		return Bits.test(o.encoding, 6);
	}

	/**
	 * Teste si l'opcode correspond à la famille CCF
	 * 
	 * @param o
	 *            : opcode de famille SCF ou CCF
	 * @return true si l'opcode correspond à la famille CCF
	 */
	private boolean isCCF(Opcode o) {
		return Bits.test(o.encoding, 3);
	}

	/**
	 * Teste si l'opcode donné correspond à une instruction add ou sub avec carry
	 * 
	 * @param o
	 *            : opcode de l'instruction du type ADD ou SUB
	 * @return true si l'opcode donné correspond à une instruction add ou sub avec
	 *         carry
	 */
	private boolean addSubCarry(Opcode o) {
		return Bits.test(registerFile.get(Reg.F), 4) && Bits.extract(o.encoding, 3, 1) == 1;
	}

	/**
	 * Lève l'interruption donnée, c-à-d met à 1 le bit correspondant dans le
	 * registre IF.
	 * 
	 * @param i
	 *            : correspond à l'interruption définie pour le type énuméré
	 */
	public void requestInterrupt(Interrupt i) {
		IF = Bits.set(IF, i.index(), true);
	}

	/**
	 * Teste si il y a si la condition attachée aux instructions conditionnelles est
	 * vraie
	 * 
	 * @param o
	 *            : opcode dont on va extraire la condition
	 * @return vrai si la condition sur le fanion F est vraie (au préalablement
	 *         extraite de l'opcode)
	 */
	private boolean extractConditionAndTest(Opcode o) {
		int f = registerFile.get(Reg.F);
		switch (BINARY_TO_CONDITION[Bits.extract(o.encoding, 3, 2)]) {
		case NZ: {
			return !(Bits.test(f, Flag.Z));
		}
		case Z: {
			return Bits.test(f, Flag.Z);
		}
		case NC: {
			return !(Bits.test(f, Flag.C));
		}
		case C: {
			return Bits.test(f, Flag.C);
		}
		default:
			return false;
		}

	}
}
