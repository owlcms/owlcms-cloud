package app.owlcms.fly.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class LogDialog extends Dialog {

    Logger logger = LoggerFactory.getLogger(LogDialog.class);
    private Pre logArea;
    private Button closeButton;
    private Button clearButton;

    public LogDialog() {
        this.setModal(false);
        this.setDraggable(true);
        this.setResizable(true);
        this.setWidth("80vw");
        this.setHeight("70vh");
        this.setHeaderTitle("Operation Logs");

        // Create log area
        logArea = new Pre();
        logArea.setWidthFull();
        logArea.getStyle().set("overflow", "auto");
        logArea.getStyle().set("padding", "1em");
        logArea.getStyle().set("background-color", "#f5f5f5");
        logArea.getStyle().set("border", "1px solid #ddd");
        logArea.getStyle().set("border-radius", "4px");
        logArea.getStyle().set("flex-grow", "1");
        logArea.getStyle().set("min-height", "0");
        logArea.getStyle().set("margin", "0");
        logArea.getStyle().set("box-sizing", "border-box");
        logArea.setId("logArea");

        // Create buttons
        closeButton = new Button("Close", e -> this.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        clearButton = new Button("Clear", e -> clear(UI.getCurrent()));
        clearButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        HorizontalLayout buttonLayout = new HorizontalLayout(clearButton, closeButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        buttonLayout.setPadding(false);
        buttonLayout.setMargin(false);

        // Create main layout
        VerticalLayout mainLayout = new VerticalLayout(logArea, buttonLayout);
        mainLayout.setFlexGrow(1, logArea);
        mainLayout.setSizeFull();
        mainLayout.setPadding(false);
        mainLayout.setMargin(false);
        mainLayout.setSpacing(true);

        add(mainLayout);
        
        // Remove default padding from dialog
        getElement().getStyle().set("padding", "0");
    }

    public void clear(UI ui) {
        ui.access(() -> {
            logArea.setText("");
            ui.push();
        });
    }

    public void appendLine(String line, UI ui, String prompt) {
        ui.access(() -> {
            logger.info(prompt + " " + line);
            String curValue = logArea.getText();
            String newValue = curValue + (curValue.isEmpty() ? "" : System.lineSeparator()) + line;
            logArea.setText(newValue);
            logArea.getElement().executeJs(
                "var objDiv = document.getElementById('logArea');" +
                "objDiv.scrollTop = objDiv.scrollHeight;" +
                "objDiv.scrollIntoView(false);"
            );
            ui.push();
        });
    }

    public void append(String string, UI ui) {
        appendLine(string, ui, ">>>");
    }

    public void appendError(String string, UI ui) {
        appendLine(string, ui, "***");
    }

    public void show() {
        this.open();
    }

    public void hide() {
        this.close();
    }
}
