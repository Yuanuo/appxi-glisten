package com.gluonhq.charm.glisten.control;

import com.gluonhq.impl.charm.glisten.control.skin.TextAreaSkin;
import javafx.scene.control.Skin;

public class TextArea extends TextInput {
    public final javafx.scene.control.TextArea innerInput;

    public TextArea() {
        this("");
    }

    public TextArea(String text) {
        this.setText(text);
        this.getStyleClass().setAll("charm-text-area");
        this.innerInput = new javafx.scene.control.TextArea();
    }

    protected Skin<?> createDefaultSkin() {
        return new TextAreaSkin(this, this.innerInput);
    }
}
