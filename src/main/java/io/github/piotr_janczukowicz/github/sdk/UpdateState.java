package io.github.piotr_janczukowicz.github.sdk;

/** Tracks whether an update builder has any field set, so an empty update is refused before a request is made. */
final class UpdateState {

    private boolean isEmpty;

    UpdateState() {
        this.isEmpty = true;
    }

    void mark() {
        isEmpty = false;
    }

    void requireChange() {
        if (isEmpty) {
            throw new IllegalStateException("There is nothing to update");
        }
    }
}
