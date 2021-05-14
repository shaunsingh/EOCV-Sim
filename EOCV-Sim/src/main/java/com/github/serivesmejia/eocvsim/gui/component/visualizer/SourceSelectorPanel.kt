package com.github.serivesmejia.eocvsim.gui.component.visualizer

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.component.PopupX
import com.github.serivesmejia.eocvsim.gui.util.SourcesListIconRenderer
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class SourceSelectorPanel(private val eocvSim: EOCVSim) : JPanel() {

    val sourceSelector                 = JList<String>()
    val sourceSelectorScroll           = JScrollPane()
    var sourceSelectorButtonsContainer = JPanel()
    val sourceSelectorCreateBtt        = JButton("Create")
    val sourceSelectorDeleteBtt        = JButton("Delete")

    val sourceSelectorLabel = JLabel("Sources")

    private var beforeSelectedSource = ""
    private var beforeSelectedSourceIndex = 0

    private var lastCreateSourcePopup: PopupX? = null

    init {
        layout = FlowLayout(FlowLayout.CENTER)

        sourceSelectorLabel.font = sourceSelectorLabel.font.deriveFont(20.0f)
        sourceSelectorLabel.horizontalAlignment = JLabel.CENTER

        add(sourceSelectorLabel)

        val sourceSelectorScrollContainer = JPanel()
        sourceSelectorScrollContainer.layout = GridLayout()
        sourceSelectorScrollContainer.border = BorderFactory.createEmptyBorder(0, 20, 0, 20)

        sourceSelectorScrollContainer.add(sourceSelectorScroll)

        sourceSelector.selectionMode = ListSelectionModel.SINGLE_SELECTION

        sourceSelectorScroll.setViewportView(sourceSelector)
        sourceSelectorScroll.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        sourceSelectorScroll.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED

        //different icons
        sourceSelector.setCellRenderer(SourcesListIconRenderer(eocvSim.inputSourceManager))

        sourceSelectorCreateBtt.addActionListener {
            lastCreateSourcePopup?.hide()

            val panel = CreateSourcePanel(eocvSim)
            val location = sourceSelectorCreateBtt.locationOnScreen

            val frame = SwingUtilities.getWindowAncestor(this)

            val popup = PopupX(frame, panel, location.x, location.y, true)
            lastCreateSourcePopup = popup

            popup.show()
        }

        add(sourceSelectorScrollContainer)

        sourceSelectorButtonsContainer = JPanel(FlowLayout(FlowLayout.CENTER))

        sourceSelectorButtonsContainer.add(sourceSelectorCreateBtt)
        sourceSelectorButtonsContainer.add(sourceSelectorDeleteBtt)

        add(sourceSelectorButtonsContainer)

        registerListeners()
    }

    private fun registerListeners() {
        //listener for changing input sources
        sourceSelector.addListSelectionListener { evt ->
            try {
                if (sourceSelector.selectedIndex != -1) {
                    val model = sourceSelector.model
                    val source = model.getElementAt(sourceSelector.selectedIndex)

                    if (!evt.valueIsAdjusting && source != beforeSelectedSource) {
                        if (!eocvSim.pipelineManager.paused) {
                            eocvSim.inputSourceManager.requestSetInputSource(source)
                            beforeSelectedSource = source
                            beforeSelectedSourceIndex = sourceSelector.selectedIndex
                        } else {
                            //check if the user requested the pause or if it was due to one shoot analysis when selecting images
                            if (eocvSim.pipelineManager.pauseReason !== PipelineManager.PauseReason.IMAGE_ONE_ANALYSIS) {
                                sourceSelector.setSelectedIndex(beforeSelectedSourceIndex)
                            } else { //handling pausing
                                eocvSim.pipelineManager.requestSetPaused(false)
                                eocvSim.inputSourceManager.requestSetInputSource(source)
                                beforeSelectedSource = source
                                beforeSelectedSourceIndex = sourceSelector.selectedIndex
                            }
                        }
                    }
                } else {
                    sourceSelector.setSelectedIndex(1)
                }
            } catch (ignored: ArrayIndexOutOfBoundsException) {
            }
        }

        // delete selected input source
        sourceSelectorDeleteBtt.addActionListener {
            val source = sourceSelector.model.getElementAt(sourceSelector.selectedIndex)
            eocvSim.onMainUpdate.doOnce {
                eocvSim.inputSourceManager.deleteInputSource(source)
                updateSourcesList()
            }
        }
    }

    fun updateSourcesList() = runBlocking {
        launch(Dispatchers.Swing) {
            val listModel = DefaultListModel<String>()

            for (source in eocvSim.inputSourceManager.sortedInputSources) {
                listModel.addElement(source.name)
            }

            sourceSelector.fixedCellWidth = 240

            sourceSelector.model = listModel
            sourceSelector.revalidate()
            sourceSelectorScroll.revalidate()
        }
    }

    fun revalAndRepaint() {
        sourceSelector.revalidate()
        sourceSelector.repaint()
        sourceSelectorScroll.revalidate()
        sourceSelectorScroll.repaint()
        revalidate(); repaint()
    }

}