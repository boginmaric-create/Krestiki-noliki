package com.edgefan.edgerunners.client.chrome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Data for one external chrome set: per body part, a tree of animated groups of textured cubes. */
public final class ChromeModel {
    public final Map<String, List<Group>> parts = new HashMap<>();

    public record Cube(float x0, float y0, float z0, float x1, float y1, float z1, int mat, boolean chromeAtlas, String glow) {}

    public record Anim(boolean rotation, int axis, String signal, float amp, float speed, float phase, boolean range, float from, float to) {}

    public record Group(float px, float py, float pz, float rx, float ry, float rz, List<Anim> anims, List<Cube> cubes, List<Group> children) {}

    public static ChromeModel parse(JsonObject root) {
        ChromeModel m = new ChromeModel();
        JsonObject parts = root.getAsJsonObject("parts");
        for (Map.Entry<String, JsonElement> e : parts.entrySet()) {
            List<Group> list = new ArrayList<>();
            for (JsonElement g : e.getValue().getAsJsonArray()) list.add(group(g.getAsJsonObject()));
            m.parts.put(e.getKey(), list);
        }
        return m;
    }

    private static Group group(JsonObject o) {
        float[] pv = floats(o.getAsJsonArray("pivot"));
        float[] rot = o.has("rot") ? floats(o.getAsJsonArray("rot")) : new float[3];
        List<Anim> anims = new ArrayList<>();
        if (o.has("anims")) for (JsonElement el : o.getAsJsonArray("anims")) {
            JsonObject a = el.getAsJsonObject();
            boolean range = a.has("from");
            anims.add(new Anim("rot".equals(a.get("type").getAsString()), "xyz".indexOf(a.get("axis").getAsString()),
                    a.get("signal").getAsString(), f(a, "amp"), f(a, "speed"), f(a, "phase"), range, f(a, "from"), f(a, "to")));
        }
        List<Cube> cubes = new ArrayList<>();
        for (JsonElement el : o.getAsJsonArray("cubes")) {
            JsonObject c = el.getAsJsonObject();
            float[] fr = floats(c.getAsJsonArray("from")), to = floats(c.getAsJsonArray("to"));
            cubes.add(new Cube(fr[0], fr[1], fr[2], to[0], to[1], to[2], c.get("mat").getAsInt(),
                    "c".equals(c.get("atlas").getAsString()), c.has("glow") ? c.get("glow").getAsString() : null));
        }
        List<Group> children = new ArrayList<>();
        if (o.has("children")) for (JsonElement el : o.getAsJsonArray("children")) children.add(group(el.getAsJsonObject()));
        return new Group(pv[0], pv[1], pv[2], rot[0], rot[1], rot[2], anims, cubes, children);
    }

    private static float f(JsonObject o, String k) { return o.has(k) ? o.get(k).getAsFloat() : 0f; }

    private static float[] floats(JsonArray a) {
        float[] r = new float[a.size()];
        for (int i = 0; i < r.length; i++) r[i] = a.get(i).getAsFloat();
        return r;
    }
}
