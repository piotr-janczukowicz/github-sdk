package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class UpdateStateTest {

    @Test
    void requireChange_shouldThrowIllegalStateException_whenNothingWasMarked() {
        final UpdateState sut = new UpdateState();

        final Throwable thrown = catchThrowable(sut::requireChange);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("There is nothing to update");
    }

    @Test
    void requireChange_shouldReturnNormally_whenAFieldWasMarked() {
        final UpdateState sut = new UpdateState();
        sut.mark();

        final Throwable thrown = catchThrowable(sut::requireChange);

        assertThat(thrown).isNull();
    }
}
