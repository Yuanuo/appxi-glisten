package com.gluonhq.charm.glisten.control;

import com.gluonhq.attach.display.DisplayService;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.impl.charm.glisten.event.DialogEventDispatcher;
import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.geometry.Dimension2D;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Optional;
import java.util.UUID;

public class Dialog<T> implements EventTarget {
    private T result;
    private final ObservableList<ButtonBase> buttons;
    private FullScreenDialogView fullScreenDialogView;
    private DialogView dialogView;
    boolean isFullscreen;
    protected BorderPane rootNode;
    private final ReadOnlyBooleanWrapper showingProperty;
    private final ObjectProperty<Node> titleProperty;
    private final StringProperty titleTextProperty;
    private final StringProperty contentTextProperty;
    private final ObjectProperty<Node> contentProperty;
    private final ObjectProperty<Node> graphic;
    private final BooleanProperty autoHide;
    private final StringProperty id;
    private final DialogEventDispatcher<LifecycleEvent> dialogEventDispatcher;
    private final ObjectProperty<EventHandler<LifecycleEvent>> onShowing;
    private final ObjectProperty<EventHandler<LifecycleEvent>> onShown;
    private final ObjectProperty<EventHandler<LifecycleEvent>> onHiding;
    private final ObjectProperty<EventHandler<LifecycleEvent>> onHidden;
    private final ObjectProperty<EventHandler<LifecycleEvent>> onCloseRequest;

    public Dialog() {
        this(false);
    }

    public Dialog(String contentText) {
        this(false);
        this.setContent(new Label(contentText));
    }

    public Dialog(String title, String contentText) {
        this(false);
        this.setTitle(new Label(title));
        this.setContent(new Label(contentText));
    }

    public Dialog(@NamedArg("fullscreen") boolean fullscreen) {
        this.result = null;
        this.buttons = FXCollections.observableArrayList();
        this.rootNode = new BorderPane() {
            protected double computePrefWidth(double height) {
                return DisplayService.create().map((service) -> {
                    Dimension2D dim = service.getDefaultDimensions();
                    double minWidth = dim.getWidth() * (service.isTablet() ? 0.6D : 0.75D);
                    Node content = Dialog.this.getContent();
                    if (content != null) {
                        double prefWidth = content.prefWidth(-1.0D);
                        return prefWidth < minWidth ? minWidth : prefWidth;
                    } else {
                        return minWidth;
                    }
                }).orElse(400.0D);
            }
        };
        this.showingProperty = new ReadOnlyBooleanWrapper(false);
        this.titleProperty = new SimpleObjectProperty<>() {
            private Node oldTitle;

            protected void invalidated() {
                if (this.oldTitle != null) {
                    this.oldTitle.getStyleClass().remove("dialog-title");
                }

                Node title = this.get();
                this.oldTitle = title;
                if (title != null && !title.getStyleClass().contains("dialog-title")) {
                    title.getStyleClass().add("dialog-title");
                }

                if (title instanceof Labeled) {
                    Labeled lbl = (Labeled) title;
                    if (!lbl.wrapTextProperty().isBound()) {
                        lbl.setWrapText(true);
                    }
                }

            }
        };
        this.titleTextProperty = new SimpleStringProperty(this, "titleText") {
            protected void invalidated() {
                if (this.get() != null) {
                    Dialog.this.setTitle(new Label(this.get()));
                }

            }
        };
        this.contentTextProperty = new SimpleStringProperty(this, "contentText") {
            protected void invalidated() {
                if (this.get() != null) {
                    Dialog.this.setContent(new Label(this.get()));
                }

            }
        };
        this.contentProperty = new SimpleObjectProperty<>() {
            private Node oldContent;

            protected void invalidated() {
                if (this.oldContent != null) {
                    this.oldContent.getStyleClass().remove("dialog-content");
                }

                Node content = this.get();
                this.oldContent = content;
                if (content != null && !content.getStyleClass().contains("dialog-content")) {
                    content.getStyleClass().add("dialog-content");
                }

                if (content instanceof Labeled) {
                    Labeled lbl = (Labeled) content;
                    if (!lbl.wrapTextProperty().isBound()) {
                        lbl.setWrapText(true);
                    }
                }

            }
        };
        this.graphic = new SimpleObjectProperty<>(this, "graphic");
        this.autoHide = new SimpleBooleanProperty(this, "autoHide", true);
        this.id = new SimpleStringProperty();
        this.dialogEventDispatcher = new DialogEventDispatcher<>(this);
        this.onShowing = new SimpleObjectProperty<>(this, "onShowing") {
            protected void invalidated() {
                Dialog.this.dialogEventDispatcher.setEventHandler(LifecycleEvent.SHOWING, this.get());
            }
        };
        this.onShown = new SimpleObjectProperty<>(this, "onShown") {
            protected void invalidated() {
                Dialog.this.dialogEventDispatcher.setEventHandler(LifecycleEvent.SHOWN, this.get());
            }
        };
        this.onHiding = new SimpleObjectProperty<>(this, "onHiding") {
            protected void invalidated() {
                Dialog.this.dialogEventDispatcher.setEventHandler(LifecycleEvent.HIDING, this.get());
            }
        };
        this.onHidden = new SimpleObjectProperty<>(this, "onHidden") {
            protected void invalidated() {
                Dialog.this.dialogEventDispatcher.setEventHandler(LifecycleEvent.HIDDEN, this.get());
            }
        };
        this.onCloseRequest = new SimpleObjectProperty<>(this, "onCloseRequest") {
            protected void invalidated() {
                Dialog.this.dialogEventDispatcher.setEventHandler(LifecycleEvent.CLOSE_REQUEST, this.get());
            }
        };
        this.isFullscreen = fullscreen;
        if (!this.isFullscreen) {
            this.rootNode.setId(this.generateId());
            this.dialogView = new DialogView(this.rootNode, this);
            installIntoMobileApplication(this.dialogView, this.rootNode, this.showingProperty);
            this.rootNode.getStyleClass().add("dialog");
            //
            this.dialogView.setOnShowing(e -> {
                if (null != this.onShowing.get())
                    this.onShowing.get().handle(new LifecycleEvent(this, LifecycleEvent.SHOWING));
            });
            this.dialogView.setOnShown(e -> {
                if (null != this.onShown.get())
                    this.onShown.get().handle(new LifecycleEvent(this, LifecycleEvent.SHOWN));
            });
//            this.dialogView.setOnShown(e -> Event.fireEvent(this, new LifecycleEvent(this, LifecycleEvent.SHOWN)));
        } else {
            this.fullScreenDialogView = new FullScreenDialogView(this.generateId());
            this.showingProperty.bind(this.fullScreenDialogView.showingProperty());
            this.showingProperty.addListener((o, ov, showing) -> {
                if (showing) {
                    this.fullScreenDialogView.getScene().getWindow().setOnCloseRequest((event) -> {
                        Platform.exitNestedEventLoop(this.fullScreenDialogView, null);
                    });
                    Platform.enterNestedEventLoop(this.fullScreenDialogView);
                } else {
                    this.fullScreenDialogView.getScene().getWindow().setOnCloseRequest((event) -> {
                    });
                    Platform.exitNestedEventLoop(this.fullScreenDialogView, null);
                }

            });
            AppManager.getInstance().addViewFactory(this.fullScreenDialogView.getName(), () -> this.fullScreenDialogView);
            this.fullScreenDialogView.setOnCloseButtonPressed((event) -> this.hide());
            //
            this.fullScreenDialogView.setOnShowing(e -> {
                if (null != this.onShowing.get())
                    this.onShowing.get().handle(new LifecycleEvent(this, LifecycleEvent.SHOWING));
            });
            this.fullScreenDialogView.setOnShown(e -> {
                if (null != this.onShown.get())
                    this.onShown.get().handle(new LifecycleEvent(this, LifecycleEvent.SHOWN));
            });
//            this.fullScreenDialogView.setOnShown(e -> Event.fireEvent(this, new LifecycleEvent(this, LifecycleEvent.SHOWN)));
        }

    }

    public final ReadOnlyBooleanProperty showingProperty() {
        return this.showingProperty.getReadOnlyProperty();
    }

    public final boolean isShowing() {
        return this.showingProperty.get();
    }

    public final ObjectProperty<Node> titleProperty() {
        return this.titleProperty;
    }

    public final Node getTitle() {
        return this.titleProperty.get();
    }

    public final void setTitle(Node title) {
        this.titleProperty.set(title);
    }

    public final StringProperty titleTextProperty() {
        return this.titleTextProperty;
    }

    public final void setTitleText(String text) {
        this.titleTextProperty.set(text);
    }

    public final String getTitleText() {
        return this.titleTextProperty.get();
    }

    public final StringProperty contentTextProperty() {
        return this.contentTextProperty;
    }

    public final void setContentText(String text) {
        this.contentTextProperty.set(text);
    }

    public final String getContentText() {
        return this.contentTextProperty.get();
    }

    public final ObjectProperty<Node> contentProperty() {
        return this.contentProperty;
    }

    public final Node getContent() {
        return this.contentProperty.get();
    }

    public final void setContent(Node content) {
        this.contentProperty.set(content);
    }

    public final ObjectProperty<Node> graphicProperty() {
        return this.graphic;
    }

    public final void setGraphic(Node graphic) {
        this.graphic.set(graphic);
    }

    public final Node getGraphic() {
        return this.graphic.get();
    }

    public final BooleanProperty autoHideProperty() {
        return this.autoHide;
    }

    public final void setAutoHide(boolean value) {
        this.autoHide.set(value);
    }

    public final boolean isAutoHide() {
        return this.autoHide.get();
    }

    public final StringProperty idProperty() {
        return this.id;
    }

    public final void setId(String id) {
        this.id.setValue(id);
    }

    public final String getId() {
        return this.id.get();
    }

    public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        return tail.prepend(this.dialogEventDispatcher);
    }

    public final ObjectProperty<EventHandler<LifecycleEvent>> onShowingProperty() {
        return this.onShowing;
    }

    public final void setOnShowing(EventHandler<LifecycleEvent> value) {
        this.onShowing.set(value);
    }

    public final EventHandler<LifecycleEvent> getOnShowing() {
        return this.onShowing.get();
    }

    public final ObjectProperty<EventHandler<LifecycleEvent>> onShownProperty() {
        return this.onShown;
    }

    public final void setOnShown(EventHandler<LifecycleEvent> value) {
        this.onShown.set(value);
    }

    public final EventHandler<LifecycleEvent> getOnShown() {
        return this.onShown.get();
    }

    public final ObjectProperty<EventHandler<LifecycleEvent>> onHidingProperty() {
        return this.onHiding;
    }

    public final void setOnHiding(EventHandler<LifecycleEvent> value) {
        this.onHiding.set(value);
    }

    public final EventHandler<LifecycleEvent> getOnHiding() {
        return this.onHiding.get();
    }

    public final ObjectProperty<EventHandler<LifecycleEvent>> onHiddenProperty() {
        return this.onHidden;
    }

    public final void setOnHidden(EventHandler<LifecycleEvent> value) {
        this.onHidden.set(value);
    }

    public final EventHandler<LifecycleEvent> getOnHidden() {
        return this.onHidden.get();
    }

    public final ObjectProperty<EventHandler<LifecycleEvent>> onCloseRequestProperty() {
        return this.isFullscreen() ? this.fullScreenDialogView.onCloseRequestProperty() : this.onCloseRequest;
    }

    public final void setOnCloseRequest(EventHandler<LifecycleEvent> value) {
        if (this.isFullscreen()) {
            this.fullScreenDialogView.setOnCloseRequest(value);
        } else {
            this.onCloseRequest.set(value);
        }

    }

    public final EventHandler<LifecycleEvent> getOnCloseRequest() {
        return this.isFullscreen() ? this.fullScreenDialogView.getOnCloseRequest() : this.onCloseRequest.get();
    }

    public final ObservableList<ButtonBase> getButtons() {
        return this.buttons;
    }

    public final boolean isFullscreen() {
        return this.isFullscreen;
    }

    public final Optional<T> showAndWait() {
        if (!this.isFullscreen && this.rootNode.getScene() != null && !this.rootNode.getScene().getWindow().isShowing()) {
            throw new IllegalStateException("Stage must be showing before making the Dialog visible.");
        } else {
            if (this.isFullscreen) {
                View showingView = AppManager.getInstance().getView();
                if (showingView == null) {
                    throw new IllegalStateException("A View needs to be showing before showing the full screen Dialog");
                }

                if (!showingView.getScene().getWindow().isShowing()) {
                    throw new IllegalStateException("Stage must be showing before making the Dialog visible.");
                }
            }

            //
//            Event.fireEvent(this, new LifecycleEvent(this, LifecycleEvent.SHOWING));
            this.rebuild();
            //
//            Event.fireEvent(this, new LifecycleEvent(this, LifecycleEvent.SHOWN));
            if (!this.isFullscreen()) {
                this.dialogView.show();
            } else {
                AppManager.getInstance().switchView(this.fullScreenDialogView.getName());
            }

            return Optional.ofNullable(this.result);
        }
    }

    public final void setResult(T result) {
        this.result = result;
    }

    public final void hide() {
        Event.fireEvent(this, new LifecycleEvent(this, LifecycleEvent.HIDING));
        LifecycleEvent closeRequestEvent = new LifecycleEvent(this, LifecycleEvent.CLOSE_REQUEST);
        Event.fireEvent(this, closeRequestEvent);
        if (!closeRequestEvent.isConsumed()) {
            if (!this.isFullscreen()) {
                this.dialogView.hide();
            } else if (this.isShowing()) {
                AppManager.getInstance().switchToPreviousView();
            }

            Event.fireEvent(this, new LifecycleEvent(this, LifecycleEvent.HIDDEN));
        }
    }

    static void installIntoMobileApplication(DialogView dialogView, BorderPane rootNode, BooleanProperty showingProperty) {
        showingProperty.bind(dialogView.showingProperty());
        showingProperty.addListener((o, ov, showing) -> {
            if (showing) {
                rootNode.getScene().getWindow().setOnCloseRequest((event) -> {
                    Platform.exitNestedEventLoop(rootNode, null);
                });
                Platform.enterNestedEventLoop(rootNode);
            } else {
                rootNode.getScene().getWindow().setOnCloseRequest((event) -> {
                });
                Platform.exitNestedEventLoop(rootNode, null);
            }

        });
    }

    private String generateId() {
        return "dialog-" + UUID.randomUUID().toString();
    }

    private void rebuild() {
        if (!this.isFullscreen()) {
            this.rootNode.getChildren().clear();
            if (this.getGraphic() != null) {
                StackPane pane = new StackPane(this.getGraphic());
                pane.getStyleClass().add("graphic-container");
                this.rootNode.setTop(pane);
            }

            VBox container = new VBox();
            container.getStyleClass().add("container");
            Node title = this.titleProperty.get();
            if (title != null) {
                container.getChildren().add(title);
            }

            Node content = this.contentProperty.get();
            if (content != null) {
                container.getChildren().add(content);
                VBox.setVgrow(content, Priority.ALWAYS);
            }

            this.rootNode.setCenter(container);
            FlowPane buttonBar = new FlowPane();
            buttonBar.getStyleClass().add("dialog-button-bar");

            for (ButtonBase btn : this.buttons) {
                btn.getStyleClass().addAll("flat", "light");
                buttonBar.getChildren().add(btn);
            }

            this.rootNode.setBottom(buttonBar);
        } else {
            Node node = this.titleProperty.get();
            if (node != null) {
                this.fullScreenDialogView.setTitle(node);
            }

            node = this.contentProperty.get();
            if (node != null) {
                this.fullScreenDialogView.setContent(node);
            }

            this.fullScreenDialogView.getButtons().setAll(this.getButtons());
        }

    }
}
