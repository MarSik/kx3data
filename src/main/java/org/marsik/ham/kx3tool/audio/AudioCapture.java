package org.marsik.ham.kx3tool.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import javax.sound.sampled.TargetDataLine;
import java.nio.Buffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.commons.math3.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class AudioCapture implements AutoCloseable, Callable<Void> {
    private static final Logger logger = LoggerFactory.getLogger(AudioCapture.class);
    public static final int FFT_SIZE = 512;
    public final int FFT_RATE = 15;

    private final Mixer mixer; // the sound card
    private final TargetDataLine line; // the actual DAC

    private final byte[] data;
    private final AudioFormat format;
    private Buffer dataWrapper;

    private final ExecutorService executor;
    private Future<?> captureRunning;

    private final FrequencyAnalyzer frequencyAnalyzer;
    private final IQReader.IQSample iqSampler;
    private final IQReader iqReader;

    private final Subject<FftResult> fftResults = PublishSubject.create();

    public AudioCapture(Mixer.Info mixerInfo, DataLine.Info lineInfo, ExecutorService executor) throws LineUnavailableException {
        this.executor = executor;

        mixer = AudioSystem.getMixer(mixerInfo);
        line = (TargetDataLine) mixer.getLine(lineInfo);
        format = line.getFormat();

        data = new byte[computeBufferSize((int)format.getFrameRate(), format.getFrameSize(), FFT_RATE)];
        frequencyAnalyzer = new FrequencyAnalyzer();
        iqSampler = new HammingIqSampler(FFT_SIZE);

        if (format.getSampleSizeInBits() == 16) {
            iqReader = new PcmIQReader(iqSampler, format.isBigEndian());
        } else {
            throw new IllegalArgumentException("Unsupported audio format");
        }
    }

    private int computeBufferSize(int frameRate, int frameSize, int fftFrameRate) {
        return frameRate * frameSize / fftFrameRate;
    }

    /**
     * This configures the selected soundcard (mixer) to capture from the selected channel (port) only
     * using the selected DAC (line).
     *
     * @throws LineUnavailableException
     */
    public void open() throws LineUnavailableException {
        line.open();
        logger.info("Opened audio input stream {} {} {} chan {}Hz buffer {}B frame size {}B sample size {}b",
                mixer.getMixerInfo().getDescription(),
                format.getEncoding(),
                format.getChannels(),
                format.getFrameRate(),
                data.length,
                format.getFrameSize(),
                format.getSampleSizeInBits());
    }

    public void start() {
        // Start the thread
        captureRunning = executor.submit(this);
    }

    public void stop() {
        captureRunning.cancel(true);
    }

    public void close() {
        line.close();
    }

    public static List<Mixer.Info> getAvailableDevices() {
        return Arrays.stream(AudioSystem.getMixerInfo())
                .filter(info -> !getInputLines(info).isEmpty())
                .collect(Collectors.toList());
    }

    public static List<DataLine.Info> getInputLines(Mixer.Info mixer) {
        return Arrays.stream(AudioSystem.getMixer(mixer).getTargetLineInfo())
                .filter(info -> DataLine.class.isAssignableFrom(info.getLineClass()))
                .map(line -> (DataLine.Info) line)
                .collect(Collectors.toList());
    }

    public static List<Port.Info> getInputPorts(Mixer.Info mixer) {
        return Arrays.stream(AudioSystem.getMixer(mixer).getTargetLineInfo())
                .filter(info -> Port.class.isAssignableFrom(info.getLineClass()))
                .map(line -> (Port.Info) line)
                .collect(Collectors.toList());
    }

    public AudioFormat getFormat() {
        return format;
    }

    /**
     * Do not invoke manually! This method is used to capture the data from the audio device,
     * and the start() method starts it in a separate thread.
     */
    @Override
    public Void call() throws Exception {
        logger.debug("Audio capture starting.");
        line.start();
        logger.debug("Audio capture started.");

        int received = 0;
        while (!Thread.currentThread().isInterrupted()) {
            final int read = line.read(data, received, data.length - received);
            logger.trace("Received {} bytes of audio", read);

            received += read;
            if (received == data.length) {
                received = 0;

                // prepare data for FFT
                Complex[] iqData = iqReader.read(null, Arrays.copyOfRange(data, data.length - FFT_SIZE * format.getFrameSize(), data.length));
                Complex[] freqData = frequencyAnalyzer.analyze(iqData);
                FftResult fftResult = new FftResult(format.getSampleRate(), freqData);
                logger.debug("Frequency data: {}", fftResult.amplitudes());
                fftResults.onNext(fftResult);
            }
        }

        line.stop();
        logger.debug("Audio capture stopped.");

        return null;
    }

    public Observable<FftResult> getFftResults() {
        return fftResults.hide();
    }
}
