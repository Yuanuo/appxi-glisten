package com.gluonhq.impl.charm.glisten.control.skin;

import com.gluonhq.charm.glisten.control.TextInput;
import javafx.animation.Transition;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.control.Label;
import javafx.scene.control.SkinBase;
import javafx.scene.control.TextInputControl;
import javafx.util.Duration;

public abstract class TextInputSkin extends SkinBase<TextInput> {
    static final int FLOAT_LABEL_HEIGHT = 10;
    static final double TEXT_FIELD_ERROR_LABEL_PADDING = 5.0D;
    static final PseudoClass PSEUDO_CLASS_ERROR = PseudoClass.getPseudoClass("error");
    protected TextInput control = null;
    TextInputControl innerTextControl;
    Label floatLabel;
    Label errorLabel;
    private double floatLabelX;
    private double floatLabelY;
    private boolean transitionPlaying;
    private double contentPadding = 0.0D;
    private final Transition floatTransition = new Transition() {
        {
            this.setCycleDuration(Duration.millis(200.0D));
        }

        protected void interpolate(double frac) {
            double scalingFactor = 1.0D - 0.23D * frac;
            TextInputSkin.this.floatLabel.setScaleX(scalingFactor);
            TextInputSkin.this.floatLabel.setScaleY(scalingFactor);
            double originalWidth = TextInputSkin.this.floatLabel.prefWidth(-1.0D);
            double originalHeight = TextInputSkin.this.floatLabel.prefHeight(-1.0D);
            double newWidth = originalWidth - originalWidth * (1.0D - scalingFactor);
            double newHeight = originalHeight - originalHeight * (1.0D - scalingFactor);
            TextInputSkin.this.calculateNewX(TextInputSkin.this.snappedLeftInset() + TextInputSkin.this.innerTextControl.getPadding().getLeft() + TextInputSkin.this.contentPadding, newWidth);
            TextInputSkin.this.calculateNewY(TextInputSkin.this.snappedTopInset() + TextInputSkin.this.innerTextControl.getPadding().getTop() - FLOAT_LABEL_HEIGHT * 0.23D * frac, newHeight);
            TextInputSkin.this.control.requestLayout();
        }
    };
    private final ChangeListener<Boolean> focusChangeListener = (observable, oldValue, newValue) -> {
        this.control.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), newValue);
        this.setTransitionToFalse();
        this.control.requestLayout();
    };
    private final ChangeListener<String> textChangeListener = (observable, oldValue, newValue) -> {
        if (this.getSkinnable().getErrorValidator() != null) {
            String errorText = this.getSkinnable().getErrorValidator().apply(newValue);
            this.errorLabel.setText(errorText);
            this.control.pseudoClassStateChanged(PSEUDO_CLASS_ERROR, !this.isEmpty(errorText));
        }

    };
    private final ChangeListener<Number> heightChangeListener = (observable, oldValue, newValue) -> {
        if (!this.innerTextControl.getText().isEmpty()) {
            this.floatTransition.playFromStart();
            this.transitionPlaying = true;
        }

    };

    TextInputSkin(TextInput control, TextInputControl innerTextInput, double textPadding) {
        super(control);
        this.control = control;
        this.innerTextControl = innerTextInput;
        control.setFocusTraversable(false);
        this.contentPadding = textPadding;
        this.updateChildren();
        control.floatTextProperty().addListener((ov) -> this.updateChildren());
        control.errorValidatorProperty().addListener((ov) -> this.updateChildren());
    }

    void bindInnerControlProperties() {
        this.innerTextControl.promptTextProperty().bind(this.control.promptTextProperty());
        this.innerTextControl.textFormatterProperty().bind(this.control.textFormatterProperty());
        this.innerTextControl.textProperty().bindBidirectional(this.control.textProperty());
        this.innerTextControl.focusedProperty().addListener(this.focusChangeListener);
        this.innerTextControl.editableProperty().bind(this.control.editableProperty());
    }

    void unbindInnerControlProperties() {
        if (this.innerTextControl != null) {
            if (this.innerTextControl.promptTextProperty().isBound()) {
                this.innerTextControl.promptTextProperty().unbind();
            }

            if (this.innerTextControl.textFormatterProperty().isBound()) {
                this.innerTextControl.textFormatterProperty().unbind();
            }

            if (this.innerTextControl.textProperty().isBound()) {
                this.innerTextControl.textProperty().unbindBidirectional(this.control.textProperty());
            }
            
            if (this.innerTextControl.editableProperty().isBound()) {
                this.innerTextControl.editableProperty().unbind();
            }

            this.innerTextControl.focusedProperty().removeListener(this.focusChangeListener);
            this.innerTextControl.textProperty().removeListener(this.textChangeListener);
            this.innerTextControl.heightProperty().removeListener(this.heightChangeListener);
        }

    }

    void updateChildren() {
        this.getChildren().clear();
        this.unbindInnerControlProperties();
        this.getChildren().add(this.innerTextControl);
        this.bindInnerControlProperties();
        if (!this.control.getFloatText().isEmpty()) {
            this.createAndAddFloatLabel();
            this.innerTextControl.heightProperty().addListener(this.heightChangeListener);
        }

        if (this.control.getErrorValidator() != null) {
            this.createAndAddErrorLabel();
        }

    }

    protected void layoutChildren(double x, double y, double w, double h) {
        double fullWidth = w + this.snappedLeftInset() + this.snappedRightInset();
        double textFieldStartY = this.floatLabel == null ? this.snapPosition(y) : this.snapPosition(y) + FLOAT_LABEL_HEIGHT;
        double textFieldHeight = this.innerTextControl.prefHeight(w);
        this.innerTextControl.resizeRelocate(0.0D, textFieldStartY, fullWidth, textFieldHeight);
        if (this.floatLabel != null) {
            if (!this.transitionPlaying) {
                if (this.floatLabel.getScaleX() == 1.0D) {
                    if (this.innerTextControl.isFocused()) {
                        this.floatTransition.playFromStart();
                        this.transitionPlaying = true;
                    } else {
                        this.floatLabelX = this.snappedLeftInset() + this.innerTextControl.getPadding().getLeft() + this.contentPadding;
                        this.floatLabelY = this.snappedTopInset() + this.innerTextControl.getPadding().getTop() + FLOAT_LABEL_HEIGHT;
                    }
                } else if (!this.innerTextControl.isFocused() && this.innerTextControl.getText().isEmpty()) {
                    this.floatLabel.setScaleX(1.0D);
                    this.floatLabel.setScaleY(1.0D);
                    this.floatLabelX = this.snappedLeftInset() + this.innerTextControl.getPadding().getLeft() + this.contentPadding;
                    this.floatLabelY = this.snappedTopInset() + this.innerTextControl.getPadding().getTop() + FLOAT_LABEL_HEIGHT;
                }
            }

            this.floatLabel.resizeRelocate(this.floatLabelX, this.floatLabelY, this.floatLabel.prefWidth(-1.0D), this.floatLabel.prefHeight(-1.0D));
        }

        if (this.errorLabel != null) {
            double errorStartY = textFieldStartY + textFieldHeight + TEXT_FIELD_ERROR_LABEL_PADDING;
            this.errorLabel.resizeRelocate(x, errorStartY, this.errorLabel.prefWidth(-1.0D), this.errorLabel.prefHeight(-1.0D));
        }

    }

    protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        double floatingLabelHeight = this.floatLabel == null ? 0.0D : FLOAT_LABEL_HEIGHT;
        double errorLabelHeight = this.errorLabel == null ? 0.0D : this.errorLabel.prefHeight(-1.0D) + TEXT_FIELD_ERROR_LABEL_PADDING;
        double innerTextControlHeight = this.innerTextControl.prefHeight(width);
        return topInset + innerTextControlHeight + floatingLabelHeight + errorLabelHeight + bottomInset;
    }

    private void setTransitionToFalse() {
        this.transitionPlaying = false;
    }

    private void calculateNewX(double originalX, double newWidth) {
        this.floatLabelX = originalX - (this.floatLabel.prefWidth(-1.0D) - newWidth) / 2.0D;
    }

    private void calculateNewY(double originalY, double newHeight) {
        this.floatLabelY = originalY - (this.floatLabel.prefHeight(-1.0D) - newHeight);
    }

    private void createAndAddFloatLabel() {
        this.unbindFloatLabelProperties();
        this.floatLabel = new Label(this.control.getFloatText());
        this.floatLabel.getStyleClass().setAll("float");
        this.floatLabel.setFocusTraversable(false);
        this.floatLabel.setMouseTransparent(true);
        this.floatLabel.fontProperty().bind(this.innerTextControl.fontProperty());
        this.getChildren().add(this.floatLabel);
    }

    private void unbindFloatLabelProperties() {
        if (this.floatLabel != null && this.floatLabel.fontProperty().isBound()) {
            this.floatLabel.fontProperty().unbind();
        }

    }

    private void createAndAddErrorLabel() {
        this.errorLabel = new Label();
        this.errorLabel.setFocusTraversable(false);
        this.errorLabel.getStyleClass().setAll("error");
        this.innerTextControl.textProperty().addListener(this.textChangeListener);
        this.getChildren().add(this.errorLabel);
    }

    private boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }
}
