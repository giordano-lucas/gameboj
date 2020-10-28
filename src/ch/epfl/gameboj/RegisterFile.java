package ch.epfl.gameboj;

import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;

public final class RegisterFile<E extends Register> {

	private final byte[] register;

	/**
	 * construit le tableau register
	 * 
	 * @param allRegs
	 *            : tableau contenant tous les éléments destinés à devenir les
	 *            futurs registres
	 */
	public RegisterFile(E[] allRegs) {
		register = new byte[allRegs.length];
	}

	/**
	 * Renvoie l'octed stocké dans le registre reg
	 * 
	 * @param reg
	 *            : registre dont on veut extraire la valeur
	 * @return la valeur 8 bits contenue dans le registre donné, sous la forme d'un
	 *         entier compris entre 0 (inclus) et 0xFF (inclus),
	 */
	public int get(E reg) {
		return Byte.toUnsignedInt(register[reg.index()]);

	}

	/**
	 * modifie le contenu du registre donné pour qu'il soit égal à la valeur 8 bits
	 * donnée ; lève IllegalArgumentException si la valeur n'est pas une valeur 8
	 * bits valide,
	 * 
	 * @param reg
	 *            : registre dans lequel on veut stocker newValue
	 * @param newValue
	 *            : valeur de 8 bits que l'on désire stocker dans reg
	 */
	public void set(E reg, int newValue) {
		newValue = Preconditions.checkBits8(newValue);
		register[reg.index()] = (byte) newValue;

	}

	/**
	 * teste si le bit d'index déterminé par la valeur ordinale de b au sein de son
	 * type énuméré vaut 1
	 * 
	 * @param reg
	 *            : registre dont on veut tester la valeur
	 * @param b
	 *            : élément destiné à founir l'indice du bit à tester
	 * @return vrai si et seulement si le bit donné du registre donné vaut 1,
	 */
	public boolean testBit(E reg, Bit b) {
		return Bits.test(register[reg.index()], b);
	}

	/**
	 * modifie la valeur stockée dans le registre donné pour que le bit donné ait la
	 * nouvelle valeur donnée.
	 * 
	 * @param reg
	 *            : registre dans lequel on veut stocker newValue
	 * @param bit
	 *            : élément destiné à founir l'indice du bit à modifier (via l'index
	 *            de b au sein de son type énuméré)
	 * @param newValue
	 *            : nouvelle valeur du bit à modifier. Correspond à 1 ssi newValue
	 *            est vrai
	 */
	public void setBit(E reg, Bit bit, boolean newValue) {
		set(reg, Bits.set(get(reg), bit.index(), newValue));
	}
}