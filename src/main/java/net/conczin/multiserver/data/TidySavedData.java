package net.conczin.multiserver.data;

import net.minecraft.world.level.saveddata.SavedData;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;

public abstract class TidySavedData extends SavedData {
    @Override
    public void save(File file) {
        try {
            Files.createDirectories(file.getParentFile().toPath());
        } catch (FileAlreadyExistsException ignored) {
            // nop
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        super.save(file);
    }
}
