package io.github.piotr_janczukowicz.github.sdk.internal;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * Gives the object packages of the SDK access to the wrapped {@link GitHub} client without exposing it in the public
 * API. This package is not exported by the module, so applications cannot name it. {@code Repository} and
 * {@code GitHubSdk} register the access when their classes are initialized, which always happens before an instance
 * exists. The package is hidden only when the module runs on the module path; on the classpath the accessors are
 * public, so the encapsulation is a convention and not a security boundary, and code in the same JVM is trusted.
 */
public final class Internals {

    private static final AtomicReference<@Nullable Function<Repository, GitHub>> REPOSITORY_ACCESS =
            new AtomicReference<>();
    private static final AtomicReference<@Nullable Function<GitHubSdk, GitHub>> SDK_ACCESS = new AtomicReference<>();

    private Internals() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Registers how to reach the client of a repository. Called by {@link Repository} when it is initialized. Only the
     * first registration counts, so code that calls this later cannot redirect the objects to another client.
     *
     * @param access the function that returns the client of a repository
     */
    public static void registerRepositoryAccess(final Function<Repository, GitHub> access) {
        REPOSITORY_ACCESS.compareAndSet(null, access);
    }

    /**
     * Registers how to reach the client of an SDK. Called by {@link GitHubSdk} when it is initialized. Only the first
     * registration counts, so code that calls this later cannot redirect the objects to another client.
     *
     * @param access the function that returns the client of an SDK
     */
    public static void registerSdkAccess(final Function<GitHubSdk, GitHub> access) {
        SDK_ACCESS.compareAndSet(null, access);
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
