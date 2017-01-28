package org.marsik.ham.kx3tool.ui;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import javax.inject.Inject;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import jssc.SerialPortException;
import org.marsik.ham.kx3tool.audio.AudioCapture;
import org.marsik.ham.kx3tool.audio.FftResult;
import org.marsik.ham.kx3tool.cdi.JobExecutor;
import org.marsik.ham.kx3tool.cdi.Timer;
import org.marsik.ham.kx3tool.configuration.Configuration;
import org.marsik.ham.kx3tool.configuration.Macro;
import org.marsik.ham.kx3tool.radio.RadioConnection;
import org.marsik.ham.kx3tool.radio.RadioInfo;
import org.marsik.ham.kx3tool.serial.SerialUtil;
import org.marsik.ham.kx3tool.waterfall.Waterfall;

public class MainController implements Initializable {
    @FXML private VBox root;
    @FXML private Pane dataPane;
    @FXML private Tab dataTab;

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

    @FXML private ChoiceBox<Mixer.Info> audioDevice;
    @FXML private ChoiceBox<DataLine.Info> inputDac;
    @FXML private Label inputDacLabel;
    @FXML private ChoiceBox<Integer> fftSize;
    @FXML private ChoiceBox<Integer> fftRefresh;

    @FXML private Button startAudio;
    @FXML private Button stopAudio;

    @FXML private Canvas waterfallCanvas;
    @FXML private Pane waterfallPane;

    @FXML private Label dbBaseLevel;
    @FXML private Label dbDynamicRange;

    @FXML private Label labelFrequency;

    @FXML private AnchorPane smithPane;
    @FXML private Canvas smithCanvas;

    private WritableImage currentWaterfallImage;
    private AtomicReference<int[]> latestWaterfallLineData = new AtomicReference<>(null);
    private AtomicReference<FftResult> latestFftResult = new AtomicReference<>(null);
    private Runnable redrawWaterfall = new RedrawWaterfall();

    @Inject
    private Configuration configuration;

    @Inject @JobExecutor
    private ExecutorService executor;

    private AudioCapture audioCapture;

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

    @Inject @Timer
    private ScheduledExecutorService scheduler;

    private Scheduler javaFxScheduler = Schedulers.from(Platform::runLater);

    private Waterfall waterfall = new Waterfall();

    public static List<KeyCodeCombination> FUNCTION_KEYS = Stream.of(KeyCode.F1, KeyCode.F2, KeyCode.F3,
            KeyCode.F4, KeyCode.F5, KeyCode.F6, KeyCode.F7, KeyCode.F8)
            .map(KeyCodeCombination::new)
            .collect(Collectors.toList());

    public void initialize(URL location, ResourceBundle resources) {
        rigConnect.setDisable(false);
        vnaConnect.setDisable(false);
        atuConnect.setDisable(false);

        rigBaudRate.disableProperty().bind(rigConnect.disabledProperty());
        rigSerialPort.disableProperty().bind(rigConnect.disabledProperty());
        rigDisconnect.disableProperty().bind(rigConnect.disabledProperty().not());
        dataSend.disableProperty().bind(rigDisconnect.disabledProperty());

        vnaDisconnect.disableProperty().bind(vnaConnect.disabledProperty().not());
        vnaBaudRate.disableProperty().bind(vnaConnect.disabledProperty());
        vnaSerialPort.disableProperty().bind(vnaConnect.disabledProperty());

        atuDisconnect.disableProperty().bind(atuConnect.disabledProperty().not());
        atuBaudRate.disableProperty().bind(atuConnect.disabledProperty());
        atuProtocol.disableProperty().bind(atuConnect.disabledProperty());
        atuSerialPort.disableProperty().bind(atuConnect.disabledProperty());

        txInProgress.managedProperty().bind(txInProgress.visibleProperty());
        txInProgress.visibleProperty().bind(txBuffer.textProperty().isEmpty().not());

        //dataSend.setDisable(true);
        rigBaudRate.getItems().addAll(serialUtil.getAvailableBaudRates());
        rigBaudRate.setValue(38400);

        atuBaudRate.getItems().addAll(serialUtil.getAvailableBaudRates());
        atuBaudRate.setValue(38400);

        atuProtocol.getItems().addAll("REMOTE_ATU_OK7MS");
        atuProtocol.setValue(atuProtocol.getItems().get(0));

        vnaBaudRate.getItems().addAll(serialUtil.getAvailableBaudRates());
        vnaBaudRate.setValue(38400);

        Stream.of(rigSerialPort, atuSerialPort, vnaSerialPort).forEach(combo -> {
            combo.disabledProperty().addListener(x -> {
                refreshSerialPortList(combo);
            });
            serialUtil.getAvailabilityStream().map(x -> combo).filter(c -> !c.isDisable()).forEach(this::refreshSerialPortList);
            refreshSerialPortList(combo);
        });

        // Start clock
        scheduler.scheduleAtFixedRate(() -> notify(radioInfo),
                0, 1, TimeUnit.SECONDS);

        // Workaround https://bugs.openjdk.java.net/browse/JDK-8094695 by setting the filter
        // so the function key events do not reach the text areas
        dataPane.addEventFilter(KeyEvent.KEY_PRESSED, this::processFunctionKeysEarly);

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

        radioConnection.getRxQueue().subscribeOn(javaFxScheduler)
                .subscribe(s -> appendKeepSelection(dataRx, s));
        radioConnection.getTxQueue().subscribeOn(javaFxScheduler)
                .subscribe(s -> appendKeepSelection(txBuffer, s));
        radioConnection.getTxTransmittedQueue().subscribeOn(javaFxScheduler)
                .subscribe(c -> {
                    removeFromStartKeepSelection(txBuffer, c.getContent());
                    if (c.isSuccessful()) {
                        appendKeepSelection(dataRx, c.getContent());
                    }
                });
        radioConnection.getInfoQueue().subscribe(this::notify);

        final List<Mixer.Info> availableDevices = AudioCapture.getAvailableDevices();
        audioDevice.setConverter(new StringConverter<Mixer.Info>() {
            @Override
            public String toString(Mixer.Info object) {
                return object.getDescription();
            }

            @Override
            public Mixer.Info fromString(String string) {
                return null;
            }
        });


        audioDevice.getItems().addAll(availableDevices);
        inputDacLabel.visibleProperty().bind(inputDac.visibleProperty());

        audioDevice.valueProperty().addListener((observable, oldValue, newValue) -> {
            final List<DataLine.Info> inputLines = AudioCapture.getInputLines(newValue);
            inputDac.setVisible(inputLines.size() > 1);
            inputDac.getItems().clear();
            inputDac.getItems().addAll(inputLines);
            inputDac.setValue(inputLines.get(0));
        });

        fftSize.getItems().addAll(256, 512, 1024, 2048, 4096, 6144, 8192);
        fftSize.setValue(1024);
        fftRefresh.getItems().addAll(5, 10, 15, 20, 30, 45, 60, 100);
        fftRefresh.setValue(20);

        audioDevice.setValue(availableDevices.get(0));
        audioDevice.disableProperty().bind(startAudio.disabledProperty());

        inputDac.disableProperty().bind(startAudio.disabledProperty());
        stopAudio.disableProperty().bind(startAudio.disabledProperty().not());
        fftSize.disableProperty().bind(startAudio.disabledProperty());
        fftRefresh.disableProperty().bind(startAudio.disabledProperty());

        waterfall.setDynamicRange(100);
        waterfall.setReferenceLevel(0);
        currentWaterfallImage = new WritableImage((int)waterfallCanvas.getWidth(), (int)waterfallCanvas.getHeight());

        waterfallCanvas.widthProperty().bind(waterfallPane.widthProperty());
        waterfallCanvas.heightProperty().bind(waterfallPane.heightProperty());

        updateWaterfallLevels();

        smithCanvas.widthProperty().bind(smithPane.widthProperty());
        smithCanvas.heightProperty().bind(smithPane.heightProperty());

        Platform.runLater(this::redrawSmithChart);
    }

    private static double[] IMPEDANCE_CIRCLES = new double[] {0.2f, 0.5f, 1f, 2f, 5f, 10f};

    private void redrawSmithChart() {
        double height = smithCanvas.getHeight() - 4;
        double cy = 2 + height / 2;

        double width = smithCanvas.getWidth() - 4;
        double cx = 2 + width / 2;

        double radius = Math.min(height, width) / 2;

        GraphicsContext gc = smithCanvas.getGraphicsContext2D();

        // prepare clip
        gc.beginPath();
        gc.arc(cx, cy, radius, radius, 0, Math.PI * 2 * radius);
        gc.closePath();
        gc.clip();

        // set color
        gc.setStroke(Color.GRAY);

        // border and 0 reactance line
        gc.strokeOval(2, 2, 2*radius, 2*radius);
        gc.strokeLine(2, 2 + radius, 2 + 2*radius, 2 + radius);

        // constant impedance circles
        for(double r: IMPEDANCE_CIRCLES){
            double circleRadius = radius / (1 + r);
            gc.strokeOval(width - 2*circleRadius, cy - circleRadius, 2*circleRadius, 2*circleRadius);
        }

        // constant reactance circles
        for(double r: IMPEDANCE_CIRCLES){
            double circleRadius = radius / r;
            gc.strokeOval(width - circleRadius, cy, 2*circleRadius, 2*circleRadius);
            gc.strokeOval(width - circleRadius, cy - 2*circleRadius, 2*circleRadius, 2*circleRadius);
        }
    }

    private void updateWaterfallLevels() {
        dbBaseLevel.setText((int)waterfall.getReferenceLevel() + " dB");
        dbDynamicRange.setText((int)waterfall.getDynamicRange() + " dB");
    }

    private void addButtonAccelerator(Button button, KeyCodeCombination accel) {
        dataPane.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (accel.match(ev)) {
                button.fire();
            }
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
        if (combobox.isDisable()) {
            return;
        }

        final List<String> availablePorts = serialUtil.getAvailablePorts();
        // XXX Make this more efficient
        // Add new ports and remove missing ports without touching the rest
        availablePorts.stream()
                .filter(p -> !combobox.getItems().contains(p))
                .forEach(combobox.getItems()::add);

        combobox.getItems().removeIf(value -> !availablePorts.contains(value));

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
            rigConnect.setDisable(true);
            refreshSerialPortList(atuSerialPort);
            refreshSerialPortList(vnaSerialPort);
        } catch (SerialPortException ex) {
            ex.printStackTrace();
        }
    }

    public void onRigDisconnect(ActionEvent event) {
        try {
            radioConnection.close();
            rigConnect.setDisable(false);
            refreshSerialPortList(atuSerialPort);
            refreshSerialPortList(vnaSerialPort);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onDataSend(ActionEvent event) {
        radioConnection.sendData(dataTx.getText());
        dataTx.clear();
        dataTx.requestFocus();
    }

    public void onTxClear(ActionEvent event) {
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
            radioText += " " + (info.getFrequency() != 0 ? info.getFrequency() : "")
                    + " " + (info.getMode() != null ? info.getMode().name() : "")
                    + " " + (info.isTx() ? "TX" : "RX");
        }

        labelFrequency.setText(info.getFrequency() != 0 ? String.format("%,d MHz", info.getFrequency()) : "");

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

    public void onStartAudio(ActionEvent event) {
        try {
            audioCapture = new AudioCapture(audioDevice.getValue(), inputDac.getValue(), executor,
                    fftSize.getValue(), fftRefresh.getValue());
            audioCapture.getFftResults().subscribe(this::updateWaterfall);
            startAudio.setDisable(true);
            audioCapture.open();
            audioCapture.start();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void onStopAudio() {
        audioCapture.stop();
        audioCapture.close();
        audioCapture = null;
        startAudio.setDisable(false);
    }

    public void updateWaterfall(final FftResult result) {
        executor.execute(() -> {
            final int[] pixels = waterfall.pixelLine(result);
            latestWaterfallLineData.set(pixels);
            latestFftResult.set(result);
            Platform.runLater(redrawWaterfall);
        });
    }

    private class RedrawWaterfall implements Runnable {
        @Override
        public void run() {
            final int[] waterfallLine = latestWaterfallLineData.get();
            final WritableImage newWaterfallImage;

            // Allocate new Image, but only when the needed size changed
            if (waterfallLine.length != (int)currentWaterfallImage.getWidth()
                    || (int) waterfallCanvas.getHeight() != (int)currentWaterfallImage.getHeight()) {
                newWaterfallImage = new WritableImage(waterfallLine.length,
                        (int) waterfallCanvas.getHeight());
            } else {
                newWaterfallImage = currentWaterfallImage;
            }

            // Scroll and resize the current content
            int border = (int)((newWaterfallImage.getWidth() - currentWaterfallImage.getWidth()) / 2);
            int height = (int)Math.min(currentWaterfallImage.getHeight(), newWaterfallImage.getHeight());
            int width = (int)Math.min(currentWaterfallImage.getWidth(), newWaterfallImage.getWidth());

            int cutoffborder = 0;
            if (border < 0) {
                cutoffborder = -border;
                border = 0;
            }

            newWaterfallImage.getPixelWriter().setPixels(border, 1,
                    width, height - 1,
                    currentWaterfallImage.getPixelReader(), cutoffborder, 0);
            currentWaterfallImage = newWaterfallImage;

            // Write the new line
            currentWaterfallImage.getPixelWriter().setPixels(
                    0, 0,
                    waterfallLine.length, 1,
                    WritablePixelFormat.getIntArgbInstance(),
                    waterfallLine,
                    0, waterfallLine.length);

            // Plot the full waterfall to screen
            waterfallCanvas.getGraphicsContext2D().drawImage(currentWaterfallImage,
                    0, 0, currentWaterfallImage.getWidth(), currentWaterfallImage.getHeight(),
                    0, 0, waterfallCanvas.getWidth(), waterfallCanvas.getHeight());
        }
    }

    public void onWaterfallClick(MouseEvent event) {
        if (latestFftResult.get() == null) {
            return;
        }

        EventTarget target = event.getTarget();
        if (!(target instanceof Canvas)) {
            return;
        }

        Canvas canvas = (Canvas) event.getTarget();

        double freqOffset = latestFftResult.get().frequency(0, (int)(event.getX() - canvas.getWidth()/2));
        radioConnection.tuneOffset(freqOffset);
    }

    public void onWaterfallMove(MouseEvent event) {
        if (latestFftResult.get() == null) {
            return;
        }

        EventTarget target = event.getTarget();
        if (!(target instanceof Canvas)) {
            return;
        }

        Canvas canvas = (Canvas) event.getTarget();

        double freq = latestFftResult.get().frequency(radioInfo.getFrequency(), (int)(event.getX() - canvas.getWidth()/2));
        statusLine.setText("F " + freq);
    }

    public void onLowerBase(ActionEvent event) {
        waterfall.lowerReference(5.0);
        updateWaterfallLevels();
    }

    public void onHigherBase(ActionEvent event) {
        waterfall.higherReference(5.0);
        updateWaterfallLevels();
    }

    public void onLessBandwidth(ActionEvent event) {
        waterfall.decreaseDynamicRange(5.0);
        updateWaterfallLevels();
    }

    public void onMoreBandwidth(ActionEvent event) {
        waterfall.increateDynamicRange(5.0);
        updateWaterfallLevels();
    }

    public void processFunctionKeysEarly(KeyEvent event) {
        if (FUNCTION_KEYS.stream().anyMatch(e -> e.match(event))) {
            root.fireEvent(event);
            event.consume();
        }
    }
}
