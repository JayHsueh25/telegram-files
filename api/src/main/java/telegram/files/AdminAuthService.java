package telegram.files;

import cn.hutool.core.util.StrUtil;
import io.vertx.core.Future;
import io.vertx.ext.web.Session;
import telegram.files.repository.SettingKey;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class AdminAuthService {

    public static final String SESSION_AUTHENTICATED = "adminAuthenticated";

    public static final String SESSION_USERNAME = "adminUsername";

    private static final int PASSWORD_MIN_LENGTH = 4;

    private static final int PBKDF2_ITERATIONS = 120_000;

    private static final int KEY_LENGTH = 256;

    private static final int SALT_LENGTH = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    public Future<Void> ensureInitialized() {
        return DataVerticle.settingRepository.<String>getByKey(SettingKey.adminUsername)
                .compose(savedUsername -> DataVerticle.settingRepository.<String>getByKey(SettingKey.adminPasswordHash)
                        .compose(savedHash -> {
                            if (StrUtil.isNotBlank(savedUsername) && StrUtil.isNotBlank(savedHash)) {
                                return Future.succeededFuture();
                            }
                            return persistCredentials(Config.ADMIN_USERNAME, Config.ADMIN_PASSWORD);
                        }));
    }

    public Future<Boolean> verifyCredentials(String username, String password) {
        return DataVerticle.settingRepository.<String>getByKey(SettingKey.adminUsername)
                .compose(savedUsername -> DataVerticle.settingRepository.<String>getByKey(SettingKey.adminPasswordHash)
                        .map(savedHash -> StrUtil.equals(savedUsername, StrUtil.trim(username))
                                && verifyPassword(password, savedHash)));
    }

    public Future<Void> updateCredentials(String username, String password) {
        String normalizedUsername = StrUtil.trim(username);
        if (StrUtil.isBlank(normalizedUsername)) {
            return Future.failedFuture("Username is required");
        }
        if (StrUtil.length(password) < PASSWORD_MIN_LENGTH) {
            return Future.failedFuture("Password must be at least 4 characters");
        }
        return persistCredentials(normalizedUsername, password);
    }

    public boolean isAuthenticated(Session session) {
        return session != null && Boolean.TRUE.equals(session.get(SESSION_AUTHENTICATED));
    }

    public void markAuthenticated(Session session, String username) {
        if (session == null) {
            return;
        }
        session.put(SESSION_AUTHENTICATED, true);
        session.put(SESSION_USERNAME, StrUtil.trim(username));
    }

    public void clearAuthenticated(Session session) {
        if (session == null) {
            return;
        }
        session.remove(SESSION_AUTHENTICATED);
        session.remove(SESSION_USERNAME);
    }

    private Future<Void> persistCredentials(String username, String password) {
        String passwordHash = hashPassword(password);
        return DataVerticle.settingRepository.createOrUpdate(SettingKey.adminUsername.name(), username)
                .compose(_ -> DataVerticle.settingRepository.createOrUpdate(SettingKey.adminPasswordHash.name(), passwordHash))
                .mapEmpty();
    }

    private String hashPassword(String password) {
        try {
            byte[] salt = new byte[SALT_LENGTH];
            secureRandom.nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();

            return "pbkdf2$%d$%s$%s".formatted(
                    PBKDF2_ITERATIONS,
                    Base64.getEncoder().encodeToString(salt),
                    Base64.getEncoder().encodeToString(hash)
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to hash password", exception);
        }
    }

    private boolean verifyPassword(String password, String encodedHash) {
        if (StrUtil.isBlank(password) || StrUtil.isBlank(encodedHash)) {
            return false;
        }

        try {
            String[] parts = encodedHash.split("\\$");
            if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
                return false;
            }

            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, expectedHash.length * Byte.SIZE);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] actualHash = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();

            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception ignored) {
            return false;
        }
    }
}
