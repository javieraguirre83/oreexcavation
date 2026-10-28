package oreexcavation.shapes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import oreexcavation.utils.JsonHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** The player's saved shapes (client side, config/oreexcavation/shapes.json). */
public class ShapeRegistry {
    public static final File FILE_SHAPES = new File("config/oreexcavation/shapes.json");
    public static final ShapeRegistry INSTANCE = new ShapeRegistry();

    private final List<ExcavateShape> shapeList = new ArrayList<>();
    private int curShape = 0;

    public void toggleShape() {
        setActiveShape((curShape + 1) % (shapeList.size() + 1));
    }

    public void toggleShapeRev() {
        setActiveShape((curShape + shapeList.size()) % (shapeList.size() + 1));
    }

    public ExcavateShape getActiveShape() {
        return getShapeAt(curShape);
    }

    public void setActiveShape(int idx) {
        curShape = idx > 0 && idx <= shapeList.size() ? idx : 0;
    }

    public List<ExcavateShape> getShapeList() {
        return Collections.unmodifiableList(shapeList);
    }

    public CompletableFuture<Void> addShape(ExcavateShape shape) {
        return Minecraft.getInstance().submit(() -> {
            if (!shapeList.contains(shape)) shapeList.add(shape);
        });
    }

    public CompletableFuture<Void> removeShape(ExcavateShape shape) {
        return Minecraft.getInstance().submit(() -> {
            shapeList.remove(shape);
        });
    }

    public ExcavateShape getShapeAt(int idx) {
        return idx > 0 && idx <= shapeList.size() ? shapeList.get(idx - 1) : null;
    }

    public void loadShapes(File file) {
        shapeList.clear();
        if (file.exists()) {
            JsonObject json = JsonHelper.ReadFromFile(file);
            JsonHelper.GetArray(json, "shapes").forEach(element -> {
                if (element instanceof JsonObject obj) {
                    ExcavateShape shape = new ExcavateShape();
                    shape.readFromJson(obj);
                    shapeList.add(shape);
                }
            });
        } else {
            shapeList.add(new ExcavateShape().setName("1x2").setMask(4224).setReticle(12));
            shapeList.add(new ExcavateShape().setName("3x3").setMask(473536).setReticle(12));
            shapeList.add(new ExcavateShape().setName("5x5").setMask(33554431).setReticle(7));
            shapeList.add(new ExcavateShape().setName("Stairs Up").setMask(135296).setStairStride(1).setStairElev(1).setReticle(12));
            shapeList.add(new ExcavateShape().setName("Stairs Down").setMask(135296).setStairStride(1).setStairElev(-1).setReticle(17));
            saveShapes(file);
        }
    }

    public void saveShapes(File file) {
        JsonArray array = new JsonArray();
        shapeList.forEach(s -> array.add(s.writeToJson(new JsonObject())));
        JsonObject json = new JsonObject();
        json.add("shapes", array);
        JsonHelper.WriteToFile(file, json);
    }
}
