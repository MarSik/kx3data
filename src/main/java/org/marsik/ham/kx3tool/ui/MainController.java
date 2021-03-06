package org.marsik.ham.kx3tool.ui;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import javax.inject.Inject;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import jssc.SerialPortException;
import org.marsik.ham.kx3tool.configuration.Configuration;
import org.marsik.ham.kx3tool.configuration.Macro;
import org.marsik.ham.kx3tool.radio.RadioConnection;
import org.marsik.ham.kx3tool.radio.RadioInfo;
import org.marsik.ham.kx3tool.serial.SerialUtil;

public class MainController implements Initializable {
    @FXML private VBox root;

    @FXML private Button dataSend;

    @FXML private TextArea dataTx;
    @FXML private TextArea dataRx;

    @FXML private ChoiceBox<Integer> rigBaudRate;
    @FXML private ComboBox<String> rigSerialPort;
    @FXML private Button rigConnect;
    @FXML private Button rigDisconnect;

    @FXML private ChoiceBox<Integer> atuBaudRate;
    @FXML private ComboBox<String> atuSerialPort;
    @FXML private ChoiceBox<String> atuProtocol;
    @FXML private Button atuConnect;
    @FXML private Button atuDisconnect;

    @FXML private ChoiceBox<Integer> vnaBaudRate;
    @FXML private ComboBox<String> vnaSerialPort;
    @FXML private Button vnaConnect;
    @FXML private Button vnaDisconnect;

    @FXML private Label radioLine;
    @FXML private Label statusLine;

    @FXML private HBox txInProgress;
    @FXML private TextArea txBuffer;
    @FXML private Button abortTx;

    @FXML private Button macro1;
    @FXML private Button macro2;
    @FXML private Button macro3;
    @FXML private Button macro4;
    @FXML private Button macro5;
    @FXML private Button macro6;
    @FXML private Button macro7;
    @FXML private Button macro8;

    @FXML private CheckBox autoScrollCheckBox;

    @Inject
    private Configuration configuration;

    private static final DateTimeFormatter SIMPLE_LOCAL_TIME;
    static {
        SIMPLE_LOCAL_TIME = new DateTimeFormatterBuilder()
                .appendValue(HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(SECOND_OF_MINUTE, 2)
                .toFormatter();
    }

    @Inject
    private SerialUtil serialUtil;

    @Inject
    private RadioConnection radioConnection;

    @Inject
    private RadioInfo radioInfo;

    @Inject
    private DialogLoader dialogLoader;

    private Timer clockTimer = new Timer("Clock timer", true);

    public void initialize(URL location, ResourceBundle resources) {
        rigConnect.setDisable(false);
        rigDisconnect.setDisable(true);
        vnaConnect.setDisable(false);
        vnaDisconnect.setDisable(true);
        atuConnect.setDisable(false);
        atuDisconnect.setDisable(true);
        dataSend.setDisable(true);

        txInProgress.managedProperty().bind(txInProgress.visibleProperty());
        txInProgress.setVisible(false);

        //dataSend.setDisable(true);
        rigBaudRate.getItems().addAll(serialUtil.getAvailableBaudRates());
        rigBaudRate.setValue(38400);

        atuBaudRate.getItems().addAll(serialUtil.getAvailableBaudRates());
        atuBaudRate.setValue(38400);

        atuProtocol.getItems().addAll("REMOTE_ATU_OK7MS");
        atuProtocol.setValue(atuProtocol.getItems().get(0));

        vnaBaudRate.getItems().addAll(serialUtil.getAvailableBaudRates());
        vnaBaudRate.setValue(38400);

        refreshSerialPortList(rigSerialPort);
        refreshSerialPortList(atuSerialPort);
        refreshSerialPortList(vnaSerialPort);

        // Start clock
        clockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                MainController.this.notify(radioInfo);
            }
        }, 0, 1000);

        updateMacroButton(1, macro1, configuration.getMacro(1));
        macro1.setOnAction(new MacroButtonClicked(1));
        macro1.setOnContextMenuRequested(new MacroButtonConfigureEvent(1));
        addButtonAccelerator(macro1, new KeyCodeCombination(KeyCode.F1));

        updateMacroButton(2, macro2, configuration.getMacro(2));
        macro2.setOnAction(new MacroButtonClicked(2));
        macro2.setOnContextMenuRequested(new MacroButtonConfigureEvent(2));
        addButtonAccelerator(macro2, new KeyCodeCombination(KeyCode.F2));

        updateMacroButton(3, macro3, configuration.getMacro(3));
        macro3.setOnAction(new MacroButtonClicked(3));
        macro3.setOnContextMenuRequested(new MacroButtonConfigureEvent(3));
        addButtonAccelerator(macro3, new KeyCodeCombination(KeyCode.F3));

        updateMacroButton(4, macro4, configuration.getMacro(4));
        macro4.setOnAction(new MacroButtonClicked(4));
        macro4.setOnContextMenuRequested(new MacroButtonConfigureEvent(4));
        addButtonAccelerator(macro4, new KeyCodeCombination(KeyCode.F4));

        updateMacroButton(5, macro5, configuration.getMacro(5));
        macro5.setOnAction(new MacroButtonClicked(5));
        macro5.setOnContextMenuRequested(new MacroButtonConfigureEvent(5));
        addButtonAccelerator(macro5, new KeyCodeCombination(KeyCode.F5));

        updateMacroButton(6, macro6, configuration.getMacro(6));
        macro6.setOnAction(new MacroButtonClicked(6));
        macro6.setOnContextMenuRequested(new MacroButtonConfigureEvent(6));
        addButtonAccelerator(macro6, new KeyCodeCombination(KeyCode.F6));

        updateMacroButton(7, macro7, configuration.getMacro(7));
        macro7.setOnAction(new MacroButtonClicked(7));
        macro7.setOnContextMenuRequested(new MacroButtonConfigureEvent(7));
        addButtonAccelerator(macro7, new KeyCodeCombination(KeyCode.F7));

        updateMacroButton(8, macro8, configuration.getMacro(8));
        macro8.setOnAction(new MacroButtonClicked(8));
        macro8.setOnContextMenuRequested(new MacroButtonConfigureEvent(8));
        addButtonAccelerator(macro8, new KeyCodeCombination(KeyCode.F8));

        addButtonAccelerator(dataSend, new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHORTCUT_DOWN));

        radioConnection.getRxQueue().subscribe(s -> Platform.runLater(() -> appendKeepSelection(dataRx, s)));
        radioConnection.getTxQueue().subscribe(s -> Platform.runLater(() -> appendKeepSelection(txBuffer, s)));
        radioConnection.getTxTransmittedQueue().subscribe(c -> Platform.runLater(() -> {
            removeFromStartKeepSelection(txBuffer, c);
            appendKeepSelection(dataRx, c);
            if (txBuffer.getText().isEmpty()) {
                txInProgress.setVisible(false);
            }
        }));
        radioConnection.getInfoQueue().subscribe(this::notify);
    }

    private void addButtonAccelerator(Button button, KeyCodeCombination accel) {
        Platform.runLater(() -> {
            button.getScene().getAccelerators().put(accel, button::fire);
        });
    }

    private void updateMacroButton(int id, Button button, Macro macro) {
        button.setText("[F" + id + "] " + macro.getName());
    }

    private class MacroButtonClicked implements EventHandler<ActionEvent> {
        final int id;

        public MacroButtonClicked(int id) {
            this.id = id;
        }

        @Override
        public void handle(ActionEvent event) {
            insertReplaceSelection(dataTx, configuration.getMacro(id).getValue());
            dataTx.requestFocus();
        }
    }

    private class MacroButtonConfigureEvent implements EventHandler<ContextMenuEvent> {
        final int idx;

        public MacroButtonConfigureEvent(int idx) {
            this.idx = idx;
        }

        @Override
        public void handle(ContextMenuEvent event) {
            Dialog<EditMacroController> macroDialog = dialogLoader.showDialog(
                    (Stage)((Node) event.getSource()).getScene().getWindow(),
                    "edit_macro.fxml",
                    "Edit macro " + idx
            );

            macroDialog.getController().load(configuration.getMacro(idx));
            macroDialog.showAndWait();
            if (macroDialog.getController().isOk()) {
                final Macro macro = macroDialog.getController().getValue();
                configuration.setMacro(idx, macro);
                updateMacroButton(idx, (Button) event.getSource(), macro);
            }
        }
    }

    private void refreshSerialPortList(ComboBox<String> combobox) {
        final List<String> availablePorts = serialUtil.getAvailablePorts();
        // XXX Make this more efficient
        // Add new ports and remove missing ports without touching the rest
        availablePorts.stream()
                .filter(p -> !combobox.getItems().contains(p))
                .forEach(combobox.getItems()::add);

        for (Iterator<String> it = combobox.getItems().iterator(); it.hasNext(); ) {
            String value = it.next();
            if (! availablePorts.contains(value)) {
                it.remove();
            }
        }

        if (combobox.getItems().isEmpty()) {
            combobox.setValue(null);
            return;
        }

        if (!combobox.getItems().contains(combobox.getValue())) {
            combobox.setValue(combobox.getItems().get(0));
        }
    }

    public void onRigConnect(ActionEvent event) {
        try {
            radioConnection.open(rigSerialPort.getValue(), rigBaudRate.getValue());
            rigBaudRate.setDisable(true);
            rigSerialPort.setDisable(true);
            rigConnect.setDisable(true);
            rigDisconnect.setDisable(false);
            refreshSerialPortList(atuSerialPort);
            refreshSerialPortList(vnaSerialPort);
            dataSend.setDisable(false);
        } catch (SerialPortException ex) {
            ex.printStackTrace();
        }
    }

    public void onRigDisconnect(ActionEvent event) {
        try {
            radioConnection.close();
            rigBaudRate.setDisable(false);
            rigSerialPort.setDisable(false);
            rigConnect.setDisable(false);
            rigDisconnect.setDisable(true);
            refreshSerialPortList(atuSerialPort);
            refreshSerialPortList(vnaSerialPort);
            dataSend.setDisable(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onDataSend(ActionEvent event) {
        radioConnection.sendData(dataTx.getText());
        dataTx.clear();
        txInProgress.setVisible(true);
        dataTx.requestFocus();
    }

    public void onTxClear(MouseEvent event) {
        dataTx.clear();
        dataTx.requestFocus();
    }

    /**
     * Update radio status
     * @param info
     */
    private void notify(RadioInfo info) {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        LocalTime now = LocalTime.now(utc.toZoneId());

        String radioText = now.format(SIMPLE_LOCAL_TIME);

        if (info.getRadioModel() != RadioInfo.RadioModel.UNKNOWN) {
            radioText += " " + (info.getFrequency() != null ? info.getFrequency().toString() : "")
                    + " " + (info.getMode() != null ? info.getMode().name() : "")
                    + " " + (info.isTx() ? "TX" : "RX");
        }

        final String statusText = info.getRadioModel().name();

        // Meke sure this is executed from the right thread (UI)
        final String finalRadioText = radioText;
        Platform.runLater(() -> radioLine.setText(finalRadioText));
        Platform.runLater(() -> statusLine.setText(statusText));
    }

    private void insertReplaceSelection(TextArea area, String s) {
        IndexRange selected = area.getSelection();
        area.replaceSelection(s);
        area.positionCaret(selected.getStart() + s.length());
    }

    private void appendKeepSelection(TextArea area, String s) {
        IndexRange selected = area.getSelection();
        double scroll = area.getScrollTop();

        area.appendText(s);

        area.selectRange(selected.getStart(), selected.getEnd());
        if (autoScrollCheckBox.isSelected()) {
            area.setScrollTop(Double.MAX_VALUE);
        } else {
            area.setScrollTop(scroll);
        }
    }

    private String removeFromStartKeepSelection(TextArea area, String prefix) {
        IndexRange selected = area.getSelection();
        int pos = area.getCaretPosition();
        int len = area.getLength();

        final String oldContent = area.getText();
        int count = findEndPrefixMatch(prefix, oldContent);

        if (count == 0) {
            return "";
        }

        String newContent = oldContent.substring(count);
        area.setText(newContent);

        int start = selected.getStart() - count;
        if (start >= 0) {
            selected = new IndexRange(start, selected.getEnd() - count);
        } else if (selected.getEnd() + start >= 0) {
            selected = new IndexRange(0, selected.getEnd() + start);
        }

        area.selectRange(selected.getStart(), selected.getEnd());
        if (pos == len) area.positionCaret(pos);

        return oldContent.substring(0, count);
    }

    /**
     * Find the substring that is a tail of prefix and a prefix for content
     * and return its length.
     *
     * for example:
     * Current    ABCDEFG
     * Prefix  XYZABCD
     * result -> EFG -> 3
     */
    static int findEndPrefixMatch(String prefix, String content) {
        int count = prefix.length();
        while (count > 0) {
            if (!content.startsWith(prefix.substring(prefix.length() - count))) {
                count--;
            } else {
                break;
            }
        }
        return count;
    }
}
