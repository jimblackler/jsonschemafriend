package net.jimblackler.jsonschemafriend;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public final class TestUtil {
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void clearDirectory(final Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }

        try (Stream<Path> s = Files.walk(dir)) {
            s.sorted(Comparator.reverseOrder())
                    .filter(p -> !dir.equals(p))
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}
