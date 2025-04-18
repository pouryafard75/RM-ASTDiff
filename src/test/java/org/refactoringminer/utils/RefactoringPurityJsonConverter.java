package org.refactoringminer.utils;

import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ArgumentConverter;
import org.refactoringminer.test.RefactoringPopulator;

import javax.json.JsonObject;
import javax.json.JsonValue;

public class RefactoringPurityJsonConverter implements ArgumentConverter {
    private static RefactoringPopulator.Root fromJsonObject(JsonObject json) {
        RefactoringPopulator.Root root = new RefactoringPopulator.Root();
        root.repository = json.getString("repository");
        root.sha1 = json.getString("sha1");
        root.url = json.getString("url");
        root.refactorings = json.getJsonArray("refactorings").getValuesAs((JsonValue value) -> {
            JsonObject jsonRefactoring = (JsonObject) value;
            RefactoringPopulator.Refactoring refactoring = new RefactoringPopulator.Refactoring();
            refactoring.type = getString(jsonRefactoring, "type");
            refactoring.description = getString(jsonRefactoring, "description");
            RefactoringPopulator.Purity purity = new RefactoringPopulator.Purity();
            JsonObject jsonPurity = jsonRefactoring.getJsonObject("purity");
            purity.purityValue = getString(jsonPurity, "purityValue");
            refactoring.purity = purity;
            return refactoring;
        });
        return root;
    }

    private static String getString(JsonObject json, String name) {
        if (json.isNull(name)) {
            return null;
        }
        return json.getString(name);
    }

    @Override
    public Object convert(Object source, ParameterContext context) {
        if (!(source instanceof JsonObject)) {
            throw new ArgumentConversionException("Not a JsonObject");
        }
        JsonObject json = (JsonObject) source;
        String name = context.getParameter().getName();
        Class<?> type = context.getParameter().getType();
        if (type == RefactoringPopulator.Root.class) {
            return fromJsonObject(json);
        } else if (type == String.class) {
            return json.getString(name);
        } else if (type == int.class) {
            return json.getInt(name);
        } else if (type == boolean.class) {
            return json.getBoolean(name);
        }
        throw new ArgumentConversionException("Can't convert to type: '" + type.getName() + "'");
    }
}
