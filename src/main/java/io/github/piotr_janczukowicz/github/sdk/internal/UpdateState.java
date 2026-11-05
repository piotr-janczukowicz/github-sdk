package io.github.piotr_janczukowicz.github.sdk.internal;

/** Tracks whether an update builder has any field set, so an empty update is refused before a request is made. */
public final class UpdateState {

    private boolean isEmpty;

    /** Creates a state in which no field is set. */
    public UpdateState() {
        this.isEmpty = true;
    }

    /** Records that a field was set. */
    public void mark() {
        isEmpty = false;
    }

    /**
     * Refuses an update with no field set.
     *
     * @throws IllegalStateException if no field was set
     */
    public void requireChange() {
        if (isEmpty) {
            throw new IllegalStateException("There is nothing to update");
        }
    }
}
