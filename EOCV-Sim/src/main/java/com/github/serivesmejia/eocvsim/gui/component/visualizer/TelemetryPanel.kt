package com.github.serivesmejia.eocvsim.gui.component.visualizer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing

import org.firstinspires.ftc.robotcore.external.Telemetry

import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import javax.swing.*

class TelemetryPanel : JPanel(FlowLayout(FlowLayout.CENTER)) {

    val telemetryScroll = JScrollPane()
    val telemetryList  = JList<String>()

    init {
        /*
         * TELEMETRY
         */

        val telemetryLabel = JLabel("Telemetry")

        telemetryLabel.font = telemetryLabel.font.deriveFont(20.0f)
        telemetryLabel.horizontalAlignment = JLabel.CENTER

        add(telemetryLabel)

        telemetryScroll.setViewportView(telemetryList)
        telemetryScroll.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        telemetryScroll.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS

        //tooltips for the telemetry list items (thnx stackoverflow)
        telemetryList.addMouseMotionListener(object : MouseMotionListener {
            override fun mouseDragged(e: MouseEvent) {}
            override fun mouseMoved(e: MouseEvent) {
                val l = e.source as JList<*>
                val m = l.model
                val index = l.locationToIndex(e.point)
                if (index > -1) {
                    l.toolTipText = m.getElementAt(index).toString()
                }
            }
        })

        telemetryList.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION

        val telemetryScrollContainer = JPanel()
        telemetryScrollContainer.layout = GridLayout()
        telemetryScrollContainer.border = BorderFactory.createEmptyBorder(0, 20, 20, 20)

        telemetryScrollContainer.add(telemetryScroll)

        add(telemetryScrollContainer)
    }

    fun revalAndRepaint() {
        telemetryList.revalidate()
        telemetryList.repaint()
        telemetryScroll.revalidate()
        telemetryScroll.repaint()
    }

    fun updateTelemetry(telemetry: Telemetry?) {
        var telemetryText: String? = null

        if (telemetry != null && telemetry.hasChanged()) {
            telemetryText = telemetry.toString()

            val listModel = DefaultListModel<String>()
            for (line in telemetryText.split("\n").toTypedArray()) {
                    listModel.addElement(line)
            }

            telemetryList.fixedCellWidth = 240
            telemetryList.model = listModel
        }

        if (telemetryList.model.size <= 0 || (telemetryText != null && telemetryText.trim { it <= ' ' } == "")) {
            val listModel = DefaultListModel<String>()

            listModel.addElement("<html></html>")
            telemetryList.model = listModel
        }

        revalAndRepaint()
    }

}