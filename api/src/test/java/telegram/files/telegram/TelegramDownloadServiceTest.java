package telegram.files.telegram;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TelegramDownloadServiceTest {

    @Test
    void canBeConstructed() {
        Assertions.assertNotNull(new TelegramDownloadService());
    }
}
