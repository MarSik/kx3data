package org.marsik.ham.kx3tool.ui;

import javax.inject.Inject;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import jssc.SerialPortException;
import org.marsik.ham.kx3tool.radio.RadioConnection;
import org.marsik.ham.kx3tool.radio.RadioInfo;
import org.marsik.ham.kx3tool.serial.SerialUtil;

public class MainController implements Initializable, RadioConnection.InfoUpdated, RadioConnection.DataDecoded {
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

    @FXML private Label rigInfo;

    @Inject
    private SerialUtil serialUtil;

    @Inject
    private RadioConnection radioConnection;

    public void initialize(URL location, ResourceBundle resources) {
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
            radioConnection.addDataListener(this);
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
        final String statusText = info.getRadioModel().name()
                + " " + (info.getFrequency() != null ? info.getFrequency().toString() : "")
                + " " + (info.getMode() != null ? info.getMode().name() : "")
                + " " + (info.isTx() ? "TX" : "RX");

        // Meke sure this is executed from the right thread (UI)
        Platform.runLater(() -> rigInfo.setText(statusText));
    }

    @Override
    public void received(String s) {
        // Meke sure this is executed from the right thread (UI)
        Platform.runLater(() -> dataRx.appendText(s));
    }
}
