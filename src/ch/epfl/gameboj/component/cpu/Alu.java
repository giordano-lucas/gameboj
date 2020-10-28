package ch.epfl.gameboj.component.cpu;

import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.bits.Bit;
import java.util.Objects;
import ch.epfl.gameboj.Preconditions;

public final class Alu {

	private Alu() {

	}

	/**
	 * @author lucas Type énuméré représentant la valeur des 8 différents bits d'une
	 *         valeur du registre F de CPU
	 *
	 */
	public enum Flag implements Bit {
		UNUSED_0, UNUSED_1, UNUSED_2, UNUSED_3, C, H, N, Z;
	}

	/**
	 * @author lucas
	 * Type enuméré représentant le sens de la direction d'une rotation (gauche ou droite)
	 */
	public enum RotDir {
		LEFT, RIGHT;
	}

	/**
	 * Retourne une valeur dont les bits correspondant aux différents fanions valent
	 * 1 ssi l'argument correspondant est vrai,
	 * 
	 * @param z
	 *            : fanion Z
	 * @param n
	 *            : fanion N
	 * @param h
	 *            : fanion H
	 * @param c
	 *            : fanion C
	 * @return le masque correspondant aux fanions
	 */

	public static int maskZNHC(boolean z, boolean n, boolean h, boolean c) {
		int bit = 0;
		if (z)
			bit += Flag.Z.mask();
		if (n)
			bit += Flag.N.mask();
		if (h)
			bit += Flag.H.mask();
		if (c)
			bit += Flag.C.mask();
		return bit;
	}

	/**
	 * Retourne la valeur contenue dans le paquet valeur/fanion donné
	 * 
	 * @param valueFlags
	 *            : entier correspondant à la combinaison valeur entière v et
	 *            fanions (du type v_ZNHC0000)
	 * @return la valeur sans les fanions,
	 */

	public static int unpackValue(int valueFlags) {
		return Bits.extract(valueFlags, 8, 24);
	}

	/**
	 * Retourne les fanions contenus dans le paquet valeur/fanion donné,
	 * 
	 * @param valueFlags
	 *            : entier correspondant à la combinaison valeur entière v et
	 *            fanions (du type v_ZNHC0000)
	 * @return les fanions contenus dans valueFlags,
	 */

	public static int unpackFlags(int valueFlags) {
		return Bits.clip(8, valueFlags);
	}

	/**
	 * Retourne la somme des deux valeurs 8 bits données et du bit de retenue
	 * initial c0, et les fanions Z0HC,
	 * 
	 * @param l
	 *            : entier de 8 bits à sommer
	 * @param r
	 *            : entier de 8 bits à sommer
	 * @param c0
	 *            : bit de retenue, vaut 0 ssi c0 est false;
	 * @return la somme de l, de r et de c0 plus les fanions
	 */
	public static int add(int l, int r, boolean c0) {
		l = Preconditions.checkBits8(l);
		r = Preconditions.checkBits8(r);
		int sum = l + r;
		int c = (c0) ? 1 : 0;
		int finalSum = Bits.clip(8, sum + c);
		return packValueZNHC(finalSum, finalSum == 0, false,
				(Bits.clip(4, l) + Bits.clip(4, r) + c) > 0xF, (sum + c) > 0xFF);
	}

	/**
	 * Fonctionnement identique à add(int l, int r, boolean c0) mais avec false
	 * comme dernier argument
	 * 
	 * @param l
	 *            : entier à sommer
	 * @param r
	 *            : entier à sommer
	 * @return la somme de l et r plus les fanions
	 */
	public static int add(int l, int r) {
		return add(l, r, false);
	}

	/**
	 * Retourne la somme des deux valeurs 16 bits données et les fanions 00HC, où H
	 * et C sont les fanions correspondant à l'addition des 8 bits de poids faible,
	 * 
	 * @param l
	 *            : entier à sommer de 16 bits
	 * @param r
	 *            : entier à sommer de 16 bits
	 * @return la somme des deux valeurs 16 bits données et les fanions 00HC
	 */
	public static int add16L(int l, int r) {
		l = Preconditions.checkBits16(l);
		r = Preconditions.checkBits16(r);
		return packValueZNHC(Bits.clip(16, l + r), false, false, (Bits.clip(4, l) + Bits.clip(4, r)) > 0xF,
				(Bits.clip(8, l) + Bits.clip(8, r)) > 0xFF);
	}

	/**
	 * Fonctionnement identique à add16L, si ce n'est que les fanions H et C
	 * correspondent à l'addition des 8 bits de poids fort,
	 * 
	 * @param l
	 *            : entier à sommer de 16 bits
	 * @param r
	 *            : entier à sommer de 16 bits
	 * @return la somme des deux valeurs 16 bits données et les fanions 00HC
	 */
	public static int add16H(int l, int r) {
		l = Preconditions.checkBits16(l);
		r = Preconditions.checkBits16(r);
		return packValueZNHC(Bits.clip(16, l + r), false, false, (Bits.clip(12, l) + Bits.clip(12, r)) > 0xFFF,
				(l + r) > 0xFFFF);
	}

	/**
	 * Retourne la différence des valeurs de 8 bits données et du bit d'emprunt
	 * initial b0, et les fanions Z1HC
	 * 
	 * @param l
	 *            : entier de 8 bits dont on va retrancher r;
	 * @param r
	 *            : entier de 8 bits à retrancher à l;
	 * @param b0
	 *            : bit d'emprunt, vaut 1 ssi b0 est vrai
	 * @return la différence de l et de r et les fanions Z1HC
	 */
	public static int sub(int l, int r, boolean b0) {
		l = Preconditions.checkBits8(l);
		r = Preconditions.checkBits8(r);
		int result = l - r;
		int b = (b0) ? -1 : 0;
		int finalSub = Bits.clip(8, result + b);
		return packValueZNHC(finalSub, finalSub == 0, true,
				(Bits.clip(4, l) - Bits.clip(4, r) + b) < 0, (result + b) < 0);
	}

	/**
	 * Fonctionnement identique à sub(int l, int r, boolean c0) mais avec false
	 * comme dernier argument
	 * 
	 * @param l
	 *            : entier de 8 bits dont on va retrancher r;
	 * @param r
	 *            : entier de 8 bits à retrancher à l;
	 * @return la différence de l et de r et les fanions Z1HC
	 */
	public static int sub(int l, int r) {
		return sub(l, r, false);
	}

	/**
	 * Ajuste la valeur 8 bits donnée en argument afin qu'elle soit au format DCB
	 * 
	 * @param v
	 *            : valeur entière de 8 bits à transformer en DCB
	 * @param n
	 *            : fanion N
	 * @param h
	 *            : fanion H
	 * @param c
	 *            : fanion C
	 * @return la valeur 8 bits au format DCB
	 */
	public static int bcdAdjust(int v, boolean n, boolean h, boolean c) {
		v = Preconditions.checkBits8(v);

		boolean fixL = h || (!n && Bits.extract(v, 0, 4) > 9);
		boolean fixH = c || (!n && v > 0x99);
		int fix = 0x60 * booleanToBinary(fixH) + 0x06 * booleanToBinary(fixL);
		int va = (n) ? v - fix : v + fix;
		va = Bits.clip(8, va);
		return packValueZNHC(va, va == 0, n, false, fixH);

	}

	/**
	 * Retourne le « et » bit à bit des deux valeurs 8 bits données et les fanions
	 * Z010
	 * 
	 * @param l
	 *            : entier de 8 bits
	 * @param r
	 *            : entier de 8 bits
	 * @return le « et » bit à bit de l et r et les fanions Z010,
	 */
	public static int and(int l, int r) {
		l = Preconditions.checkBits8(l);
		r = Preconditions.checkBits8(r);
		int v = l & r;
		return packValueZNHC(v, v == 0, false,true, false); 
	}

	/**
	 * Retourne le « ou inclusif » bit à bit des deux valeurs 8 bits données et les
	 * fanions Z000,
	 * 
	 * @param l
	 *            : entier de 8 bits
	 * @param r
	 *            : entier de 8 bits
	 * @return le « ou inclusif » bit à bit de l et r et les fanions Z000,
	 */
	public static int or(int l, int r) {
		l = Preconditions.checkBits8(l);
		r = Preconditions.checkBits8(r);
		int v = l | r;
		return packValueZNHC(v, v == 0, false,false, false); 
	}

	/**
	 * Retourne le « ou exclusif » bit à bit des deux valeurs 8 bits données et les
	 * fanions Z000,
	 * 
	 * @param l
	 *            : entier de 8 bits
	 * @param r
	 *            : entier de 8 bits
	 * @return le « ou exclusif » bit à bit de l et r et les fanions Z000,
	 */
	public static int xor(int l, int r) {
		l = Preconditions.checkBits8(l);
		r = Preconditions.checkBits8(r);
		int v = l ^ r;
		return packValueZNHC(v, v == 0, false,false, false); 
	}

	/**
	 * Retourne la valeur 8 bits donnée décalée à gauche d'un bit, et les fanions
	 * Z00C où le fanion C contient le bit éjecté par le décalage (c-à-d que C est
	 * vrai ssi le bit en question valait 1),
	 * 
	 * @param v
	 *            : entier de 8 bits
	 * @return la valeur décallée à gauche et les fanions Z00C
	 */
	public static int shiftLeft(int v) {
		v = Preconditions.checkBits8(v);

		int temp = Bits.clip(8, (v << 1));
		return packValueZNHC(temp, temp == 0, false, false, (Bits.test(v, 7)));
	}

	/**
	 * Retourne la valeur 8 bits donnée décalée à droite d'un bit, de manière
	 * arithmétique, et les fanions Z00C où C contient le bit éjecté par le
	 * décalage,
	 * 
	 * @param v
	 *            : entier de 8 bits
	 * @return la valeur décalée à droite de manière arithmétique, et les fanions
	 *         Z00C
	 */
	public static int shiftRightA(int v) {
		v = Preconditions.checkBits8(v);
		v = Bits.set(v, 8, Bits.test(v, 7));
		return packValueZNHC((v >> 1), (v >> 1) == 0, false, false, (Bits.test(v, 0)));
	}

	/**
	 * Retourne la valeur 8 bits donnée décalée à droite d'un bit, de manière
	 * logique, et les fanions Z00C où C contient le bit éjecté par le décalage,
	 * 
	 * @param v
	 *            : entier de 8 bits
	 * @return la valeur décalée à droite, de manière logique, et les fanions Z00C
	 */
	public static int shiftRightL(int v) {
		v = Preconditions.checkBits8(v);
		return packValueZNHC(v >>> 1, (v >>> 1) == 0, false, false, (Bits.test(v, 0)));
	}

	/**
	 * Retourne la rotation de la valeur 8 bits donnée, d'une distance de un bit
	 * dans la direction donnée, et les fanions Z00C où C contient le bit qui est
	 * passé d'une extrémité à l'autre lors de la rotation,
	 * 
	 * @param d
	 *            : entier correspodant à la valeur de la rotation (nombre de
	 *            décallages)
	 * @param v
	 *            : entier de 8 bits auquel la rotation va être appliquée
	 * @return la rotation de la valeur et les fanions Z00C
	 */
	public static int rotate(RotDir d, int v) {
		v = Preconditions.checkBits8(v);
		int index = (d == RotDir.RIGHT) ? 7:0;
		v = realRotate(d, 8, v);
		return packValueZNHC(v, v == 0, false, false, Bits.test(v, index));
	}

	/**
	 * Retourne la rotation à travers la retenue, dans la direction donnée, de la
	 * combinaison de la valeur 8 bits et du fanion de retenue donnés, ainsi que les
	 * fanions Z00C ; cette opération consiste à construire une valeur 9 bits à
	 * partir de la retenue et de la valeur 8 bits, la faire tourner dans la
	 * direction donnée, puis retourner les 8 bits de poids faible comme résultat,
	 * et le bit de poids le plus fort comme nouvelle retenue (fanion C)
	 * 
	 * @param d
	 *            : entier correspodant à la valeur de la rotation (nombre de
	 *            décallages)
	 * @param v
	 *            : entier de 8 bits auquel la rotation va être appliquée
	 * @param c
	 *            : valeur booléenne valant 1 ssi c est vrai. Correspond au bit de
	 *            retenue expliqué ci-dessous
	 * @return la rotation à travers la retenue de la valeur et les fanions Z00C
	 */
	public static int rotate(RotDir d, int v, boolean c) {
		v = Preconditions.checkBits8(v);
		v += (c) ? Bits.mask(8):0;
		v = realRotate(d, 9, v);
		return packValueZNHC(Bits.extract(v, 0, 8), Bits.extract(v, 0, 8) == 0, false, false, Bits.test(v, 8));
	}
	
	
	private static int realRotate(RotDir d, int size, int v) {
		int direction = (d == RotDir.RIGHT) ? -1: 1;
		return v = Bits.rotate(size, v, direction);
		
	}

	/**
	 * Retourne la valeur obtenue en échangeant les 4 bits de poids faible et de
	 * poids fort de la valeur 8 bits donnée, et les fanions Z000,
	 * 
	 * @param v
	 *            : valeur de 8 bits
	 * @return la valeur obtenue après échange et les fanions Z000,
	 */
	public static int swap(int v) {
		v = Preconditions.checkBits8(v);
		v = Bits.rotate(8, v, 4);
		return packValueZNHC(v, v == 0, false, false, false);
	}

	/**
	 * Retourne la valeur 0 et les fanions Z010 où Z est vrai ssi le bit d'index
	 * donné de la valeur 8 bits donnée vaut 1
	 * 
	 * @param v
	 *            : valeur de 8 bits
	 * @param bitIndex
	 *            : index compris entre 0 et 7
	 * @return la valeur 0 et les fanions Z010
	 */

	public static int testBit(int v, int bitIndex) {
		v = Preconditions.checkBits8(v);
		bitIndex = Objects.checkIndex(bitIndex, 8);
		return packValueZNHC(0, !(Bits.test(v, bitIndex)), false, true, false);

	}

	/**
	 * Retourne le paquet constitué de v ( à partir du 8 bits) et des fanions ZNHC
	 * (bits 4 à 7) et de zéros (bits 0 à 3)
	 * 
	 * @param v
	 *            : valeur entière
	 * @param z
	 *            : fanion Z
	 * @param n
	 *            : fanion N
	 * @param h
	 *            : fanion H
	 * @param c
	 *            : fanion C
	 * @return la valeur et les fanions enpaquetés
	 */
	private static int packValueZNHC(int v, boolean z, boolean n, boolean h, boolean c) {
		return (v << 8) | (maskZNHC(z, n, h, c));
	}

	/**
	 * Permet de transformer une valeur booléenne en une valeur Binaire
	 * 
	 * @param b
	 *            : valeur du fanion
	 * @return 1 ssi b est vraie
	 */
	private static int booleanToBinary(boolean b) {
		return (b ? 1 : 0);
	}
}
