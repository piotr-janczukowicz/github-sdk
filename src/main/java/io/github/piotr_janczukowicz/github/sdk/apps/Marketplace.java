package io.github.piotr_janczukowicz.github.sdk.apps;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.apps.GitHubListAccountsForPlanFilter;
import io.github.piotr_janczukowicz.github.api.apps.GitHubMarketplaceListingPlan;
import io.github.piotr_janczukowicz.github.api.apps.GitHubMarketplacePurchase;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The GitHub Marketplace listing of the app the SDK is authenticated as: its plans and who bought them. It works only
 * for an app with a published listing and an App JWT; otherwise GitHub answers HTTP 401 or 404. The records are
 * returned as GitHub sends them. Nothing is requested until a method is called.
 */
public final class Marketplace {

    private final GitHubSdk sdk;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param sdk the SDK that makes the calls
     * @throws NullPointerException if an argument is {@code null}
     */
    public Marketplace(final GitHubSdk sdk) {
        this.sdk = requireNonNull(sdk, "sdk");
    }

    /**
     * Lists the pricing plans of the listing ({@code GET /marketplace_listing/plans}). The stream is lazy and can be
     * used once.
     *
     * @return the plans
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubMarketplaceListingPlan> plans() throws IOException {
        return github().apps().marketplace().listPlans(GitHubPageRequest.byPage()).stream();
    }

    /**
     * Lists the accounts that bought a plan ({@code GET /marketplace_listing/plans/{plan_id}/accounts}). The stream is
     * lazy and can be used once.
     *
     * @param planId the plan id
     * @return the purchases, in GitHub's default order
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubMarketplacePurchase> accounts(final long planId) throws IOException {
        return github()
                .apps()
                .marketplace()
                .listAccountsForPlan(
                        planId, new GitHubListAccountsForPlanFilter(null, null), GitHubPageRequest.byPage())
                .stream();
    }

    /**
     * Loads the purchase of one account ({@code GET /marketplace_listing/accounts/{account_id}}).
     *
     * @param accountId the id of the user or organization
     * @return the purchase, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<GitHubMarketplacePurchase> purchase(final long accountId) throws IOException {
        return github().apps().marketplace().getSubscriptionPlanForAccount(accountId);
    }

    private GitHub github() {
        return Internals.github(sdk);
    }
}
