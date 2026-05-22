package telegram.files.telegram;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TelegramChatFileServiceTest {

    @Test
    void canBeConstructedWithDependencies() {
        TelegramChatFileService service = new TelegramChatFileService();

        Assertions.assertNotNull(service);
    }
}
