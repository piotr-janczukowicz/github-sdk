module io.github.piotr_janczukowicz.githubsdk {
    requires transitive io.github.piotr_janczukowicz.githubrestclient;
    requires common.util;
    requires org.jspecify;

    exports io.github.piotr_janczukowicz.github.sdk;
    exports io.github.piotr_janczukowicz.github.sdk.actions;
    exports io.github.piotr_janczukowicz.github.sdk.checks;
    exports io.github.piotr_janczukowicz.github.sdk.collaborators;
    exports io.github.piotr_janczukowicz.github.sdk.content;
    exports io.github.piotr_janczukowicz.github.sdk.git;
    exports io.github.piotr_janczukowicz.github.sdk.issues;
    exports io.github.piotr_janczukowicz.github.sdk.pulls;
    exports io.github.piotr_janczukowicz.github.sdk.reactions;
    exports io.github.piotr_janczukowicz.github.sdk.webhooks;
}
