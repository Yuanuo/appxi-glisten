package com.gluonhq.charm.glisten.application;

import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.LifecycleEvent;
import com.gluonhq.charm.glisten.layout.Layer;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.impl.charm.glisten.application.GlassPaneHelper;
import com.gluonhq.impl.charm.glisten.application.GlassPaneHelper.GlassPaneAccessor;
import javafx.animation.Animation.Status;
import javafx.animation.Transition;
import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GlassPane extends BorderPane {
    private static final ObservableList<Layer> layers = FXCollections.observableArrayList();
    private static boolean hideAllLayers;
    private static boolean opaqueLayerPressed;
    private Region opaqueLayer;
    private StatusBar statusBar;
    private AppBar appBar;
    private Transition viewTransition;
    private Node nodeInTransition;
    private Layer topLayer;
    private boolean shouldUpdateFade;
    private final InvalidationListener showingListener = o -> {
        this.shouldUpdateFade = true;
        this.requestLayout();
    };
    private final ObjectProperty<Node> rootProperty = new SimpleObjectProperty<>(this, "root") {
        private Node oldRoot = null;

        private void replaceRoot(Node newRoot) {
            if (this.oldRoot != null) {
                GlassPane.this.getChildren().remove(this.oldRoot);
            }

            this.oldRoot = newRoot;
        }

        protected void invalidated() {
            Node newRoot = this.get();
            if (GlassPane.this.getChildren().contains(newRoot) && GlassPane.this.viewTransition != null && GlassPane.this.viewTransition.getStatus() == Status.RUNNING) {
                GlassPane.this.viewTransition.stop();
                this.onFinished(GlassPane.this.nodeInTransition);
            }

            GlassPane.this.getChildren().add(this.oldRoot == null ? 0 : 1, newRoot);
            if (newRoot instanceof View) {
                GlassPane.this.viewTransition = ((View) newRoot).getShowTransition();
                GlassPane.this.viewTransition.setOnFinished(e -> this.onFinished(newRoot));
                GlassPane.this.nodeInTransition = newRoot;
                GlassPane.this.layoutChildren();
                GlassPane.this.viewTransition.play();
            } else {
                this.replaceRoot(newRoot);
            }

        }

        private void onFinished(Node newRoot) {
            this.replaceRoot(newRoot);
            Event.fireEvent(newRoot, new LifecycleEvent(newRoot, LifecycleEvent.SHOWN));
        }
    };
    private final DoubleProperty backgroundFade = new SimpleDoubleProperty(this, "backgroundFade");

    GlassPane() {
        layers.addListener((ListChangeListener<? super Layer>) c -> {
            label30:
            while(true) {
                if (c.next()) {
                    Iterator<?> var2;
                    if (c.wasAdded()) {
                        opaqueLayerPressed = false;
                        this.getChildren().addAll(c.getAddedSubList());
                        var2 = c.getAddedSubList().iterator();

                        while(true) {
                            if (!var2.hasNext()) {
                                continue label30;
                            }

                            ((Layer)var2.next()).showingProperty().addListener(this.showingListener);
                        }
                    }

                    if (!c.wasRemoved()) {
                        continue;
                    }

                    this.getChildren().removeAll(c.getRemoved());
                    var2 = c.getRemoved().iterator();

                    while(true) {
                        if (!var2.hasNext()) {
                            continue label30;
                        }

                        ((Layer)var2.next()).showingProperty().removeListener(this.showingListener);
                    }
                }

                this.updateFadeLevel();
                return;
            }
        });
    }

    void postInit() {
        this.statusBar = AppManager.getInstance().getStatusBar();
        this.appBar = AppManager.getInstance().getAppBar();
        this.appBar.visibleProperty().addListener(observable -> this.requestLayout());
        this.opaqueLayer = new Region();
        this.opaqueLayer.setMouseTransparent(true);
        this.opaqueLayer.setPickOnBounds(false);
        this.opaqueLayer.setOnMouseReleased(e -> {
            opaqueLayerPressed = true;
            if (layers.size() > 0) {
                Layer layer = layers.get(layers.size() - 1);
                if (layer.isShowing() && layer.isAutoHide()) {
                    layer.hide();
                }
            }
        });

        this.opaqueLayer.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
        this.opaqueLayer.opacityProperty().bind(this.backgroundFade);
        this.getChildren().addAll(this.statusBar.fakeStatusBar, this.appBar, this.opaqueLayer);
    }

    final void setRoot(Node newRoot) {
        this.rootProperty.set(newRoot);
    }

    public final Node getRoot() {
        return this.rootProperty.get();
    }

    final ObjectProperty<Node> rootProperty() {
        return this.rootProperty;
    }

    private void setBackgroundFade(double fadeLevel) {
        this.backgroundFade.set(fadeLevel);
    }

    private double getBackgroundFade() {
        return this.backgroundFade.get();
    }

    private DoubleProperty backgroundFadeProperty() {
        return this.backgroundFade;
    }

    protected void layoutChildren() {
        if (this.shouldUpdateFade) {
            this.updateFadeLevel();
            this.shouldUpdateFade = false;
        }

        double w = this.getWidth();
        double h = this.getHeight();
        double appBarHeight = this.appBar != null && this.appBar.isVisible() ? this.appBar.prefHeight(w) : 0.0D;
        if (this.appBar != null) {
            this.appBar.resizeRelocate(0.0D, 0.0D, w, appBarHeight);
        }

        Node root = this.getRoot();
        if (root != null) {
            root.resizeRelocate(0.0D, appBarHeight, w, h - appBarHeight);
        }

        if (this.opaqueLayer != null) {
            this.opaqueLayer.resizeRelocate(0.0D, 0.0D, w, h);
        }

        boolean isShowing = false;
//        this.toRemove.clear();
        List<Node> toRemove = new ArrayList<>();

        for (Layer layer : layers) {
            isShowing = isShowing || layer.isShowing() && !layer.isMouseTransparent();
            layer.layoutChildren();
            if (!layer.isShowing() && !layer.isVisible()) {
                toRemove.add(layer);
            }
        }

        layers.removeAll(toRemove);
        if (this.opaqueLayer != null) {
            if (this.topLayer != null && !this.topLayer.isAutoHide()) {
                //
                this.opaqueLayer.setMouseTransparent(false);
            } else {
                this.opaqueLayer.setMouseTransparent(!isShowing);
            }
        }

    }

    private void updateFadeLevel() {
        if (!layers.isEmpty()) {
            Layer layer = layers.get(layers.size() - 1);
            if (layer != this.topLayer) {
                this.topLayer = layer;
                this.backgroundFadeProperty().unbind();
                this.backgroundFadeProperty().bind(layer.backgroundFadeProperty());
            }
        } else {
            if (this.topLayer != null) {
                this.backgroundFadeProperty().unbind();
                this.setBackgroundFade(0.0D);
            }

            this.topLayer = null;
        }

    }

    static {
        GlassPaneHelper.setGlassPaneAccessor(new GlassPaneAccessor() {
            public ObservableList<Layer> getLayers() {
                return GlassPane.layers;
            }

            public boolean isHideAllLayers() {
                return GlassPane.hideAllLayers && GlassPane.opaqueLayerPressed;
            }

            public void setHideAllLayers(boolean hideAllLayers) {
                GlassPane.hideAllLayers = hideAllLayers;
            }
        });
    }
}
