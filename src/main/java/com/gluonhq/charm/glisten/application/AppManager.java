package com.gluonhq.charm.glisten.application;

import com.gluonhq.attach.display.DisplayService;
import com.gluonhq.attach.lifecycle.LifecycleService;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.LifecycleEvent;
import com.gluonhq.charm.glisten.control.NavigationDrawer;
import com.gluonhq.charm.glisten.control.Snackbar;
import com.gluonhq.charm.glisten.layout.Layer;
import com.gluonhq.charm.glisten.mvc.SplashView;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.Swatch;
import com.gluonhq.charm.glisten.visual.Theme;
import com.gluonhq.impl.charm.glisten.application.GlassPaneHelper;
import com.gluonhq.impl.charm.glisten.util.CachedFactory;
import com.gluonhq.impl.charm.glisten.util.DeviceSettings;
import com.gluonhq.impl.charm.glisten.util.GlistenSettings;
import com.gluonhq.impl.charm.glisten.util.StylesheetTools;
import com.gluonhq.impl.charm.glisten.util.ViewTools;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Dimension2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.lang.ref.WeakReference;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class AppManager {
    public static final String HOME_VIEW = "home";
    public static final String SPLASH_VIEW = "splash";
    private static final String CHARM_VERSION = "7.0.0";
    private static AppManager APPLICATION_MANAGER;
    private final Consumer<Scene> postInit;
    private Stage primaryStage;
    private Scene primaryScene;
    private GlassPane glassPane;
    private StatusBar statusBar;
    private AppBar appBar;
    private NavigationDrawer drawer;
    private final CachedFactory<View> viewFactory = new CachedFactory<>();
    private final CachedFactory<Layer> layerFactory = new CachedFactory<>();
    private final Deque<String> viewStack = new ArrayDeque<>();
    private static final GlistenSettings settings = new GlistenSettings();
    private final ReadOnlyObjectWrapper<View> viewProperty = new ReadOnlyObjectWrapper<>(null, "view") {
        private WeakReference<View> viewRef = null;

        public void invalidated() {
            View oldView = this.viewRef == null ? null : this.viewRef.get();
            View newView = this.get();
            if (oldView == null || oldView != newView) {
                if (oldView != null) {
                    Event.fireEvent(oldView, new LifecycleEvent(oldView, LifecycleEvent.HIDDEN));
                }

                this.viewRef = new WeakReference<>(newView);
                if (newView != null) {
                    if (AppManager.this.glassPane == null) {
                        throw new RuntimeException("The GlassPane was not initialized yet. Consider calling switchView() from postInit() instead");
                    }

                    AppManager.this.glassPane.setRoot(newView);
                    AppManager.this.doSwitchView(ViewTools.findViewName(newView), ViewStackPolicy.USE);
                    Event.fireEvent(newView, new LifecycleEvent(newView, LifecycleEvent.SHOWING));
                }

            }
        }
    };
    private final ObjectProperty<Swatch> swatchProperty = new SimpleObjectProperty<>(this, "swatch") {
        protected void invalidated() {
            Swatch swatch = Optional.ofNullable(AppManager.this.getSwatch()).orElse(Swatch.getDefault());
            swatch.assignTo(AppManager.this.primaryScene);
        }
    };

    public static AppManager getInstance() {
        return APPLICATION_MANAGER;
    }

    public static AppManager initialize(Consumer<Scene> postInit) {
        if (APPLICATION_MANAGER != null) {
            throw new RuntimeException("The AppManager was initialized already.");
        } else {
            APPLICATION_MANAGER = new AppManager(postInit);
            return APPLICATION_MANAGER;
        }
    }

    public static AppManager initialize() {
        return initialize(null);
    }

    private AppManager(Consumer<Scene> postInit) {
        this.postInit = postInit;
    }

    public void start(Stage primaryStage) {
        this.start(primaryStage, (glassPane)-> {
            Dimension2D dim = DisplayService.create().map(DisplayService::getDefaultDimensions).orElse(new Dimension2D(this.getScreenWidth(), this.getScreenHeight()));
            Scene scene = new Scene(glassPane, dim.getWidth(), dim.getHeight());
            Swatch.getDefault().assignTo(scene);
            Theme.getDefault().assignTo(scene);
            return scene;
        });
    }

    public void start(Stage primaryStage, Function<GlassPane, Scene> sceneBuilder) {
        this.primaryStage = primaryStage;
        this.appBar = new AppBar();
        this.glassPane = new GlassPane();
        this.primaryScene = sceneBuilder.apply(this.glassPane);
        StylesheetTools.addStylesheet(this.primaryScene, this.getPlatformSpecificStylesheetName());
        primaryStage.setScene(this.primaryScene);
        boolean splashInited = false;
        if (this.viewFactory.containsKey(SPLASH_VIEW) && this.viewFactory.get(SPLASH_VIEW).get() instanceof SplashView) {
            splashInited = true;
            this.switchView(SPLASH_VIEW);
        }

        if (splashInited) {
            boolean finalSplashInited = splashInited;
            Platform.runLater(() -> this.continueInit(finalSplashInited));
        } else {
            this.continueInit(splashInited);
        }

    }

    private void continueInit(boolean splashInited) {
        this.glassPane.postInit();
        if (com.gluonhq.attach.util.Platform.isAndroid()) {
            this.primaryScene.addEventHandler(KeyEvent.KEY_RELEASED, (e) -> {
                if (KeyCode.ESCAPE.equals(e.getCode())) {

                    for (Layer layer : GlassPaneHelper.getLayers()) {
                        if (layer.isShowing()) {
                            MobileEvent event = new MobileEvent(layer, MobileEvent.BACK_BUTTON_PRESSED);
                            Event.fireEvent(layer, event);
                            if (event.isConsumed()) {
                                return;
                            }
                        }
                    }

                    boolean success = getInstance().switchToPreviousView().isPresent();
                    if (!success && !getInstance().existsPreviousView()) {
                        LifecycleService.create().ifPresent(LifecycleService::shutdown);
                    }
                }

            });
        }

//        License licenseAnnotation = this.getClass().getAnnotation(License.class);
//        String licenseKey = LicenseManager.validateLicense(CHARM_VERSION, licenseAnnotation, () -> {
//            (new NagScreenPresenter()).showDialog();
//        });
//        if (licenseKey == null) {
//            (new NagScreenPresenter()).showDialog();
//        }
//        TrackingManager.trackUsage("7.0.0", licenseKey);

        if (!splashInited) {
            this.switchView(HOME_VIEW);
        }

        try {
            if (this.postInit != null) {
                this.postInit.accept(this.primaryScene);
            }
        } catch (Throwable var5) {
            var5.printStackTrace();
        }

        this.primaryStage.show();
    }

    public StringProperty titleProperty() {
        return this.primaryStage.titleProperty();
    }

    public void setTitle(String title) {
        this.primaryStage.setTitle(title);
    }

    public String getTitle() {
        return this.primaryStage.getTitle();
    }

    public ReadOnlyObjectProperty<View> viewProperty() {
        return this.viewProperty.getReadOnlyProperty();
    }

    private void setView(View view) {
        this.viewProperty.set(view);
    }

    public View getView() {
        return this.viewProperty.get();
    }

    public ObjectProperty<Swatch> swatchProperty() {
        return this.swatchProperty;
    }

    public Swatch getSwatch() {
        return this.swatchProperty.get();
    }

    public void setSwatch(Swatch swatch) {
        this.swatchProperty.set(swatch);
    }

    public GlassPane getGlassPane() {
        return this.glassPane;
    }

    public AppBar getAppBar() {
        return this.appBar;
    }

    public NavigationDrawer getDrawer() {
        if (this.drawer == null) {
            this.drawer = new NavigationDrawer();
        }

        return this.drawer;
    }

    public StatusBar getStatusBar() {
        if (this.statusBar == null) {
            this.statusBar = new StatusBar();
        }

        return this.statusBar;
    }

    public <T extends View> Optional<T> switchView(String viewName) {
        return this.switchView(viewName, ViewStackPolicy.USE);
    }

    public <T extends View> Optional<T> switchView(String viewName, ViewStackPolicy viewStackPolicy) {
        if (this.getView() != null) {
            Event onCloseRequest = new LifecycleEvent(this.getView(), LifecycleEvent.CLOSE_REQUEST);
            Event.fireEvent(this.getView(), onCloseRequest);
            if (onCloseRequest.isConsumed()) {
                return Optional.empty();
            }
        }

        return this.doSwitchView(viewName, viewStackPolicy);
    }

    public <T extends View> Optional<T> switchToPreviousView() {
        if (this.getView() != null) {
            LifecycleEvent onCloseRequest = new LifecycleEvent(this.getView(), LifecycleEvent.CLOSE_REQUEST);
            Event.fireEvent(this.getView(), onCloseRequest);
            if (onCloseRequest.isConsumed()) {
                return Optional.empty();
            }
        }

        if (this.viewStack.isEmpty()) {
            return Optional.empty();
        } else {
            String viewName = this.viewStack.pop();
            return Optional.ofNullable(viewName).flatMap((vn) -> {
                return this.doSwitchView(vn, ViewStackPolicy.SKIP);
            });
        }
    }

    public <T extends View> Optional<T> goHome() {
        return this.switchView(HOME_VIEW, ViewStackPolicy.CLEAR);
    }

    public Optional<View> retrieveView(String viewName) {
        return this.viewFactory.get(viewName);
    }

    public void addViewFactory(String viewName, Supplier<View> supplier) {
        if (this.viewFactory.containsKey(viewName)) {
            throw new IllegalArgumentException("View with name '" + viewName + "' already exists - names must be unique");
        } else {
            this.viewFactory.put(viewName, supplier);
        }
    }

    public boolean removeViewFactory(String viewName) {
        if (this.viewFactory.containsKey(viewName)) {
            this.viewFactory.remove(viewName);
            return true;
        } else {
            return false;
        }
    }

    public boolean isViewPresent(String viewName) {
        return this.viewFactory.containsKey(viewName);
    }

    public void addLayerFactory(String layerName, Supplier<Layer> supplier) {
        if (this.layerFactory.containsKey(layerName)) {
            throw new IllegalArgumentException("Layer with name '" + layerName + "' already exists - names must be unique");
        } else {
            this.layerFactory.put(layerName, supplier);
        }
    }

    public boolean removeLayerFactory(String layerName) {
        if (this.layerFactory.containsKey(layerName)) {
            this.layerFactory.remove(layerName);
            return true;
        } else {
            return false;
        }
    }

    public boolean isLayerPresent(String layerName) {
        return this.layerFactory.containsKey(layerName);
    }

    public void showLayer(String layerName) {
        this.layerFactory.get(layerName).ifPresent((layer) -> {
            layer.setId(layerName);
            layer.show();
        });
    }

    public void hideLayer(String layerName) {
        this.layerFactory.get(layerName).ifPresent(Layer::hide);
    }

    public void hideAllLayers(boolean hideAllLayers) {
        GlassPaneHelper.setHideAllLayers(hideAllLayers);
    }

    public double getScreenHeight() {
        return Screen.getPrimary().getVisualBounds().getHeight();
    }

    public double getScreenWidth() {
        return Screen.getPrimary().getVisualBounds().getWidth();
    }

    public void showMessage(String message) {
        this.showMessage(message, null, null);
    }

    public void showMessage(String message, String buttonText, EventHandler<ActionEvent> evtHandler) {
        Snackbar snackbar = new Snackbar(message, buttonText, evtHandler);
        snackbar.show();
    }

    protected URLStreamHandlerFactory createUserURLStreamHandlerFactory() {
        return null;
    }

    private boolean existsPreviousView() {
        return !this.viewStack.isEmpty();
    }

    private <T extends View> Optional<T> doSwitchView(String viewName, ViewStackPolicy viewStackPolicy) {
        if (ViewStackPolicy.CLEAR == viewStackPolicy) {
            return this.viewFactory.get(viewName).map((newView) -> {
                this.viewStack.clear();
                this.setView(newView);
                return (T)newView;
            });
        } else {
            View currentView = this.getView();
            String currentViewName = ViewTools.findViewName(currentView);
            return currentView != null && currentViewName != null && currentViewName.equalsIgnoreCase(viewName) ? Optional.empty() : this.viewFactory.get(viewName).map((newView) -> {
                String newViewName = ViewTools.findViewName(newView);
                if (newViewName == null || newViewName.isEmpty()) {
                    ViewTools.storeViewName(newView, viewName);
                }

                if (ViewStackPolicy.USE == viewStackPolicy && currentView != null) {
                    this.viewStack.push(currentViewName);
                }

                if (currentView != null) {
                    Event.fireEvent(currentView, new LifecycleEvent(currentView, LifecycleEvent.HIDING));
                }

                this.setView(newView);
                return (T)newView;
            });
        }
    }

    private String getPlatformSpecificStylesheetName() {
        com.gluonhq.attach.util.Platform currentPlatform = com.gluonhq.attach.util.Platform.getCurrent();
        String platformSuffix = com.gluonhq.attach.util.Platform.isDesktop() ? "" : "_" + currentPlatform.toString().toLowerCase(Locale.ROOT);
        String formFactorSuffix = DeviceSettings.hasNotch() ? "_notch" : "";
        DisplayService.create().map((s) -> s.isTablet() ? "_tablet" : "").orElse("");
        return String.format("glisten%s%s.css", platformSuffix, formFactorSuffix);
    }

    public static class MobileEvent extends Event {
        public static final EventType<AppManager.MobileEvent> ANY;
        public static final EventType<AppManager.MobileEvent> BACK_BUTTON_PRESSED;

        public MobileEvent(EventTarget source, EventType<? extends Event> eventType) {
            super(source, source, eventType);
        }

        static {
            ANY = new EventType<>(Event.ANY, "MOBILE_EVENT");
            BACK_BUTTON_PRESSED = new EventType<>(ANY, "BACK_BUTTON_PRESSED");
        }
    }
}
