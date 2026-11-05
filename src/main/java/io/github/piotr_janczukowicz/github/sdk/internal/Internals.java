package io.github.piotr_janczukowicz.github.sdk.internal;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Gives the object packages of the SDK access to the wrapped {@link GitHub} client without exposing it in the public
 * API. This package is not exported by the module, so applications cannot name it. {@code Repository} and
 * {@code GitHubSdk} register the access when their classes are initialized, which always happens before an instance
 * exists.
 */
public final class Internals {

    private static final AtomicReference<Function<Repository, GitHub>> REPOSITORY_ACCESS = new AtomicReference<>();
    private static final AtomicReference<Function<GitHubSdk, GitHub>> SDK_ACCESS = new AtomicReference<>();

    private Internals() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Registers how to reach the client of a repository. Called by {@link Repository} when it is initialized.
     *
     * @param access the function that returns the client of a repository
     */
    public static void registerRepositoryAccess(final Function<Repository, GitHub> access) {
        REPOSITORY_ACCESS.set(access);
    }

    /**
     * Registers how to reach the client of an SDK. Called by {@link GitHubSdk} when it is initialized.
     *
     * @param access the function that returns the client of an SDK
     */
    public static void registerSdkAccess(final Function<GitHubSdk, GitHub> access) {
        SDK_ACCESS.set(access);
    }

    /**
     * Returns the client behind a repository.
     *
     * @param repository the repository
     * @return the wrapped client
     * @throws NullPointerException if the repository access was never registered
     */
    public static GitHub github(final Repository repository) {
        return requireNonNull(REPOSITORY_ACCESS.get(), "repositoryAccess").apply(repository);
    }

    /**
     * Returns the client behind an SDK.
     *
     * @param sdk the SDK
     * @return the wrapped client
     * @throws NullPointerException if the SDK access was never registered
     */
    public static GitHub github(final GitHubSdk sdk) {
        return requireNonNull(SDK_ACCESS.get(), "sdkAccess").apply(sdk);
    }
}
