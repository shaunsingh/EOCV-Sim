/*
 * Copyright (c) 2021 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.github.serivesmejia.eocvsim.gui;

import com.formdev.flatlaf.FlatLaf;
import com.github.serivesmejia.eocvsim.EOCVSim;
import com.github.serivesmejia.eocvsim.gui.component.Viewport;
import com.github.serivesmejia.eocvsim.gui.component.tuner.ColorPicker;
import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanel;
import com.github.serivesmejia.eocvsim.gui.component.visualizer.pipeline.PipelineSelectorPanel;
import com.github.serivesmejia.eocvsim.gui.component.visualizer.SourceSelectorPanel;
import com.github.serivesmejia.eocvsim.gui.component.visualizer.TelemetryPanel;
import com.github.serivesmejia.eocvsim.gui.component.visualizer.TopMenuBar;
import com.github.serivesmejia.eocvsim.gui.theme.Theme;
import com.github.serivesmejia.eocvsim.gui.util.GuiUtil;
import com.github.serivesmejia.eocvsim.gui.util.ReflectTaskbar;
import com.github.serivesmejia.eocvsim.pipeline.compiler.PipelineCompiler;
import com.github.serivesmejia.eocvsim.util.Log;
import com.github.serivesmejia.eocvsim.workspace.util.template.GradleWorkspaceTemplate;
import kotlin.Unit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Visualizer {

    public static ImageIcon ICO_EOCVSIM = null;

    static {
        try {
            ICO_EOCVSIM = GuiUtil.loadImageIcon("/images/icon/ico_eocvsim.png");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public final ArrayList<AsyncPleaseWaitDialog> pleaseWaitDialogs = new ArrayList<>();

    public final ArrayList<JFrame> childFrames = new ArrayList<>();
    public final ArrayList<JDialog> childDialogs = new ArrayList<>();

    private final ArrayList<Runnable> onInitFinishedRunns = new ArrayList<>();

    private final EOCVSim eocvSim;

    public JFrame frame = null;

    public Viewport viewport = null;

    public TopMenuBar menuBar = null;

    public JPanel tunerMenuPanel = new JPanel();

    public JScrollPane imgScrollPane = null;

    public JPanel rightContainer = null;
    public JSplitPane globalSplitPane = null;
    public JSplitPane imageTunerSplitPane = null;

    public PipelineSelectorPanel pipelineSelectorPanel = null;
    public SourceSelectorPanel sourceSelectorPanel = null;
    public TelemetryPanel telemetryPanel = null;

    private String title = "EasyOpenCV Simulator v" + EOCVSim.VERSION;
    private String titleMsg = "No pipeline";
    private String beforeTitle = "";
    private String beforeTitleMsg = "";

    public ColorPicker colorPicker = null;

    //stuff for zooming handling
    private volatile boolean isCtrlPressed = false;

    private volatile boolean hasFinishedInitializing = false;

    public Visualizer(EOCVSim eocvSim) {
        this.eocvSim = eocvSim;
    }

    public void init(Theme theme) {
        if(ReflectTaskbar.INSTANCE.isUsable()){
            try {
                //set icon for mac os (and other systems which do support this method)
                ReflectTaskbar.INSTANCE.setIconImage(ICO_EOCVSIM.getImage());
            } catch (final UnsupportedOperationException e) {
                Log.warn("Visualizer", "Setting the Taskbar icon image is not supported on this platform");
            } catch (final SecurityException e) {
                Log.error("Visualizer", "Security exception while setting TaskBar icon", e);
            }
        }

        try {
            theme.install();
        } catch (Exception e) {
            Log.error("Visualizer", "Failed to install theme " + theme.name(), e);
        }

        Icons.INSTANCE.setDark(FlatLaf.isLafDark());

        //instantiate all swing elements after theme installation
        frame = new JFrame();
        viewport = new Viewport(eocvSim, eocvSim.getConfig().maxFps);

        menuBar = new TopMenuBar(this, eocvSim);

        tunerMenuPanel = new JPanel();

        pipelineSelectorPanel = new PipelineSelectorPanel(eocvSim);
        sourceSelectorPanel   = new SourceSelectorPanel(eocvSim);
        telemetryPanel        = new TelemetryPanel();

        rightContainer = new JPanel();

        /*
         * TOP MENU BAR
         */
        
        frame.setJMenuBar(menuBar);

        /*
         * IMG VISUALIZER & SCROLL PANE
         */

        imgScrollPane = new JScrollPane(viewport);

        imgScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        imgScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        imgScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        imgScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        rightContainer.setLayout(new GridLayout(3, 1));

        /*
         * PIPELINE SELECTOR
         */

        rightContainer.add(pipelineSelectorPanel);

        /*
         * SOURCE SELECTOR
         */

        rightContainer.add(sourceSelectorPanel);

        /*
         * TELEMETRY
         */

        rightContainer.add(telemetryPanel);

        /*
         * SPLIT
         */

        //left side, image scroll & tuner menu split panel
        imageTunerSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, imgScrollPane, tunerMenuPanel);

        imageTunerSplitPane.setResizeWeight(1);
        imageTunerSplitPane.setOneTouchExpandable(false);
        imageTunerSplitPane.setContinuousLayout(true);

        //global
        globalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, imageTunerSplitPane, rightContainer);

        globalSplitPane.setResizeWeight(1);
        globalSplitPane.setOneTouchExpandable(false);
        globalSplitPane.setContinuousLayout(true);

        frame.add(globalSplitPane, BorderLayout.CENTER);

        //initialize other various stuff of the frame
        frame.setSize(780, 645);
        frame.setMinimumSize(frame.getSize());
        frame.setTitle("EasyOpenCV Simulator - No Pipeline");

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        frame.setIconImage(ICO_EOCVSIM.getImage());

        frame.setLocationRelativeTo(null);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        globalSplitPane.setDividerLocation(1070);

        frame.setVisible(true);

        registerListeners();

        colorPicker = new ColorPicker(viewport.image);

        for(Runnable runn : onInitFinishedRunns) {
            runn.run();
        }

        hasFinishedInitializing = true;
    }

    public void initAsync(Theme simTheme) {
        SwingUtilities.invokeLater(() -> init(simTheme));
    }

    private void registerListeners() {

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                eocvSim.onMainUpdate.doOnce((Runnable) eocvSim::destroy);
            }
        });

        //handling onViewportTapped evts
        viewport.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if(!colorPicker.isPicking())
                    eocvSim.pipelineManager.callViewportTapped();
            }
        });

        //VIEWPORT RESIZE HANDLING
        imgScrollPane.addMouseWheelListener(e -> {
            if (isCtrlPressed) { //check if control key is pressed
                double scale = viewport.getViewportScale() - (0.15 * e.getPreciseWheelRotation());
                viewport.setViewportScale(scale);
            }
        });

        //listening for keyboard presses and releases, to check if ctrl key was pressed or released (handling zoom)
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(ke -> {
            switch (ke.getID()) {
                case KeyEvent.KEY_PRESSED:
                    if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
                        isCtrlPressed = true;
                        imgScrollPane.setWheelScrollingEnabled(false); //lock scrolling if ctrl is pressed
                    }
                    break;
                case KeyEvent.KEY_RELEASED:
                    if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
                        isCtrlPressed = false;
                        imgScrollPane.setWheelScrollingEnabled(true); //unlock
                    }
                    break;
            }
            return false; //idk let's just return false 'cause keyboard input doesn't work otherwise
        });

        //resizes all three JLists in right panel to make buttons visible in smaller resolutions
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent evt) {
                double ratio = frame.getSize().getHeight() / 645;

                double fontSize = 15.5 * ratio;
                int columns = (int) Math.round(5 * ratio);

                Font font = pipelineSelectorPanel.getPipelineSelectorLabel().getFont().deriveFont((float)fontSize);

                pipelineSelectorPanel.getPipelineSelector().setVisibleRowCount(columns);
                pipelineSelectorPanel.getPipelineSelectorLabel().setFont(font);
                pipelineSelectorPanel.revalAndRepaint();

                columns = (int) Math.round(5 * ratio);
                
                sourceSelectorPanel.getSourceSelector().setVisibleRowCount(columns);
                sourceSelectorPanel.getSourceSelectorLabel().setFont(font);
                sourceSelectorPanel.revalAndRepaint();

                telemetryPanel.getTelemetryList().setVisibleRowCount(columns);
                telemetryPanel.getTelemetryLabel().setFont(font);
                telemetryPanel.revalAndRepaint();

                rightContainer.revalidate();
                rightContainer.repaint();
            }
        });

        //stop color-picking mode when changing pipeline
        //eocvSim.pipelineManager.onPipelineChange.doPersistent(() -> colorPicker.stopPicking());
    }

    public boolean hasFinishedInit() { return hasFinishedInitializing; }

    public void waitForFinishingInit() {
        while (!hasFinishedInitializing) {
            Thread.yield();
        }
    }

    public void onInitFinished(Runnable runn) {
        onInitFinishedRunns.add(runn);
    }

    public void close() {
        SwingUtilities.invokeLater(() -> {
            frame.setVisible(false);
            viewport.stop();

            //close all asyncpleasewait dialogs
            for (AsyncPleaseWaitDialog dialog : pleaseWaitDialogs) {
                if (dialog != null) {
                    dialog.destroyDialog();
                }
            }

            pleaseWaitDialogs.clear();

            //close all opened frames
            for (JFrame frame : childFrames) {
                if (frame != null && frame.isVisible()) {
                    frame.setVisible(false);
                    frame.dispose();
                }
            }

            childFrames.clear();

            //close all opened dialogs
            for (JDialog dialog : childDialogs) {
                if (dialog != null && dialog.isVisible()) {
                    dialog.setVisible(false);
                    dialog.dispose();
                }
            }

            childDialogs.clear();
            frame.dispose();
            viewport.flush();
        });
    }

    private void setFrameTitle(String title, String titleMsg) {
        frame.setTitle(title + " - " + titleMsg);
    }

    public void setTitle(String title) {
        this.title = title;
        if (!beforeTitle.equals(title)) setFrameTitle(title, titleMsg);
        beforeTitle = title;
    }

    public void setTitleMessage(String titleMsg) {
        this.titleMsg = titleMsg;
        if (!beforeTitleMsg.equals(title)) setFrameTitle(title, titleMsg);
        beforeTitleMsg = titleMsg;
    }

    public void updateTunerFields(List<TunableFieldPanel> fields) {
        tunerMenuPanel.removeAll();

        for (TunableFieldPanel fieldPanel : fields) {
            tunerMenuPanel.add(fieldPanel);
            fieldPanel.showFieldPanel();
        }

        tunerMenuPanel.updateUI();
        imageTunerSplitPane.updateUI();
    }

    public void asyncCompilePipelines() {
        if(PipelineCompiler.Companion.getIS_USABLE()) {
            menuBar.workspCompile.setEnabled(false);
            pipelineSelectorPanel.getButtonsPanel().getPipelineCompileBtt().setEnabled(false);

            eocvSim.pipelineManager.compiledPipelineManager.asyncCompile((result) -> {
                menuBar.workspCompile.setEnabled(true);
                pipelineSelectorPanel.getButtonsPanel().getPipelineCompileBtt().setEnabled(true);

                return Unit.INSTANCE;
            });
        } else {
            asyncPleaseWaitDialog(
                    "Runtime compilation is not supported on this JVM",
                    "For further info, check the EOCV-Sim GitHub repo",
                    "Close",
                    new Dimension(320, 160),
                    true, true
            );
        }
    }

    public void selectPipelinesWorkspace() {
        DialogFactory.createFileChooser(
                frame, DialogFactory.FileChooser.Mode.DIRECTORY_SELECT
        ).addCloseListener((OPTION, selectedFile, selectedFileFilter) -> {
            if (OPTION == JFileChooser.APPROVE_OPTION) {
                eocvSim.onMainUpdate.doOnce(() -> {
                    eocvSim.workspaceManager.setWorkspaceFile(selectedFile);
                });
                asyncCompilePipelines();
            }
        });
    }

    public void createVSCodeWorkspace() {
        DialogFactory.createFileChooser(frame, DialogFactory.FileChooser.Mode.DIRECTORY_SELECT)
        .addCloseListener((OPTION, selectedFile, selectedFileFilter) -> {
            if(OPTION == JFileChooser.APPROVE_OPTION) {
                if(selectedFile.isDirectory() && Objects.requireNonNull(selectedFile.listFiles()).length == 0) {
                    eocvSim.workspaceManager.createWorkspaceWithTemplateAsync(selectedFile, GradleWorkspaceTemplate.INSTANCE);
                } else {
                    asyncPleaseWaitDialog(
                            "The selected directory must be empty", "Select an empty directory or create a new one",
                            "Retry", new Dimension(320, 160), true, true
                    ).onCancel(this::createVSCodeWorkspace);
                }
            }
        });
    }

    // PLEASE WAIT DIALOGS

    public boolean pleaseWaitDialog(JDialog diag, String message, String subMessage, String cancelBttText, Dimension size, boolean cancellable, AsyncPleaseWaitDialog apwd, boolean isError) {
        final JDialog dialog = diag == null ? new JDialog(this.frame) : diag;

        boolean addSubMessage = subMessage != null;

        int rows = 3;
        if (!addSubMessage) {
            rows--;
        }

        dialog.setModal(true);
        dialog.setLayout(new GridLayout(rows, 1));

        if (isError) {
            dialog.setTitle("Operation failed");
        } else {
            dialog.setTitle("Operation in progress");
        }

        JLabel msg = new JLabel(message);
        msg.setHorizontalAlignment(JLabel.CENTER);
        msg.setVerticalAlignment(JLabel.CENTER);

        dialog.add(msg);

        JLabel subMsg = null;
        if (addSubMessage) {

            subMsg = new JLabel(subMessage);
            subMsg.setHorizontalAlignment(JLabel.CENTER);
            subMsg.setVerticalAlignment(JLabel.CENTER);

            dialog.add(subMsg);

        }

        JPanel exitBttPanel = new JPanel(new FlowLayout());
        JButton cancelBtt = new JButton(cancelBttText);

        cancelBtt.setEnabled(cancellable);

        exitBttPanel.add(cancelBtt);

        boolean[] cancelled = {false};

        cancelBtt.addActionListener(e -> {
            cancelled[0] = true;
            dialog.setVisible(false);
            dialog.dispose();
        });

        dialog.add(exitBttPanel);

        if (apwd != null) {
            apwd.msg = msg;
            apwd.subMsg = subMsg;
            apwd.cancelBtt = cancelBtt;
        }
        
        if(size == null) size = new Dimension(400, 200);
        dialog.setSize(size);

        dialog.setLocationRelativeTo(null);
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        dialog.setVisible(true);

        return cancelled[0];
    }

    public void pleaseWaitDialog(JDialog dialog, String message, String subMessage, String cancelBttText, Dimension size, boolean cancellable) {
        pleaseWaitDialog(dialog, message, subMessage, cancelBttText, size, cancellable, null, false);
    }

    public void pleaseWaitDialog(String message, String subMessage, String cancelBttText, Dimension size, boolean cancellable) {
        pleaseWaitDialog(null, message, subMessage, cancelBttText, size, cancellable, null, false);
    }

    public AsyncPleaseWaitDialog asyncPleaseWaitDialog(String message, String subMessage, String cancelBttText, Dimension size, boolean cancellable, boolean isError) {
        AsyncPleaseWaitDialog rPWD = new AsyncPleaseWaitDialog(message, subMessage, cancelBttText, size, cancellable, isError, eocvSim);
        SwingUtilities.invokeLater(rPWD);

        return rPWD;
    }

    public AsyncPleaseWaitDialog asyncPleaseWaitDialog(String message, String subMessage, String cancelBttText, Dimension size, boolean cancellable) {
        AsyncPleaseWaitDialog rPWD = new AsyncPleaseWaitDialog(message, subMessage, cancelBttText, size, cancellable, false, eocvSim);
        SwingUtilities.invokeLater(rPWD);

        return rPWD;
    }

    public class AsyncPleaseWaitDialog implements Runnable {

        public volatile JDialog dialog = new JDialog(frame);

        public volatile JLabel msg = null;
        public volatile JLabel subMsg = null;

        public volatile JButton cancelBtt = null;

        public volatile boolean wasCancelled = false;
        public volatile boolean isError;

        public volatile String initialMessage;
        public volatile String initialSubMessage;

        public volatile boolean isDestroyed = false;

        String message;
        String subMessage;
        String cancelBttText;

        Dimension size;

        boolean cancellable;

        private final ArrayList<Runnable> onCancelRunnables = new ArrayList<>();

        public AsyncPleaseWaitDialog(String message, String subMessage, String cancelBttText, Dimension size, boolean cancellable, boolean isError, EOCVSim eocvSim) {
            this.message = message;
            this.subMessage = subMessage;
            this.initialMessage = message;
            this.initialSubMessage = subMessage;
            this.cancelBttText = cancelBttText;

            this.size = size;
            this.cancellable = cancellable;

            this.isError = isError;

            eocvSim.visualizer.pleaseWaitDialogs.add(this);
        }

        public void onCancel(Runnable runn) {
            onCancelRunnables.add(runn);
        }

        @Override
        public void run() {
            wasCancelled = pleaseWaitDialog(dialog, message, subMessage, cancelBttText, size, cancellable, this, isError);

            if (wasCancelled) {
                for (Runnable runn : onCancelRunnables) {
                    runn.run();
                }
            }
        }

        public void destroyDialog() {
            if (!isDestroyed) {
                dialog.setVisible(false);
                dialog.dispose();
                isDestroyed = true;
            }
        }

    }

}
