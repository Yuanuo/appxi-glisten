package com.gluonhq.charm.glisten.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Control;
import javafx.scene.control.TextFormatter;

import java.util.function.Function;

public abstract class TextInput extends Control {
    private final StringProperty promptText = new SimpleStringProperty(this, "promptText", "") {
        protected void invalidated() {
            String txt = this.get();
            if (txt != null && txt.contains("\n")) {
                txt = txt.replace("\n", "");
                this.set(txt);
            }

        }
    };
    private final StringProperty text = new SimpleStringProperty();
    private final StringProperty floatText = new SimpleStringProperty(this, "floatText", "");
    private final ObjectProperty<Function<String, String>> errorValidator = new SimpleObjectProperty<>(this, "error");
    private final ObjectProperty<TextFormatter<?>> textFormatter = new SimpleObjectProperty<>(this, "textFormatter");
    private final BooleanProperty editable = new SimpleBooleanProperty(this, "editable", true);

    public TextInput() {
    }

    public final StringProperty promptTextProperty() {
        return this.promptText;
    }

    public final String getPromptText() {
        return this.promptText.get();
    }

    public final void setPromptText(String value) {
        this.promptText.set(value);
    }

    public final String getText() {
        return this.text.get();
    }

    public final void setText(String value) {
        this.text.set(value);
    }

    public final StringProperty textProperty() {
        return this.text;
    }

    public final String getFloatText() {
        return this.floatText.get();
    }

    public final void setFloatText(String floatText) {
        this.floatText.set(floatText);
    }

    public final StringProperty floatTextProperty() {
        return this.floatText;
    }

    public final ObjectProperty<Function<String, String>> errorValidatorProperty() {
        return this.errorValidator;
    }

    public final Function<String, String> getErrorValidator() {
        return this.errorValidator.get();
    }

    public final void setErrorValidator(Function<String, String> value) {
        this.errorValidator.set(value);
    }

    public final ObjectProperty<TextFormatter<?>> textFormatterProperty() {
        return this.textFormatter;
    }

    public final TextFormatter<?> getTextFormatter() {
        return this.textFormatter.get();
    }

    public final void setTextFormatter(TextFormatter<?> value) {
        this.textFormatter.set(value);
    }

    public final BooleanProperty editableProperty() {
        return editable;
    }

    public final boolean isEditable() {
        return editable.get();
    }

    public final void setEditable(boolean editable) {
        this.editable.set(editable);
    }
}
