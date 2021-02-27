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

package com.github.serivesmejia.eocvsim.gui.component.tuner

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.component.input.EnumComboBox
import com.github.serivesmejia.eocvsim.gui.component.input.SizeFields
import com.github.serivesmejia.eocvsim.tuner.TunableField
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JToggleButton

class TunableFieldPanelConfig(private val fieldOptions: TunableFieldPanelOptions,
                              private val eocvSim: EOCVSim) : JPanel() {

    var localConfig = eocvSim.config.globalTunableFieldsConfig.copy()
        private set

    val currentConfig: Config
        get() {
            val config = localConfig.copy()
            applyToConfig(config)
            return config
        }

    private val sliderRangeFieldsPanel = JPanel()

    private var sliderRangeFields     = createRangeFields()
    private val colorSpaceComboBox    = EnumComboBox("Color space: ", PickerColorSpace::class.java, PickerColorSpace.values())

    private val applyToAllButtonPanel = JPanel(GridBagLayout())
    private val applyToAllButton      = JToggleButton("Apply to all fields...")

    private val applyModesPanel             = JPanel(GridLayout(1, 2))
    private val applyToAllFieldsButton      = JButton("Globally")
    private val applyToAllOfSameTypeButton  = JButton("Of this type")

    private val constCenterBottom = GridBagConstraints()

    private val configSourceLabel = JLabel(localConfig.source.description)

    private val allowsDecimals
        get() = fieldOptions.fieldPanel.tunableField.allowMode == TunableField.AllowMode.ONLY_NUMBERS_DECIMAL

    private val fieldTypeClass = fieldOptions.fieldPanel.tunableField::class.java

    //represents a color space conversion when picking from the viewport. always
    //convert from rgb to the desired color space since that's the color space of
    //the scalar the ColorPicker returns from the viewport after picking.
    enum class PickerColorSpace(val cvtCode: Int) {
        YCrCb(Imgproc.COLOR_RGB2YCrCb),
        HSV(Imgproc.COLOR_RGB2HSV),
        RGB(Imgproc.COLOR_RGBA2RGB),
        Lab(Imgproc.COLOR_RGB2Lab)
    }

    enum class ConfigSource(val description: String) {
        LOCAL("From local config"),
        GLOBAL("From global config"),
        TYPE_SPECIFIC("From specific config")
    }

    data class Config(var sliderRange: Size,
                      var pickerColorSpace: PickerColorSpace,
                      var fieldPanelMode: TunableFieldPanel.Mode,
                      var source: ConfigSource)

    init {
        layout = GridLayout(4, 1)

        //adding into an individual panel so that we can add
        //and remove later when recreating without much problem
        sliderRangeFieldsPanel.add(sliderRangeFields)
        add(sliderRangeFieldsPanel)

        colorSpaceComboBox.onSelect.doPersistent { updateConfigSourceLabel(currentConfig) }
        //combo box to select color space
        colorSpaceComboBox.selectedEnum = localConfig.pickerColorSpace
        add(colorSpaceComboBox)

        //centering apply to all button...
        val constCenter    = GridBagConstraints()
        constCenter.anchor = GridBagConstraints.CENTER
        constCenter.fill   = GridBagConstraints.HORIZONTAL
        constCenter.gridy  = 0

        //add apply to all button to a centered pane
        applyToAllButtonPanel.add(applyToAllButton, constCenter)
        add(applyToAllButtonPanel)

        //display or hide apply to all mode buttons
        applyToAllButton.addActionListener { toggleApplyModesPanel(applyToAllButton.isSelected) }

        //apply globally button and disable toggle for apply to all button
        applyToAllFieldsButton.addActionListener {
            toggleApplyModesPanel(false)
            applyGlobally()
        }
        applyModesPanel.add(applyToAllFieldsButton)

        //apply of same type button and disable toggle for apply to all button
        applyToAllOfSameTypeButton.addActionListener {
            toggleApplyModesPanel(false)
            applyOfSameType()
        }
        applyModesPanel.add(applyToAllOfSameTypeButton)

        //add two apply to all modes buttons to the bottom center
        constCenterBottom.anchor = GridBagConstraints.CENTER
        constCenterBottom.fill = GridBagConstraints.HORIZONTAL
        constCenterBottom.gridy = 1

        applyToAllButtonPanel.add(applyModesPanel, constCenterBottom)

        configSourceLabel.horizontalAlignment = JLabel.CENTER
        add(configSourceLabel)

        applyFromEOCVSimConfig()
    }


    //set the current config values and hide apply modes panel when panel show
    fun panelShow() {
        updateConfigGuiFromConfig()

        applyToAllButton.isSelected = false
        toggleApplyModesPanel(false)
    }

    //set the slider bounds when the popup gets closed
    fun panelHide() {
        applyToConfig()
        updateFieldGuiFromConfig()
        toggleApplyModesPanel(true)
    }

    //hides or displays apply to all mode buttons
    private fun toggleApplyModesPanel(show: Boolean) {
        if(show) {
            applyToAllButtonPanel.add(applyModesPanel, constCenterBottom)
        } else {
            applyToAllButtonPanel.remove(applyModesPanel)
        }

        //toggle or untoggle apply to all button
        applyToAllButton.isSelected = show

        //need to repaint...
        applyToAllButtonPanel.repaint(); applyToAllButtonPanel.revalidate()
        repaint(); revalidate()
    }

    //applies the config of this tunable field panel globally
    private fun applyGlobally() {
        applyToConfig() //saves the current values to the current local config

        localConfig.source = ConfigSource.GLOBAL //changes the source of the local config to global
        eocvSim.config.globalTunableFieldsConfig = localConfig.copy()

        updateConfigSourceLabel()
    }

    //applies the config of this tunable field to this type specifically
    private fun applyOfSameType() {
        applyToConfig() //saves the current values to the current local config
        val typeClass = fieldOptions.fieldPanel.tunableField::class.java

        localConfig.source = ConfigSource.TYPE_SPECIFIC //changes the source of the local config to type specific
        eocvSim.config.specificTunableFieldConfig[typeClass.name] = localConfig.copy()

        updateConfigSourceLabel()
    }

    //loads the config from global eocv sim config file
    private fun applyFromEOCVSimConfig() {
        val specificConfigs = eocvSim.config.specificTunableFieldConfig

        //apply specific config if we have one, or else, apply global
        localConfig = if(specificConfigs.containsKey(fieldTypeClass.name)) {
            specificConfigs[fieldTypeClass.name]!!.copy()
        } else {
            eocvSim.config.globalTunableFieldsConfig.copy()
        }

        updateConfigGuiFromConfig()
        updateConfigSourceLabel()
    }

    //applies the current values to the specified config, defaults to local
    private fun applyToConfig(config: Config = localConfig) {
        //if user entered a valid number and our max value is bigger than the minimum...
        if(sliderRangeFields.valid) {
            config.sliderRange = sliderRangeFields.currentSize
            //update slider range in gui sliders...
            if(config.sliderRange.height > config.sliderRange.width && config !== localConfig)
                updateFieldGuiFromConfig()
        }

        //set the color space enum to the config if it's not null
        colorSpaceComboBox.selectedEnum?.let {
            config.pickerColorSpace = it
        }

        //sets the panel mode (sliders or textboxes) to config from the current mode
        if(fieldOptions.fieldPanel?.mode != null) {
            config.fieldPanelMode = fieldOptions.fieldPanel.mode
        }
    }

    private fun updateConfigSourceLabel(currentConfig: Config = localConfig) {
        //sets to local if user changed values and hasn't applied locally or globally
        if(currentConfig != localConfig) {
            localConfig.source = ConfigSource.LOCAL
        }

        configSourceLabel.text = localConfig.source.description
    }

    //updates the actual configuration displayed on the field panel gui
    fun updateFieldGuiFromConfig() {
        //sets the slider range from config
        fieldOptions.fieldPanel.setSlidersRange(localConfig.sliderRange.width, localConfig.sliderRange.height)
        //sets the panel mode (sliders or textboxes) to config from the current mode
        if(fieldOptions.fieldPanel != null && fieldOptions.fieldPanel.fields != null){
            fieldOptions.fieldPanel.mode = localConfig.fieldPanelMode
        }
    }

    //updates the values displayed in this config's ui to the current config values
    private fun updateConfigGuiFromConfig() {
        sliderRangeFieldsPanel.remove(sliderRangeFields) //remove old fields
        sliderRangeFields = createRangeFields() //need to recreate in order to set new values
        sliderRangeFieldsPanel.add(sliderRangeFields) //add new fields

        //need to reval&repaint as always
        sliderRangeFieldsPanel.revalidate(); sliderRangeFieldsPanel.repaint()

        colorSpaceComboBox.selectedEnum = localConfig.pickerColorSpace
    }

    //simple short hand for a repetitive instantiation...
    private fun createRangeFields(): SizeFields {
        val fields = SizeFields(localConfig.sliderRange, allowsDecimals, true,"Slider range:", " to ")
        fields.onChange.doPersistent {
            updateConfigSourceLabel(currentConfig)
        }

        return fields
    }

}