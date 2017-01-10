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
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.ContextMenuEvent;
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

public class MainController implements Initializable, RadioConnection.InfoUpdated {
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

    private RadioConnection.AdditionalDataEvent dataReceived;
    private RadioConnection.AdditionalDataEvent dataQueued;
    private RadioConnection.DataPushedFromQueue dataSent = count -> {};
    private RadioConnection.DataPushedFromQueue dataTransmitted;

    public void initialize(URL location, ResourceBundle resources) {
        dataReceived = s -> Platform.runLater(() -> appendKeepSelection(dataRx, s));
        dataQueued = s -> Platform.runLater(() -> appendKeepSelection(txBuffer, s));
        dataTransmitted = c -> Platform.runLater(() -> {
            String removed = removeFromStartKeepSelection(txBuffer, c);
            appendKeepSelection(dataRx, removed);
        });

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

        updateMacroButton(macro1, configuration.getMacro(1));
        macro1.setOnMouseClicked(new MacroButtonClicked(1));
        macro1.setOnContextMenuRequested(new MacroButtonConfigureEvent(1));

        updateMacroButton(macro2, configuration.getMacro(2));
        macro2.setOnMouseClicked(new MacroButtonClicked(2));
        macro2.setOnContextMenuRequested(new MacroButtonConfigureEvent(2));

        updateMacroButton(macro3, configuration.getMacro(3));
        macro3.setOnMouseClicked(new MacroButtonClicked(3));
        macro3.setOnContextMenuRequested(new MacroButtonConfigureEvent(3));

        updateMacroButton(macro4, configuration.getMacro(4));
        macro4.setOnMouseClicked(new MacroButtonClicked(4));
        macro4.setOnContextMenuRequested(new MacroButtonConfigureEvent(4));

        updateMacroButton(macro5, configuration.getMacro(5));
        macro5.setOnMouseClicked(new MacroButtonClicked(5));
        macro5.setOnContextMenuRequested(new MacroButtonConfigureEvent(5));

        updateMacroButton(macro6, configuration.getMacro(6));
        macro6.setOnMouseClicked(new MacroButtonClicked(6));
        macro6.setOnContextMenuRequested(new MacroButtonConfigureEvent(6));

        updateMacroButton(macro7, configuration.getMacro(7));
        macro7.setOnMouseClicked(new MacroButtonClicked(7));
        macro7.setOnContextMenuRequested(new MacroButtonConfigureEvent(7));

        updateMacroButton(macro8, configuration.getMacro(8));
        macro8.setOnMouseClicked(new MacroButtonClicked(8));
        macro8.setOnContextMenuRequested(new MacroButtonConfigureEvent(8));
    }

    private void updateMacroButton(Button button, Macro macro) {
        button.setText(macro.getName());
    }

    private class MacroButtonClicked implements EventHandler<MouseEvent> {
        final int id;

        public MacroButtonClicked(int id) {
            this.id = id;
        }

        @Override
        public void handle(MouseEvent event) {
            appendKeepSelection(dataTx, configuration.getMacro(id).getValue());
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
                ((Button) event.getSource()).setText(macro.getName());
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

    public void onRigConnect(MouseEvent event) {
        try {
            radioConnection.addInfoUpdateListener(this);
            radioConnection.addReceivedDataListener(dataReceived);
            radioConnection.addQueuedDataListener(dataQueued);
            radioConnection.addDataSentListener(dataSent);
            radioConnection.addDataTransmittedListener(dataTransmitted);

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

    public void onRigDisconnect(MouseEvent event) {
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

    public void onDataSend(MouseEvent event) {
        radioConnection.sendData(dataTx.getText());
        dataTx.clear();
        txInProgress.setVisible(true);
    }

    public void onTxClear(MouseEvent event) {
        dataTx.clear();
    }

    @Override
    public void notify(RadioInfo info) {
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

    private void appendKeepSelection(TextArea area, String s) {
        IndexRange selected = area.getSelection();
        int pos = area.getCaretPosition();
        int len = area.getLength();

        area.appendText(s);

        area.selectRange(selected.getStart(), selected.getEnd());
        if (pos == len) area.positionCaret(pos);
    }

    private String removeFromStartKeepSelection(TextArea area, int count) {
        // Meke sure this is executed from the right thread (UI)
        IndexRange selected = area.getSelection();
        int pos = area.getCaretPosition();
        int len = area.getLength();

        final String oldContent = area.getText();
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
}
