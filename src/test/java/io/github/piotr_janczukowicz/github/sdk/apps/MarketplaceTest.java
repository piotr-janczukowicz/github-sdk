package io.github.piotr_janczukowicz.github.sdk.apps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.apps.GitHubListAccountsForPlanFilter;
import io.github.piotr_janczukowicz.github.api.apps.GitHubMarketplaceListingPlan;
import io.github.piotr_janczukowicz.github.api.apps.GitHubMarketplacePurchase;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class MarketplaceTest {

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Marketplace sut = new Marketplace(GitHubSdk.of(github));

    @Test
    void plans_shouldStreamThePlans_whenTheyExist() throws IOException {
        final GitHubMarketplaceListingPlan plan = mock(GitHubMarketplaceListingPlan.class);
        when(github.apps().marketplace().listPlans(any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(plan));

        final List<GitHubMarketplaceListingPlan> plans = sut.plans().toList();

        assertThat(plans).containsExactly(plan);
    }

    @Test
    void accounts_shouldStreamThePurchasesOfThePlan_whenTheyExist() throws IOException {
        final GitHubMarketplacePurchase purchase = mock(GitHubMarketplacePurchase.class);
        when(github
                        .apps()
                        .marketplace()
                        .listAccountsForPlan(
                                eq(3L), any(GitHubListAccountsForPlanFilter.class), any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(purchase));

        final List<GitHubMarketplacePurchase> purchases = sut.accounts(3L).toList();

        assertThat(purchases).containsExactly(purchase);
    }

    @Test
    void purchase_shouldReturnThePurchase_whenTheAccountHasOne() throws IOException {
        final GitHubMarketplacePurchase purchase = mock(GitHubMarketplacePurchase.class);
        when(github.apps().marketplace().getSubscriptionPlanForAccount(5L)).thenReturn(Optional.of(purchase));

        final Optional<GitHubMarketplacePurchase> found = sut.purchase(5L);

        assertThat(found).containsSame(purchase);
    }

    @Test
    void purchase_shouldBeEmpty_whenTheAccountHasNone() throws IOException {
        when(github.apps().marketplace().getSubscriptionPlanForAccount(5L)).thenReturn(Optional.empty());

        final Optional<GitHubMarketplacePurchase> found = sut.purchase(5L);

        assertThat(found).isEmpty();
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenSdkIsNull() {
        final Throwable thrown = catchThrowable(() -> new Marketplace(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("sdk");
    }
}
