
package com.gluonhq.impl.charm.glisten.control.skin;

import com.gluonhq.charm.glisten.control.TextArea;

public class TextAreaSkin extends TextInputSkin {
    private static final int TEXT_AREA_CONTENT_PADDING = 7;

    public TextAreaSkin(TextArea control, javafx.scene.control.TextArea innerTextControl) {
        super(control, innerTextControl, TEXT_AREA_CONTENT_PADDING);
        ((javafx.scene.control.TextArea)this.innerTextControl).setWrapText(true);
    }

    protected void layoutChildren(double x, double y, double w, double h) {
        super.layoutChildren(x, y, w, h);
    }
}
