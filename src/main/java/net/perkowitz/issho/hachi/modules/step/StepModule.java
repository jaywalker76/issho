package net.perkowitz.issho.hachi.modules.step;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import net.perkowitz.issho.devices.*;
import net.perkowitz.issho.hachi.Chordable;
import net.perkowitz.issho.hachi.Clockable;
import net.perkowitz.issho.hachi.Saveable;
import net.perkowitz.issho.hachi.Sessionizeable;
import net.perkowitz.issho.hachi.modules.*;
import org.codehaus.jackson.map.ObjectMapper;

import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;
import java.io.File;
import java.util.List;
import java.util.Set;

import static net.perkowitz.issho.hachi.modules.step.Stage.Marker.*;
import static net.perkowitz.issho.hachi.modules.step.StepUtil.*;

/**
 * Created by optic on 10/24/16.
 */
public class StepModule extends MidiModule implements Module, Clockable, GridListener, Sessionizeable, Chordable, Saveable, Muteable {

    ObjectMapper objectMapper = new ObjectMapper();

    private StepMemory memory = new StepMemory();
    private StepDisplay stepDisplay;
    private SettingsSubmodule settingsModule;
    private boolean settingsView = false;

    private Set<Integer> onNotes = Sets.newHashSet();

    private String filePrefix = "monomodule";
    private int currentFileIndex = 0;
    private int midiChannel = 0;

    private int currentStageIndex = 0;
    private int currentStageStepIndex = 0;
    private List<Step> currentSteps = null;
    private Stage.Marker currentMarker = Note;
    private List<Integer> stagesToRedraw = Lists.newArrayList();


    /***** Constructor ****************************************/

    public StepModule(Transmitter inputTransmitter, Receiver outputReceiver, String filePrefix) {
        super(inputTransmitter, outputReceiver);
        this.stepDisplay = new StepDisplay(this.display);
        this.filePrefix = filePrefix;
        load(0);
        currentSteps = currentStage().getSteps();
        this.settingsModule = new SettingsSubmodule();
    }


    /***** private implementation ****************************************/

    private void advance(boolean andReset) {

        for (Integer redrawIndex : stagesToRedraw) {
            stepDisplay.drawStage(memory, redrawIndex);
        }
        stagesToRedraw.clear();

        if (andReset) {
            currentStageIndex = 0;
            currentStageStepIndex = 0;
            currentSteps = currentStage().getSteps();
            // need to make sure stage0 has at least one step
        }

        stagesToRedraw.add(currentStageIndex);

        // we assume that the current stage/step exists (make sure of that during increment)
        // and we don't recompute current steps until we go to next stage
        playStep(currentSteps.get(currentStageStepIndex));

        nextStep();
    }

    private void playStep(Step step) {

        List<Integer> noteIndices = currentStage().findMarker(Stage.Marker.Note);
        for (Integer index : noteIndices) {
            stepDisplay.drawActiveNote(GridPad.at(currentStageIndex, 7 - index));
        }

        switch (step.getMode()) {
            case Play:
                notesOff();
                onNotes.add(step.getNote());
                sendMidiNote(memory.getMidiChannel(), step.getNote(), step.getVelocity());
                break;
            case Tie:
                break;
            case Rest:
                notesOff();
                break;
            case Slide:
                sendMidiNote(memory.getMidiChannel(), step.getNote(), step.getVelocity());
                if (!onNotes.contains(step.getNote())) {
                    notesOff();
                    onNotes.add(step.getNote());
                }
                break;
        }
    }

    private void nextStep() {
        currentStageStepIndex++;
        currentSteps = currentStage().getSteps();
        while (currentStageStepIndex >= currentSteps.size()) {
            // todo this could loop forever if all stages are SKIP
//            stagesToRedraw.add(currentStageIndex);
            currentStageIndex++;
            currentStageIndex = currentStageIndex % StepPattern.STAGE_COUNT;
            currentSteps = currentStage().getSteps();
            currentStageStepIndex = 0;
        }
    }

    private Stage currentStage() {
        return memory.currentPattern().getStage(currentStageIndex);
    }

    private void notesOff() {
        for (Integer note : onNotes) {
            sendMidiNote(memory.getMidiChannel(), note, 0);

        }
        onNotes.clear();
    }


    /***** Module implementation ***********************************/

    public void redraw() {
        stepDisplay.initialize();
        if (settingsView) {
            settingsModule.redraw();
            stepDisplay.drawControls();
        } else {
            stepDisplay.redraw(memory);
        }
    }

    public void setDisplay(GridDisplay display) {
        this.display = display;
        this.stepDisplay.setDisplay(display);
        this.settingsModule.setDisplay(display);
    }

    public void shutdown() {
        notesOff();
    }


    /***** Muteable implementation ***********************************/

    public void mute(boolean muted) {
        this.isMuted = muted;
        notesOff();
    }

    public boolean isMuted() {
        return isMuted;
    }


    /***** Chordable implementation ***********************************/

    public void setChordNotes(List<Integer> notes) {

    }


    /***** Sessionizeable implementation *************************************/

    public void selectSession(int index) {
    }

    public void selectPatterns(int firstIndex, int lastIndex) {
    }


    /***** GridListener interface ****************************************/

    public void onPadPressed(GridPad pad, int velocity) {
        onControlPressed(new GridControl(pad, null), velocity);
    }

    public void onPadReleased(GridPad pad) {
        onControlReleased(new GridControl(pad, null));
    }

    public void onButtonPressed(GridButton button, int velocity) {
        onControlPressed(new GridControl(button, null), velocity);
    }

    public void onButtonReleased(GridButton button) {
        onControlReleased(new GridControl(button, null));
    }

    private void onControlPressed(GridControl control, int velocity) {

        if (control.equals(StepUtil.settingsControl)) {
            settingsView = !settingsView;
            stepDisplay.setSettingsView(settingsView);
            this.redraw();

        } else if (control.equals(StepUtil.muteControl)) {
            isMuted = !isMuted;
            stepDisplay.setMuted(isMuted);
            stepDisplay.drawControls();

        } else if (settingsView) {
               onControlPressedSettings(control, velocity);

        } else if (markerControls.contains(control)) {
            currentMarker = markerPaletteMap.get(control);

        } else if (control.getPad() != null) {
            GridPad pad = control.getPad();
            Stage stage = memory.currentPattern().getStage(pad.getX());
            int index = 7 - pad.getY();
            if (currentMarker == stage.getMarker(index)) {
                stage.putMarker(index, Stage.Marker.None);
                control.draw(display, StepUtil.MARKER_COLORS.get(Stage.Marker.None));
            } else {
                stage.putMarker(index, currentMarker);
                control.draw(display, StepUtil.MARKER_COLORS.get(currentMarker));
            }

        }

    }

    private void onControlPressedSettings(GridControl control, int velocity) {

        SettingsUtil.SettingsChanged settingsChanged = settingsModule.controlPressed(control, velocity);
        switch (settingsChanged) {
            case SELECT_SESSION:
                selectSession(settingsModule.getNextSessionIndex());
                break;
            case LOAD_FILE:
                load(settingsModule.getCurrentFileIndex());
                break;
            case SAVE_FILE:
                save(settingsModule.getCurrentFileIndex());
                break;
            case SET_MIDI_CHANNEL:
                notesOff();
                memory.setMidiChannel(settingsModule.getMidiChannel());
                break;
        }
    }

    private void onControlReleased(GridControl control) {


    }


    /***** Clockable implementation ****************************************/

    public void start(boolean restart) {
    }

    public void stop() {
        notesOff();
    }

    public void tick(boolean andReset) {
        advance(andReset);
    }


    /***** Saveable implementation ****************************************/

    public void setFilePrefix(String filePrefix) {
        this.filePrefix = filePrefix;
    }

    public String getFilePrefix() {
        return filePrefix;
    }

    public void save(int index) {
        try {
            String filename = filename(index);
            File file = new File(filename);
            if (file.exists()) {
                // make a backup, but will overwrite any previous backups
                Files.copy(file, new File(filename + ".backup"));
            }
            objectMapper.writeValue(file, memory);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load(int index) {
        try {
            String filename = filename(index);
            File file = new File(filename);
            if (file.exists()) {
                memory = objectMapper.readValue(file, StepMemory.class);
            } else {
                memory = new StepMemory();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String filename(int index) {
        return filePrefix + "-" + index + ".json";
    }



}
