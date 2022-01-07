module com.gluonhq.charm.glisten {
    requires java.logging;
    requires javafx.controls;
    requires com.gluonhq.attach.device;
    requires com.gluonhq.attach.display;
    requires com.gluonhq.attach.storage;
    requires com.gluonhq.attach.lifecycle;
    requires com.gluonhq.attach.statusbar;
    requires com.gluonhq.attach.util;

    exports com.gluonhq.impl.charm.glisten.control.skin to javafx.controls;
    exports com.gluonhq.charm.glisten.animation;
    exports com.gluonhq.charm.glisten.application;
    exports com.gluonhq.charm.glisten.control;
    exports com.gluonhq.charm.glisten.control.settings;
    exports com.gluonhq.charm.glisten.layout;
    exports com.gluonhq.charm.glisten.layout.layer;
    exports com.gluonhq.charm.glisten.license;
    exports com.gluonhq.charm.glisten.mvc;
    exports com.gluonhq.charm.glisten.visual;
}