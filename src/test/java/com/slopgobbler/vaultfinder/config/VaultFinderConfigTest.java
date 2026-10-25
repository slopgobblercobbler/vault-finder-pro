package com.slopgobbler.vaultfinder.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class VaultFinderConfigTest {

    private File tempConfigFile;

    @BeforeEach
    public void setup() throws IOException {
        tempConfigFile = File.createTempFile("vaultfinder_test_config", ".json");
        VaultFinderConfig.setCustomConfigFile(tempConfigFile);
    }

    @AfterEach
    public void teardown() {
        VaultFinderConfig.setCustomConfigFile(null);
        if (tempConfigFile != null && tempConfigFile.exists()) {
            tempConfigFile.delete();
        }
    }

    @Test
    public void testDefaultConfigValues() {
        VaultFinderConfig config = new VaultFinderConfig();
        assertEquals(VaultFinderConfig.DEFAULT_RANGE, config.getHighlightRange());
        assertTrue(config.isRadarEnabled());
        assertTrue(config.isShulkerPeekEnabled());
        assertTrue(config.isXrayEnabled());
        assertEquals(VaultFinderConfig.DEFAULT_MATCHING_COLOR, config.getMatchingColor());
        assertEquals(VaultFinderConfig.DEFAULT_PARTIAL_COLOR, config.getPartialMatchingColor());
    }

    @Test
    public void testRangeClamping() {
        VaultFinderConfig config = new VaultFinderConfig();

        config.setHighlightRange(4); // below min 8
        assertEquals(VaultFinderConfig.MIN_RANGE, config.getHighlightRange());

        config.setHighlightRange(512); // above max 256
        assertEquals(VaultFinderConfig.MAX_RANGE, config.getHighlightRange());

        config.setHighlightRange(128); // valid
        assertEquals(128, config.getHighlightRange());
        assertEquals(128, config.getMaxRadarDistance());
    }

    @Test
    public void testColorParsingAndFloats() {
        VaultFinderConfig config = new VaultFinderConfig();

        int parsed = VaultFinderConfig.parseColor("#00FF00", 0);
        assertEquals(0xFF00FF00, parsed);

        config.setMatchingColor(0xFF00FF00);
        assertEquals(0xFF, config.getMatchingColorA());
        assertEquals(0x00, config.getMatchingColorR());
        assertEquals(0xFF, config.getMatchingColorG());
        assertEquals(0x00, config.getMatchingColorB());

        float[] floats = config.getMatchingColorFloats();
        assertEquals(4, floats.length);
        assertEquals(0.0f, floats[0], 0.001f);
        assertEquals(1.0f, floats[1], 0.001f);
        assertEquals(0.0f, floats[2], 0.001f);
        assertEquals(1.0f, floats[3], 0.001f);
    }

    @Test
    public void testJsonSerializationAndDeserialization() throws IOException {
        VaultFinderConfig config = new VaultFinderConfig();
        config.setHighlightRange(144);
        config.setRadarEnabled(false);
        config.setShulkerPeekEnabled(true);
        config.setXrayEnabled(false);
        config.setMatchingColorHex("#00E5FF");
        config.setPartialMatchingColorHex("#FF8800");

        config.save();
        assertTrue(tempConfigFile.exists());
        assertTrue(Files.size(tempConfigFile.toPath()) > 0);

        VaultFinderConfig loaded = new VaultFinderConfig();
        loaded.load();

        assertEquals(144, loaded.getHighlightRange());
        assertFalse(loaded.isRadarEnabled());
        assertTrue(loaded.isShulkerPeekEnabled());
        assertFalse(loaded.isXrayEnabled());
        assertEquals(config.getMatchingColor(), loaded.getMatchingColor());
        assertEquals(config.getPartialMatchingColor(), loaded.getPartialMatchingColor());
    }
}
