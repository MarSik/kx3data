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
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import jssc.SerialPortException;
import org.marsik.ham.kx3tool.radio.RadioConnection;
import org.marsik.ham.kx3tool.radio.RadioInfo;
import org.marsik.ham.kx3tool.serial.SerialUtil;

public class MainController implements Initializable, RadioConnection.InfoUpdated {
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


    public void onSetMacro1(MouseEvent event) {

    }

    public void onMacro1(MouseEvent event) {

    }

    public void onSetMacro2(MouseEvent event) {

    }

    public void onMacro2(MouseEvent event) {

    }

    public void onSetMacro3(MouseEvent event) {

    }

    public void onMacro3(MouseEvent event) {

    }

    public void onSetMacro4(MouseEvent event) {

    }

    public void onMacro4(MouseEvent event) {

    }

    public void onSetMacro5(MouseEvent event) {

    }

    public void onMacro5(MouseEvent event) {

    }

    public void onSetMacro6(MouseEvent event) {

    }

    public void onMacro6(MouseEvent event) {

    }

    public void onSetMacro7(MouseEvent event) {

    }

    public void onMacro7(MouseEvent event) {

    }

    public void onSetMacro8(MouseEvent event) {

    }

    public void onMacro8(MouseEvent event) {

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
