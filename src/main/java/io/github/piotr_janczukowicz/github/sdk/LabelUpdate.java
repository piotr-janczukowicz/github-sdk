package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.issues.GitHubUpdateLabelPayload;
import java.io.IOException;
import org.jspecify.annotations.Nullable;

/**
 * Collects changes to a {@link Label} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}.
 */
public final class LabelUpdate {

    private final Label label;
    private @Nullable String name;
    private @Nullable String color;
    private @Nullable String description;
    private boolean isEmpty = true;

    LabelUpdate(final Label label) {
        this.label = label;
    }

    /**
     * Sets the new label name.
     *
     * @param name the new value
     * @return this update
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public LabelUpdate name(final String name) {
        this.name = requireNonNull(name, "name");
        return changed();
    }

    /**
     * Sets the new color as a hexadecimal value without the leading {@code #}.
     *
     * @param color the new value
     * @return this update
     * @throws NullPointerException if {@code color} is {@code null}
     */
    public LabelUpdate color(final String color) {
        this.color = requireNonNull(color, "color");
        return changed();
    }

    /**
     * Sets the new description.
     *
     * @param description the new value
     * @return this update
     * @throws NullPointerException if {@code description} is {@code null}
     */
    public LabelUpdate description(final String description) {
        this.description = requireNonNull(description, "description");
        return changed();
    }

    /**
     * Sends the changes. The object this update was made from is not changed.
     *
     * @return the label as GitHub returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public Label send() throws IOException {
        if (isEmpty) {
            throw new IllegalStateException("There is nothing to update");
        }
        return label.apply(new GitHubUpdateLabelPayload(name, color, description, null));
    }

    private LabelUpdate changed() {
        isEmpty = false;
        return this;
    }
}
