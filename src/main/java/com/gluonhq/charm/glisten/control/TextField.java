package com.gluonhq.charm.glisten.control;

import com.gluonhq.impl.charm.glisten.control.skin.TextFieldSkin;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Skin;

public class TextField extends TextInput {
    private static final int DEFAULT_MAX_COUNTER_LENGTH = 0;
    private final IntegerProperty maxLength;
    public final javafx.scene.control.TextField innerInput;

    public TextField() {
        this("");
    }

    public TextField(String text) {
        this.maxLength = new SimpleIntegerProperty(DEFAULT_MAX_COUNTER_LENGTH);
        this.setText(text);
        this.getStyleClass().setAll("charm-text-field");

        this.innerInput = new javafx.scene.control.TextField();
    }

    public final void setMaxLength(int maxLength) {
        this.maxLength.set(maxLength);
    }

    public final int getMaxLength() {
        return this.maxLength.get();
    }

    public final IntegerProperty maxLengthProperty() {
        return this.maxLength;
    }

    protected Skin<?> createDefaultSkin() {
        return new TextFieldSkin(this, this.innerInput);
    }
}
