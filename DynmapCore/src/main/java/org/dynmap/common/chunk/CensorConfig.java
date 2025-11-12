package org.dynmap.common.chunk;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;


public final class CensorConfig {
    private static volatile List<Area> cachedAreas = null;
    private static final Object lock = new Object();
    private static final Path DEFAULT_PATH = Paths.get("plugins", "dynmap", "config", "censor_areas.yaml");


    public static final class Area {
        public final int minX, maxX, minY, maxY, minZ, maxZ;
        public Area(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
            this.minX = minX; this.maxX = maxX;
            this.minY = minY; this.maxY = maxY;
            this.minZ = minZ; this.maxZ = maxZ;
        }
        public boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX &&
                    z >= minZ && z <= maxZ &&
                    y >= minY && y <= maxY;
        }
    }

    public static List<Area> getAreas() {
        if (cachedAreas == null) {
            synchronized (lock) {
                if (cachedAreas == null) {
                    cachedAreas = loadAreasFrom(DEFAULT_PATH);
                }
            }
        }
        return cachedAreas;
    }

    public static void reload() {
        synchronized (lock) {
            cachedAreas = loadAreasFrom(DEFAULT_PATH);
        }
    }

    public static boolean isCensored(int x, int y, int z) {
        for (Area a : getAreas()) {
            if (a.contains(x, y, z)){
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<Area> loadAreasFrom(Path path) {
        if (!Files.exists(path)) {
            System.out.println("Censor config file not found: " + path.toAbsolutePath().toString());
            return Collections.emptyList();
        }else{
            System.out.println("Loading censor config from: " + path.toAbsolutePath().toString());
        }
        Yaml yaml = new Yaml();
        List<Area> areas = new ArrayList<>();
        try (InputStream in = Files.newInputStream(path)) {
            Object obj = yaml.load(in);
            if (obj instanceof Map) {
                Object areasObj = ((Map<String, Object>) obj).get("areas");
                if (areasObj instanceof Iterable) {
                    for (Object item : (Iterable<Object>) areasObj) {
                        if (item instanceof Map) {
                            Map<String, Object> m = (Map<String, Object>) item;
                            int minX = toInt(m.get("minX"), Integer.MIN_VALUE);
                            int maxX = toInt(m.get("maxX"), Integer.MAX_VALUE);
                            int minZ = toInt(m.get("minZ"), Integer.MIN_VALUE);
                            int maxZ = toInt(m.get("maxZ"), Integer.MAX_VALUE);
                            int minY = toInt(m.get("minY"), -64);
                            int maxY = toInt(m.get("maxY"), 319);
                            areas.add(new Area(minX, maxX, minY, maxY, minZ, maxZ));
                        }
                    }
                }
            }
        } catch (Exception e) {
            return Collections.emptyList();
        }
        return areas;
    }

    private static int toInt(Object v, int def) {
        if (v == null) return def;
        if (v instanceof Number) return ((Number)v).intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (Exception ex) {
            return def;
        }
    }
}