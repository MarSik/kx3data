package org.marsik.ham.kx3tool.logbook;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.marsik.ham.kx3tool.configuration.Configuration;

public class LogbookDatabase {
    private Path logbookPath = Configuration.logbookFile();

    public Connection open() throws IOException, SQLException {
        Files.createDirectories(logbookPath.getParent());
        return DriverManager.getConnection("jdbc:h2:" + logbookPath.toString());
    }
}
