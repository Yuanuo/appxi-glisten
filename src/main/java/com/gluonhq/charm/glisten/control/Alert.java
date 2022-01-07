package com.gluonhq.charm.glisten.control;

import javafx.beans.NamedArg;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

public class Alert extends Dialog<ButtonType> {
    private final Region defaultGraphic;
    private final Button cancelButton;
    private final Button OKButton;
    private final ObjectProperty<AlertType> alertType;

    public Alert(@NamedArg("alertType") AlertType alertType) {
        this.alertType = new SimpleObjectProperty<>(this, "alertType") {
            protected void invalidated() {
                Alert.this.rootNode.getStyleClass().removeAll("warning", "error", "information", "confirmation");
                switch (this.get()) {
                    case CONFIRMATION -> {
                        Alert.this.rootNode.getStyleClass().add("confirmation");
                        if (!Alert.this.getButtons().contains(Alert.this.cancelButton)) {
                            Alert.this.getButtons().add(0, Alert.this.cancelButton);
                        }
                    }
                    case ERROR -> {
                        Alert.this.rootNode.getStyleClass().add("error");
                        Alert.this.getButtons().remove(Alert.this.cancelButton);
                    }
                    case INFORMATION -> {
                        Alert.this.rootNode.getStyleClass().add("information");
                        Alert.this.getButtons().remove(Alert.this.cancelButton);
                    }
                    case WARNING -> {
                        Alert.this.rootNode.getStyleClass().add("warning");
                        Alert.this.getButtons().remove(Alert.this.cancelButton);
                    }
                    case NONE -> Alert.this.getButtons().remove(Alert.this.cancelButton);
                }

            }
        };
        this.defaultGraphic = new Region();
        this.OKButton = new Button("OK");
        this.OKButton.setOnAction((event) -> {
            this.setResult(ButtonType.OK);
            this.hide();
        });
        this.cancelButton = new Button("CANCEL");
        this.cancelButton.setOnAction((event) -> {
            this.setResult(ButtonType.CANCEL);
            this.hide();
        });
        this.getButtons().add(this.OKButton);
        this.setAlertType(alertType);
        this.defaultGraphic.getStyleClass().add("graphic");
        this.rootNode.getStyleClass().add("alert");
        this.setGraphic(this.defaultGraphic);
    }

    public Alert(@NamedArg("alertType") AlertType type, @NamedArg("contentText") String contentText) {
        this(type);
        this.setContentText(contentText);
    }

    public final ObjectProperty<AlertType> alertTypeProperty() {
        return this.alertType;
    }

    public final AlertType getAlertType() {
        return (AlertType) this.alertType.get();
    }

    public final void setAlertType(AlertType value) {
        this.alertType.set(value);
    }

    public final Region defaultGraphic() {
        return defaultGraphic;
    }

    public final Button okButton() {
        return OKButton;
    }

    public final Button cancelButton() {
        return cancelButton;
    }
}
