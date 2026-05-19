package telegram.files;

import io.vertx.core.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import telegram.files.repository.SettingKey;
import telegram.files.repository.SettingRecord;
import telegram.files.repository.SettingRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AdminAuthServiceTest {

    private final SettingRepository settingRepository = new InMemorySettingRepository();

    @AfterEach
    void tearDown() {
        DataVerticle.settingRepository = null;
    }

    @Test
    @DisplayName("首次初始化会写入默认管理员凭据并使用 PBKDF2 哈希")
    void ensureDefaultCredentialsCreated() throws Exception {
        DataVerticle.settingRepository = settingRepository;
        AdminAuthService authService = new AdminAuthService();

        await(authService.ensureInitialized());

        String username = await(DataVerticle.settingRepository.getByKey(SettingKey.adminUsername));
        String passwordHash = await(DataVerticle.settingRepository.getByKey(SettingKey.adminPasswordHash));
        Boolean verified = await(authService.verifyCredentials("admin", "admin"));

        Assertions.assertEquals("admin", username);
        Assertions.assertNotNull(passwordHash);
        Assertions.assertTrue(passwordHash.startsWith("pbkdf2$"));
        Assertions.assertTrue(verified);
    }

    @Test
    @DisplayName("拒绝过短的新密码")
    void rejectShortPassword() {
        DataVerticle.settingRepository = settingRepository;
        AdminAuthService authService = new AdminAuthService();

        Throwable throwable = Assertions.assertThrows(Exception.class, () ->
                await(authService.updateCredentials("admin", "123")));

        Assertions.assertTrue(throwable.getMessage().contains("Password must be at least 4 characters"));
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }

    private static class InMemorySettingRepository implements SettingRepository {

        private final Map<String, String> settings = new HashMap<>();

        @Override
        public Future<SettingRecord> createOrUpdate(String key, String value) {
            settings.put(key, value);
            return Future.succeededFuture(new SettingRecord(key, value));
        }

        @Override
        public Future<List<SettingRecord>> getByKeys(List<String> keys) {
            return Future.succeededFuture(keys.stream()
                    .filter(settings::containsKey)
                    .map(key -> new SettingRecord(key, settings.get(key)))
                    .toList());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Future<T> getByKey(SettingKey key) {
            String value = settings.get(key.name());
            if (value == null) {
                return Future.succeededFuture(key.defaultValue == null ? null : (T) key.defaultValue);
            }
            return Future.succeededFuture((T) key.converter.apply(value));
        }
    }
}
