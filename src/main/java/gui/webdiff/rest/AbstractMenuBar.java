package gui.webdiff.rest;

import org.refactoringminer.astDiff.models.DiffMetaInfo;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;

import static org.rendersnake.HtmlAttributesFactory.class_;
import static org.rendersnake.HtmlAttributesFactory.href;

/* Created by pourya on 2024-06-03*/
public abstract class AbstractMenuBar implements Renderable {
    private final String toolName;
    private final int id;
    private final int numOfDiffs;
    private final String routePath;
    private final boolean isMovedDiff;
    private final DiffMetaInfo metaInfo;
    private static final String BACK_BUTTON_TEXT = "Overview";
    private static final String PREV_BUTTON_TEXT = "Prev";
    private static final String NEXT_BUTTON_TEXT = "Next";
    private static final String QUIT_BUTTON_TEXT = "Quit";
    private static final String movedDiffWarningText = "This diff only shows the moves between these two files";

    private String getPrevButtonText() {
        String txt = PREV_BUTTON_TEXT;
        int rem = id;
//        if (rem == 0) return txt;
        return txt + " (" + rem + " remaining)";
    }
    private String getNextButtonText() {
        String txt = NEXT_BUTTON_TEXT;
        int rem = numOfDiffs - id - 1;
//        if (rem == 0) return txt;
        return txt + " (" + rem + " remaining)";
    }

    private String getNextHRef(){
        return routePath + (id + 1) % numOfDiffs;

    }
    private String getPrevHRef(){
        return routePath + (id - 1 + numOfDiffs) % numOfDiffs;
    }

    public AbstractMenuBar(String toolName, String routePath, int id, int numOfDiffs, boolean isMovedDiff, DiffMetaInfo metaInfo) {
        this.toolName = toolName;
        this.routePath = routePath;
        this.id = id;
        this.numOfDiffs = numOfDiffs;
        this.isMovedDiff = isMovedDiff;
        this.metaInfo = metaInfo;
    }
    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        boolean shouldDisablePrev = id == 0;
        boolean shouldDisableNext = id == numOfDiffs - 1;
        html

                .div(class_("col"))
                    .write("Generated by " + toolName + " | ")
                    .if_(metaInfo != null)
                    .a(href(metaInfo.getUrl()).target("_blank")).content(metaInfo.getInfo())
                    ._if()
                 ._div()
//                .if_(isMovedDiff).div(class_("col")).content(movedDiffWarningText)._if()
                .div(class_("col"))
                .div(class_("btn-toolbar justify-content-end"))
                .div(class_("btn-group mr-2"))
                .button(class_("btn btn-primary btn-sm").id("legend")
                        .add("data-bs-container", "body")
                        .add("data-bs-toggle", "popover")
                        .add("data-bs-placement", "bottom")
                        .add("data-bs-html", "true")
                        .add("data-bs-content", getLegendValue(), false)
                ).content("Legend")
                .button(class_("btn btn-primary btn-sm").id("shortcuts")
                        .add("data-bs-toggle", "popover")
                        .add("data-bs-placement", "bottom")
                        .add("data-bs-html", "true")
                        .add("data-bs-content", getShortcutDescriptions(), false)
                )
                .content("Shortcuts")
                ._div()
                .div(class_("btn-group"))
                .a(class_("btn btn-default btn-sm btn-primary").href("/list")).content(BACK_BUTTON_TEXT)
                .if_(id >= 0)
                .a(class_("btn btn-default btn-sm btn-primary" + (shouldDisablePrev ? " disabled" : ""))
                        .href(shouldDisablePrev ? "#" : getPrevHRef()))
                .content(getPrevButtonText())
                .a(class_("btn btn-default btn-sm btn-primary" + (shouldDisableNext ? " disabled" : ""))
                        .href(shouldDisableNext ? "#" : getNextHRef()))
                .content(getNextButtonText())
                ._if()
                .a(class_("btn btn-default btn-sm btn-danger").href("/quit")).content(QUIT_BUTTON_TEXT)
                ._div()
                ._div()
                ._div();
    }

    public abstract String getLegendValue() ;

    public String getShortcutDescriptions() {
        return "<b>Alt + q</b> quit<br><b>Alt + l</b> list<br>"
                + "<b>Alt + t</b> top<br><b>Alt + b</b> bottom <br>";
    }
}
