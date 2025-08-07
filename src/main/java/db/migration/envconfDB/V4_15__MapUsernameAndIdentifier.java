package db.migration.envconfDB;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V4_15__MapUsernameAndIdentifier extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("name-id-mapping.csv"), StandardCharsets.UTF_8))) {
            String line = null;
            while ((line = fileReader.readLine()) != null) {
                if (!line.isEmpty()) {
                    String[] split = line.split(";");
                    String name = split[0];
                    String id = split[1];
                    String sql = String.format("UPDATE additionalrevisioninfo SET authorId='%s' WHERE author='%s'", id,
                            name);
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.execute();
                    }
                }
            }
        }
    }
}
