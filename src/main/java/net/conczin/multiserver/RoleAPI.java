package net.conczin.multiserver;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class RoleAPI {
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes in milliseconds
    private final Gson Gson = new Gson();

    public static final RoleAPI INSTANCE = new RoleAPI();

    public static RoleAPI getInstance() {
        return INSTANCE;
    }

    private final Map<String, UserRoles> roles = new ConcurrentHashMap<>();

    public void get(String uuid, Consumer<UserRoles> callback) {
        if (roles.containsKey(uuid)) {
            UserRoles roles = this.roles.get(uuid);
            callback.accept(roles);

            // Update in the background
            if (roles.expired()) {
                CompletableFuture.supplyAsync(() -> {
                    fetchUserData(uuid);
                    return 0;
                });
            }
        } else {
            CompletableFuture.supplyAsync(() -> {
                fetchUserData(uuid, callback);
                return 0;
            });
        }
    }

    private void fetchUserData(String uuid) {
        fetchUserData(uuid, r -> {
        });
    }

    private void fetchUserData(String uuid, Consumer<UserRoles> callback) {
        try {
            // Construct the URL for the API request
            URL url = new URL("https://api.conczin.net/v1/minecraft/" + Config.getInstance().discordGuild + "/" + uuid.replace("-", ""));

            // Create an HttpURLConnection and set request properties
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // Get the response code
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String content = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);

                // Parse the JSON response
                JsonObject jsonObject = Gson.fromJson(content, JsonObject.class);

                // Create a Set for the roles
                Set<String> roles = new HashSet<>();
                boolean linked = false;
                if (jsonObject.has("roles")) {
                    Arrays.stream(jsonObject.get("roles").getAsString().split(",")).map(String::trim).forEach(roles::add);
                    linked = true;
                }

                UserRoles userRoles = new UserRoles(roles, linked);
                this.roles.put(uuid, userRoles);
                callback.accept(userRoles);
            } else {
                System.err.println("HTTP Request failed with response code: " + responseCode);
                callback.accept(null);
            }
        } catch (IOException e) {
            e.printStackTrace();
            callback.accept(null);
        }
    }

    public static class UserRoles {
        private final Set<String> roles;
        private final long lastUpdated;
        private final boolean linked;
        private final String bestRole;

        public UserRoles(Set<String> roles, boolean linked) {
            this.roles = roles;
            this.lastUpdated = System.currentTimeMillis();
            this.bestRole = roles.stream()
                    .filter(r -> Config.getInstance().roleViewDistance.containsKey(r))
                    .max(Comparator.comparingInt(r -> Config.getInstance().roleViewDistance.getOrDefault(r, 0)))
                    .orElse("default");
            this.linked = linked;
        }

        public boolean expired() {
            return System.currentTimeMillis() - lastUpdated > CACHE_DURATION;
        }

        public Set<String> getRoles() {
            return roles;
        }

        public String getBestRole() {
            return bestRole;
        }

        public boolean isLinked() {
            return linked;
        }
    }
}
