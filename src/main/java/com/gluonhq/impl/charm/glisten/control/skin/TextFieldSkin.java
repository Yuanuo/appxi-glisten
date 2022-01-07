package com.gluonhq.impl.charm.glisten.control.skin;

import com.gluonhq.charm.glisten.control.TextField;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Label;

public class TextFieldSkin extends TextInputSkin {
    private Label counterLabel;
    private ChangeListener<Number> lengthChangeListener;

    public TextFieldSkin(TextField control, javafx.scene.control.TextField innerTextControl) {
        super(control, innerTextControl, 0.0D);
        control.maxLengthProperty().addListener((ov) -> this.updateChildren());
    }

    protected void layoutChildren(double x, double y, double w, double h) {
        super.layoutChildren(x, y, w, h);
        double textFieldStartY = this.floatLabel == null ? this.snapPosition(y) : this.snapPosition(y) + 10.0D;
        double textFieldHeight = this.innerTextControl.prefHeight(w);
        this.innerTextControl.resizeRelocate(x, textFieldStartY, w, textFieldHeight);
        if (this.counterLabel != null) {
            double counterLabelWidth = this.counterLabel.prefWidth(-1.0D);
            double counterStartX = x + w - counterLabelWidth;
            double counterStartY = textFieldStartY + textFieldHeight + 5.0D;
            this.counterLabel.resizeRelocate(counterStartX, counterStartY, counterLabelWidth, this.counterLabel.prefHeight(-1.0D));
        }

    }

    protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        double prefHeight = super.computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
        double counterLabelHeight = this.counterLabel != null && this.errorLabel == null ? this.counterLabel.prefHeight(-1.0D) + 5.0D : 0.0D;
        return prefHeight + counterLabelHeight;
    }

    protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return this.computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
    }

    void updateChildren() {
        super.updateChildren();
        if (((TextField)this.control).getMaxLength() > 0) {
            this.createAndAddCounterLabel();
        }

    }

    void bindInnerControlProperties() {
        super.bindInnerControlProperties();
        this.innerTextControl.lengthProperty().addListener(this.lengthChangeListener);
    }

    void unbindInnerControlProperties() {
        super.unbindInnerControlProperties();
        if (this.lengthChangeListener == null) {
            this.lengthChangeListener = (ov, oldValue, newValue) -> {
                this.control.pseudoClassStateChanged(PSEUDO_CLASS_ERROR, ((TextField)this.control).getMaxLength() > 0 && newValue.intValue() > ((TextField)this.control).getMaxLength());
            };
        }

        this.innerTextControl.lengthProperty().removeListener(this.lengthChangeListener);
    }

    private void createAndAddCounterLabel() {
        this.unbindCounterProperties();
        this.counterLabel = new Label();
        this.counterLabel.setFocusTraversable(false);
        this.counterLabel.getStyleClass().setAll("counter");
        this.counterLabel.textProperty().bind(this.innerTextControl.lengthProperty().asString().concat("/").concat(((TextField)this.control).maxLengthProperty()));
        this.getChildren().add(this.counterLabel);
    }

    private void unbindCounterProperties() {
        if (this.counterLabel != null && this.counterLabel.textProperty().isBound()) {
            this.counterLabel.textProperty().unbind();
        }

    }
}
