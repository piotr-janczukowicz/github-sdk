package io.github.piotr_janczukowicz.github.sdk.organizations;

/** Which repositories of an organization can read an Actions variable. */
public enum VariableVisibility {
    /** All repositories of the organization. */
    ALL,
    /** The private repositories of the organization. */
    PRIVATE
}
