module io.github.piotr_janczukowicz.githubsdk {
    requires transitive io.github.piotr_janczukowicz.githubrestclient;
    requires common.util;
    requires org.jspecify;

    exports io.github.piotr_janczukowicz.github.sdk;
    exports io.github.piotr_janczukowicz.github.sdk.accounts;
    exports io.github.piotr_janczukowicz.github.sdk.actions;
    exports io.github.piotr_janczukowicz.github.sdk.checks;
    exports io.github.piotr_janczukowicz.github.sdk.collaborators;
    exports io.github.piotr_janczukowicz.github.sdk.content;
    exports io.github.piotr_janczukowicz.github.sdk.gists;
    exports io.github.piotr_janczukowicz.github.sdk.deploykeys;
    exports io.github.piotr_janczukowicz.github.sdk.deployments;
    exports io.github.piotr_janczukowicz.github.sdk.git;
    exports io.github.piotr_janczukowicz.github.sdk.issues;
    exports io.github.piotr_janczukowicz.github.sdk.metrics;
    exports io.github.piotr_janczukowicz.github.sdk.notifications;
    exports io.github.piotr_janczukowicz.github.sdk.organizations;
    exports io.github.piotr_janczukowicz.github.sdk.pages;
    exports io.github.piotr_janczukowicz.github.sdk.projects;
    exports io.github.piotr_janczukowicz.github.sdk.pulls;
    exports io.github.piotr_janczukowicz.github.sdk.security;
    exports io.github.piotr_janczukowicz.github.sdk.utilities;
    exports io.github.piotr_janczukowicz.github.sdk.reactions;
    exports io.github.piotr_janczukowicz.github.sdk.rulesets;
    exports io.github.piotr_janczukowicz.github.sdk.webhooks;
}
