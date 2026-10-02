package dev.adventurers.core.persistence;

import dev.adventurers.core.world.WorldModel;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import static java.nio.file.StandardCopyOption.*;

public final class WorldStore {
    private WorldStore() {}
    public static WorldModel load(Path path) throws IOException {
        if (Files.size(path) > 64 * 1024 * 1024 + 20) throw new IOException("Oversized snapshot");
        return WorldCodec.decode(Files.readAllBytes(path));
    }
    /** Validate the existing save before rotating it. A corrupt save must never be overwritten. */
    public static void save(Path path, WorldModel world) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        byte[] data = WorldCodec.encode(world);
        if (Files.exists(path)) load(path);
        Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), "adventurers-", ".tmp");
        try {
            Files.write(temporary, data);
            try (var channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) { channel.force(true); }
            if (Files.exists(path)) Files.copy(path, path.resolveSibling(path.getFileName() + ".bak"), REPLACE_EXISTING);
            try { Files.move(temporary, path, ATOMIC_MOVE, REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, path, REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
