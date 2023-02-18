package mod.chiselsandbits.api.chiseling;

/**
 * Represents the mode of operandus of the current chiseling context.
 */
public enum ChiselingOperation
{
    /**
     * The current operation is a chisel.
     * As such it removes the bit underneath the cursor.
     */
    CHISELING(false),

    /**
     * The current operation is a bit.
     * As such it places the bit underneath the cursor.
     */
    PLACING(true),

    /**
     * The current operation is an alteration.
     * It requires a bit to be held but might alter the bit underneath the cursor, or surrounding bits.
     */
    ALTERATION(true);

    private final boolean processesAir;

    ChiselingOperation(final boolean processesAir) {this.processesAir = processesAir;}

    /**
     * Indicates if this operation processes air.
     *
     * @return {@code true} if this operation processes air, {@code false} otherwise.
     */
    public boolean processesAir()
    {
        return processesAir;
    }

    /**
     * Indicates if the current mode is a chiseling mode.
     *
     * @return True when this is a chiseling mode, false otherwise.
     */
    public boolean isChiseling() {
        return this == CHISELING;
    }

    /**
     * Indicates if the current mode is a placing mode.
     *
     * @return True when this is a placing mode, false otherwise.
     */
    public boolean isPlacing() {
        return this == PLACING;
    }

    /**
     * Indicates if the current mode is an alteration mode.
     *
     * @return True when this is an alteration mode, false otherwise.
     */
    public boolean isAlteration() {
        return this == ALTERATION;
    }
}
