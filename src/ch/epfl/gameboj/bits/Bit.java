package ch.epfl.gameboj.bits;

public interface Bit {

	/**
	 * @return l'index de l'élément du type énuméré implémentant l'interface
	 *     
	 */
    int ordinal();
    
    /**
     * @return la même chose que ordinal()
     */
    default int index() {
        return this.ordinal();
    }
    
    /**
     * @return retourne le masque correspondant au bit
     */
    default int mask() {
        return Bits.mask(ordinal());
    }
}