module io.github.piotr_janczukowicz.githubsdk {
    requires transitive io.github.piotr_janczukowicz.githubrestclient;
    requires common.util;
    requires org.jspecify;

    exports io.github.piotr_janczukowicz.github.sdk;
}
